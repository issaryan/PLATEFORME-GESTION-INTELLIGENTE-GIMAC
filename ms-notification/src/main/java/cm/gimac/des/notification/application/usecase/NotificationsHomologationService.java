package cm.gimac.des.notification.application.usecase;

import cm.gimac.des.notification.application.port.in.RecevoirHomologationUseCase;
import cm.gimac.des.notification.application.port.out.ComptesInstitutionPort;
import cm.gimac.des.notification.domain.boite.NotificationUtilisateur;
import cm.gimac.des.notification.domain.boite.Reference;
import cm.gimac.des.notification.domain.boite.TypeNotification;
import cm.gimac.des.notification.domain.event.NotificationCreeePayload;
import cm.gimac.des.notification.domain.service.ReglesHomologation;
import cm.gimac.des.notification.domain.service.ReglesHomologation.Cible;
import cm.gimac.des.notification.domain.service.ReglesHomologation.FaitsJalon;
import cm.gimac.des.notification.domain.service.ReglesHomologation.FaitsOperation;
import cm.gimac.des.notification.domain.service.ReglesHomologation.Role;
import cm.gimac.des.notification.infrastructure.messaging.outbox.OutboxPublisher;
import cm.gimac.des.notification.repository.NotificationUtilisateurRepository;
import cm.gimac.des.notification.repository.PreferencesNotificationRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Notifications du parcours d'homologation (D-93, docs/lot1/effet-reel-referentiels.md §11) : les cibles et rôles
 * viennent de {@link ReglesHomologation} ; une institution visée est résolue en ses comptes actifs (MS-IAM), notifiés
 * avec un lien du portail ; un compte visé à plusieurs titres reçoit une seule notification (rôle le plus fort) ; la
 * préférence HOMOLOGATION de chaque compte est respectée. Les fonctions « à affecter » ne sont notifiées à personne
 * nommément (journal seulement).
 */
@Service
@Transactional
public class NotificationsHomologationService implements RecevoirHomologationUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(NotificationsHomologationService.class);

    /** Destinataire résolu : rôle retenu et couche du lien (portail pour un compte d'institution). */
    private record Envoi(Role role, boolean portail) {}

    private final NotificationUtilisateurRepository notifications;
    private final PreferencesNotificationRepository preferences;
    private final ComptesInstitutionPort institutions;
    private final OutboxPublisher outbox;
    private final ReglesHomologation regles;
    private final Clock horloge;
    private final Set<String> jalonsPortail;

    public NotificationsHomologationService(
        NotificationUtilisateurRepository notifications,
        PreferencesNotificationRepository preferences,
        ComptesInstitutionPort institutions,
        OutboxPublisher outbox,
        ReglesHomologation regles,
        Clock horloge,
        @Value("${gimac.notification.homologation.jalons-portail:APTE_CERTIFICATION,CERTIFIEE,APPROUVEE_SECURITE,EN_PRODUCTION}") Set<String> jalonsPortail
    ) {
        this.notifications = notifications;
        this.preferences = preferences;
        this.institutions = institutions;
        this.outbox = outbox;
        this.regles = regles;
        this.horloge = horloge;
        this.jalonsPortail = jalonsPortail == null ? ReglesHomologation.JALONS_PORTAIL_DEFAUT : Set.copyOf(jalonsPortail);
    }

    @Override
    public int operationHomologation(FaitsOperation f, Instant le) {
        if (f == null || f.nature() == null || vide(f.operationId())) {
            return 0;
        }
        List<String> aAffecter = regles.fonctionsAAffecter(f);
        if (!aAffecter.isEmpty()) {
            LOG.info("Opération {} ({}) : fonctions à affecter {} sans notification nominative", f.operationId(), f.reference(), aAffecter);
        }
        Map<String, Envoi> envois = resoudre(regles.ciblesOperation(f));
        Reference reference = Reference.operationHomologation(f.operationId());
        String sousTitre = regles.sousTitreOperation(f);
        return notifier(envois, reference, e -> regles.titreOperation(f, e.role()), sousTitre, e -> e.portail() ? regles.lienPortailOperation(f, e.role()) : regles.lienInterneOperation(f), le);
    }

    @Override
    public int jalonHomologation(FaitsJalon f, Instant le) {
        if (f == null || vide(f.liaisonId())) {
            return 0;
        }
        Map<String, Envoi> envois = resoudre(regles.ciblesJalon(f, jalonsPortail));
        String titre = regles.titreJalon(f);
        return notifier(envois, Reference.liaisonHomologation(f.liaisonId()), e -> titre, regles.sousTitreJalon(f), e -> e.portail() ? regles.lienPortailJalon(f) : regles.lienInterneJalon(f), le);
    }

    /** Cibles résolues en comptes : une institution devient ses comptes actifs, le rôle le plus fort l'emporte. */
    private Map<String, Envoi> resoudre(Map<Cible, Role> cibles) {
        Map<String, Envoi> r = new LinkedHashMap<>();
        cibles.forEach((cible, role) -> {
            if (cible.estInstitution()) {
                for (String compte : institutions.comptes(cible.institutionId())) {
                    retenir(r, compte, new Envoi(role, true));
                }
            } else {
                retenir(r, cible.compteId(), new Envoi(role, false));
            }
        });
        return r;
    }

    private static void retenir(Map<String, Envoi> r, String compte, Envoi e) {
        if (vide(compte)) {
            return;
        }
        Envoi existant = r.get(compte.strip());
        if (existant == null || ReglesHomologation.plusFort(e.role(), existant.role())) {
            r.put(compte.strip(), e);
        }
    }

    private int notifier(Map<String, Envoi> envois, Reference reference, Function<Envoi, String> titre, String sousTitre, Function<Envoi, String> lien, Instant le) {
        Instant instant = (le == null ? horloge.instant() : le).truncatedTo(ChronoUnit.MICROS);
        int n = 0;
        for (Map.Entry<String, Envoi> e : envois.entrySet()) {
            String compte = e.getKey();
            if (!preferences.findById(compte).map(p -> p.accepte(TypeNotification.HOMOLOGATION)).orElse(true)) {
                continue;
            }
            // Une notification par événement et par compte : jamais regroupée ; aucun acteur (événement du moteur).
            NotificationUtilisateur notification = new NotificationUtilisateur(
                compte,
                TypeNotification.HOMOLOGATION,
                titre.apply(e.getValue()),
                sousTitre,
                lien.apply(e.getValue()),
                reference,
                null,
                null,
                instant
            );
            publier(notifications.save(notification));
            n++;
        }
        return n;
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

    private static boolean vide(String s) {
        return s == null || s.isBlank();
    }
}
