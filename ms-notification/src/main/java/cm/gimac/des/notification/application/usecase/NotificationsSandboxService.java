package cm.gimac.des.notification.application.usecase;

import cm.gimac.des.notification.application.port.in.RecevoirNotificationsSandboxUseCase;
import cm.gimac.des.notification.application.port.out.DestinatairesSandboxPort;
import cm.gimac.des.notification.domain.boite.NotificationUtilisateur;
import cm.gimac.des.notification.domain.boite.Reference;
import cm.gimac.des.notification.domain.boite.TypeNotification;
import cm.gimac.des.notification.domain.event.NotificationCreeePayload;
import cm.gimac.des.notification.domain.exception.NotificationException;
import cm.gimac.des.notification.domain.service.ReglesSandbox;
import cm.gimac.des.notification.domain.service.ReglesSandbox.Destinataire;
import cm.gimac.des.notification.domain.service.ReglesSandbox.Modele;
import cm.gimac.des.notification.domain.service.ReglesSandbox.Portee;
import cm.gimac.des.notification.domain.service.ReglesSandbox.Role;
import cm.gimac.des.notification.infrastructure.messaging.outbox.OutboxPublisher;
import cm.gimac.des.notification.repository.NotificationUtilisateurRepository;
import cm.gimac.des.notification.repository.PreferencesNotificationRepository;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Notifications de la Sandbox GIMAC (US-EXI-09, docs/lot1/sandbox-existant-adapte.md §11) : le modèle de la demande
 * fixe titre, sous-titre et portée ({@link ReglesSandbox}) ; les destinataires sont résolus chez MS-IAM dans la seule
 * portée du modèle (comptes de l'institution, notifiés avec un lien du portail ; comptes du GIMAC porteurs d'une
 * permission, lien du DES). La préférence SANDBOX de chaque compte est respectée. Une demande sans règle est mise au
 * rebut (table rebut_notification_sandbox) avec son motif et une alerte d'exploitation est journalisée.
 */
@Service
@Transactional
public class NotificationsSandboxService implements RecevoirNotificationsSandboxUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(NotificationsSandboxService.class);

    /** Motifs de rebut. */
    public static final String MODELE_SANS_REGLE = "MODELE_SANS_REGLE";

    private final NotificationUtilisateurRepository notifications;
    private final PreferencesNotificationRepository preferences;
    private final DestinatairesSandboxPort annuaire;
    private final OutboxPublisher outbox;
    private final ReglesSandbox regles;
    private final JdbcTemplate jdbc;
    private final Clock horloge;

    public NotificationsSandboxService(
        NotificationUtilisateurRepository notifications,
        PreferencesNotificationRepository preferences,
        DestinatairesSandboxPort annuaire,
        OutboxPublisher outbox,
        ReglesSandbox regles,
        JdbcTemplate jdbc,
        Clock horloge
    ) {
        this.notifications = notifications;
        this.preferences = preferences;
        this.annuaire = annuaire;
        this.outbox = outbox;
        this.regles = regles;
        this.jdbc = jdbc;
        this.horloge = horloge;
    }

    @Override
    public int demander(Demande d) {
        Optional<Modele> modele = regles.modele(d.modele());
        if (modele.isEmpty()) {
            rebuter(d, MODELE_SANS_REGLE);
            return 0;
        }
        Modele m = modele.orElseThrow();
        Instant instant = (d.le() == null ? horloge.instant() : d.le()).truncatedTo(ChronoUnit.MICROS);
        Reference reference = Reference.sandbox(m.code(), d.evenementId());
        int n = 0;
        for (Destinataire dest : resoudre(m, d.destinataires(), d.codeParticipant())) {
            if (!preferences.findById(dest.compteId()).map(p -> p.accepte(TypeNotification.SANDBOX)).orElse(true)) {
                continue;
            }
            // Une notification par demande et par compte : jamais regroupée ; aucun acteur (demande d'un service).
            NotificationUtilisateur notification = new NotificationUtilisateur(
                dest.compteId(),
                TypeNotification.SANDBOX,
                regles.titre(m, d.codeParticipant()),
                m.sousTitre(),
                regles.lien(dest.portail(), d.codeParticipant()),
                reference,
                null,
                null,
                instant
            );
            publier(notifications.save(notification));
            n++;
        }
        if (n == 0) {
            LOG.info("Demande {} ({}, {}) : aucun destinataire notifié", d.evenementId(), m.code(), ReglesSandbox.code(d.codeParticipant()));
        }
        return n;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Resolution> simuler(String code, List<String> destinataires, String codeParticipant) {
        Modele m = regles.modele(code).orElseThrow(() -> NotificationException.champ("modele", "Modèle de notification de la sandbox sans règle"));
        return resoudre(m, destinataires, codeParticipant)
            .stream()
            .map(d -> new Resolution(d.compteId(), d.portail(), regles.titre(m, codeParticipant), regles.lien(d.portail(), codeParticipant)))
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ModeleVue> modeles() {
        return regles.modeles().stream().map(m -> new ModeleVue(m.code(), m.titre(), m.sousTitre(), m.portee().name())).toList();
    }

    /** Lit chez MS-IAM ce que la portée et les rôles de la demande exigent, pas davantage. */
    private List<Destinataire> resoudre(Modele m, List<String> demandes, String codeParticipant) {
        Map<String, Set<String>> comptes = m.portee() == Portee.GIMAC ? Map.of() : annuaire.comptesInstitution(codeParticipant);
        Map<String, List<String>> porteurs = new HashMap<>();
        for (String r : ReglesSandbox.rolesDemandes(demandes)) {
            Role role = ReglesSandbox.ROLES.get(r);
            if (role != null && role.permissionGimac() != null && m.portee() != Portee.INSTITUTION) {
                porteurs.put(role.permissionGimac(), annuaire.porteurs(role.permissionGimac()));
            }
        }
        return regles.destinataires(m, demandes, comptes, p -> porteurs.getOrDefault(p, List.of()));
    }

    /** Rebut avec motif et alerte d'exploitation : la demande n'est jamais perdue en silence. */
    private void rebuter(Demande d, String motif) {
        jdbc.update(
            "INSERT INTO rebut_notification_sandbox (evenement_id, service, modele, code_participant, motif, recu_le) VALUES (?, ?, ?, ?, ?, ?)",
            tronquer(d.evenementId(), 80),
            tronquer(d.service(), 40),
            tronquer(d.modele(), 80),
            ReglesSandbox.code(d.codeParticipant()),
            motif,
            Timestamp.from(horloge.instant())
        );
        LOG.error(
            "ALERTE_EXPLOITATION notification sandbox au rebut : evenement={} service={} modele={} motif={}",
            tronquer(d.evenementId(), 80),
            tronquer(d.service(), 40),
            tronquer(d.modele(), 80),
            motif
        );
    }

    /** Valeur bornée et sans saut de ligne (journal et rebut). */
    static String tronquer(String s, int max) {
        if (s == null) {
            return null;
        }
        String v = s.replaceAll("[\\r\\n\\t]", "_");
        return v.length() > max ? v.substring(0, max) : v;
    }

    /** notification.inapp-created.v1 au seul destinataire. */
    private void publier(NotificationUtilisateur n) {
        outbox.publish(
            BoiteNotificationsService.AGREGAT,
            n.getId(),
            NotificationCreeePayload.TYPE,
            new NotificationCreeePayload(
                n.getId(),
                n.getType().name(),
                List.of(n.getDestinataireId()),
                n.getReference().getService(),
                n.getReference().getType(),
                n.getReference().getId(),
                n.getActeurId(),
                n.getNombre(),
                n.getMisAJourLe()
            )
        );
    }
}
