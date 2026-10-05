package cm.gimac.des.notification.application.usecase;

import cm.gimac.des.notification.application.port.in.BoiteNotificationsUseCase;
import cm.gimac.des.notification.application.port.in.RecevoirEvenementsUseCase;
import cm.gimac.des.notification.application.port.out.AnnuairePort;
import cm.gimac.des.notification.application.port.out.ConversationsPort;
import cm.gimac.des.notification.domain.boite.NotificationUtilisateur;
import cm.gimac.des.notification.domain.boite.PreferencesNotification;
import cm.gimac.des.notification.domain.boite.Reference;
import cm.gimac.des.notification.domain.boite.TypeNotification;
import cm.gimac.des.notification.domain.event.NotificationCreeePayload;
import cm.gimac.des.notification.domain.exception.NotificationException;
import cm.gimac.des.notification.domain.service.ReglesNotifications;
import cm.gimac.des.notification.domain.service.ReglesNotifications.Etat;
import cm.gimac.des.notification.infrastructure.messaging.outbox.OutboxPublisher;
import cm.gimac.des.notification.repository.NotificationUtilisateurRepository;
import cm.gimac.des.notification.repository.PreferencesNotificationRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Notifications de la plateforme (D-80, docs/lot1/notifications.md) : réception des événements de
 * MS-COLLABORATION et de MS-PILOTAGE, une notification par destinataire selon ses préférences,
 * consolidation par conversation, publication de notification.inapp-created.v1 par l'outbox ; boîte du
 * compte appelant (liste, compteur, lecture) et préférences. Aucun contenu de message n'est lu ni conservé.
 */
@Service
@Transactional
public class BoiteNotificationsService implements BoiteNotificationsUseCase, RecevoirEvenementsUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(BoiteNotificationsService.class);
    static final String AGREGAT = "notification";

    private final NotificationUtilisateurRepository notifications;
    private final PreferencesNotificationRepository preferences;
    private final AnnuairePort annuaire;
    private final ConversationsPort conversations;
    private final OutboxPublisher outbox;
    private final ReglesNotifications regles;
    private final Clock horloge;

    public BoiteNotificationsService(
        NotificationUtilisateurRepository notifications,
        PreferencesNotificationRepository preferences,
        AnnuairePort annuaire,
        ConversationsPort conversations,
        OutboxPublisher outbox,
        ReglesNotifications regles,
        Clock horloge
    ) {
        this.notifications = notifications;
        this.preferences = preferences;
        this.annuaire = annuaire;
        this.conversations = conversations;
        this.outbox = outbox;
        this.regles = regles;
        this.horloge = horloge;
    }

    // ------------------------------------------------------------------ réception des événements (§1)

    @Override
    public int messageEnvoye(MessageEnvoye e) {
        if (e.evenementGroupe() != null && !e.evenementGroupe().isBlank()) {
            // Création, renommage, ajout ou retrait de membres (D-68) : pas un message, pas de notification.
            return 0;
        }
        List<String> destinataires = acceptants(regles.destinataires(e.destinataires(), e.auteurId()), TypeNotification.MESSAGE);
        if (destinataires.isEmpty()) {
            return 0;
        }
        Noms noms = new Noms();
        String auteur = noms.de(e.auteurId());
        String sousTitre = nomDuGroupe(e.conversationId());
        Reference reference = Reference.conversation(e.conversationId());
        Instant le = instant(e.envoyeLe());
        for (String d : destinataires) {
            NotificationUtilisateur n = notifications
                .nonLuePour(d, TypeNotification.MESSAGE, reference.getService(), reference.getType(), reference.getId())
                .map(existante -> {
                    existante.regrouper(regles.titreMessage(auteur), sousTitre, e.auteurId(), auteur, le);
                    return existante;
                })
                .orElseGet(() ->
                    new NotificationUtilisateur(
                        d,
                        TypeNotification.MESSAGE,
                        regles.titreMessage(auteur),
                        sousTitre,
                        regles.lienConversation(e.conversationId()),
                        reference,
                        e.auteurId(),
                        auteur,
                        le
                    )
                );
            publier(notifications.save(n));
        }
        return destinataires.size();
    }

    @Override
    public int tacheAffectee(TacheAffectee e) {
        String responsable = e.responsableId();
        if (responsable == null || responsable.isBlank() || responsable.equals(e.acteurId()) || !accepte(responsable, TypeNotification.TACHE)) {
            return 0;
        }
        String acteur = new Noms().de(e.acteurId());
        NotificationUtilisateur n = new NotificationUtilisateur(
            responsable,
            TypeNotification.TACHE,
            regles.titreTache(e.titre(), acteur),
            regles.sousTitreTache(e.echeance()),
            regles.lienTache(e.tacheId()),
            Reference.tache(e.tacheId()),
            e.acteurId(),
            acteur,
            instant(e.le())
        );
        publier(notifications.save(n));
        return 1;
    }

    @Override
    public int rapportSoumis(RapportSoumis e) {
        String destinataire = e.destinataireId();
        String acteur = e.acteurId() == null ? e.auteurId() : e.acteurId();
        if (
            destinataire == null ||
            destinataire.isBlank() ||
            destinataire.equals(acteur) ||
            destinataire.equals(e.auteurId()) ||
            !accepte(destinataire, TypeNotification.RAPPORT)
        ) {
            return 0;
        }
        String auteur = new Noms().de(e.auteurId());
        NotificationUtilisateur n = new NotificationUtilisateur(
            destinataire,
            TypeNotification.RAPPORT,
            regles.titreRapport(auteur),
            regles.sousTitreRapport(e.type(), e.periode()),
            regles.lienRapport(e.rapportId()),
            Reference.rapport(e.rapportId()),
            e.auteurId(),
            auteur,
            instant(e.le())
        );
        publier(notifications.save(n));
        return 1;
    }

    @Override
    public int purger() {
        int n = notifications.purger(regles.limiteConservation(horloge.instant()));
        if (n > 0) {
            LOG.info("Purge des notifications de plus de {} jours : {} supprimée(s)", ReglesNotifications.CONSERVATION.toDays(), n);
        }
        return n;
    }

    /**
     * Nom d'une conversation de groupe (sous-titre, §2) ; conversation directe : null. MS-COLLABORATION
     * injoignable : la notification est créée sans sous-titre plutôt que perdue.
     */
    private String nomDuGroupe(String conversationId) {
        try {
            return conversations.conversation(conversationId).filter(ConversationsPort.Conversation::estGroupe).map(ConversationsPort.Conversation::nom).orElse(null);
        } catch (NotificationException ex) {
            LOG.warn("Conversation {} indisponible ({}) : notification sans sous-titre", conversationId, ex.code());
            return null;
        }
    }

    /** Destinataires dont les préférences acceptent le type. */
    private List<String> acceptants(List<String> destinataires, TypeNotification type) {
        return destinataires.stream().filter(d -> accepte(d, type)).toList();
    }

    private boolean accepte(String compte, TypeNotification type) {
        return preferences.findById(compte).map(p -> p.accepte(type)).orElse(true);
    }

    /** notification.inapp-created.v1 au seul destinataire, à la création comme au regroupement. */
    private void publier(NotificationUtilisateur n) {
        outbox.publish(
            AGREGAT,
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

    /** Instant de l'événement, à la microseconde (précision de la base) ; à défaut, maintenant. */
    private Instant instant(Instant le) {
        return (le == null ? horloge.instant() : le).truncatedTo(ChronoUnit.MICROS);
    }

    /**
     * Noms affichés lus dans l'annuaire, une fois par événement. Annuaire injoignable : la notification
     * est tout de même créée, avec un titre neutre, plutôt que perdue.
     */
    private final class Noms {

        private final Map<String, Optional<String>> lus = new HashMap<>();

        String de(String compte) {
            if (compte == null || compte.isBlank()) {
                return null;
            }
            return lus
                .computeIfAbsent(compte, c -> {
                    try {
                        return annuaire.nomAffiche(c);
                    } catch (NotificationException ex) {
                        LOG.warn("Nom de {} indisponible ({}) : titre neutre", c, ex.code());
                        return Optional.empty();
                    }
                })
                .orElse(null);
        }
    }

    // ------------------------------------------------------------------ boîte du compte (§3)

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationUtilisateur> lister(String compte, String etat, Integer page, Integer taille) {
        Etat e = regles.etat(etat);
        int p = regles.page(page);
        int t = regles.taille(taille);
        PageRequest requete = PageRequest.of(p, t, Sort.by(Sort.Order.desc("misAJourLe"), Sort.Order.desc("creeLe"), Sort.Order.asc("id")));
        var r = e == Etat.NON_LUES ? notifications.findByDestinataireIdAndLueLeIsNull(compte, requete) : notifications.findByDestinataireId(compte, requete);
        return new Page<>(r.getContent(), r.getTotalElements(), p, t);
    }

    @Override
    @Transactional(readOnly = true)
    public long nonLues(String compte) {
        return notifications.countByDestinataireIdAndLueLeIsNull(compte);
    }

    @Override
    public void marquerLue(String compte, UUID id) {
        notifications
            .findByIdAndDestinataireId(id, compte)
            .orElseThrow(() -> NotificationException.introuvable("NOT_NOTIFICATION_INTROUVABLE", id))
            .marquerLue(horloge.instant());
    }

    @Override
    public int marquerLues(String compte, String type, ReferenceSaisie reference) {
        TypeNotification t = type == null || type.isBlank() ? null : regles.type(type, "type");
        Reference r = null;
        if (reference != null) {
            if (vide(reference.service()) || vide(reference.type()) || vide(reference.id())) {
                throw NotificationException.champ("reference", "Référence attendue : service, type et id");
            }
            r = new Reference(reference.service().strip(), reference.type().strip(), reference.id().strip());
        }
        Instant le = horloge.instant();
        int marquees = 0;
        for (NotificationUtilisateur n : notifications.findByDestinataireIdAndLueLeIsNull(compte)) {
            if ((t == null || n.getType() == t) && (r == null || r.equals(n.getReference())) && n.marquerLue(le)) {
                marquees++;
            }
        }
        return marquees;
    }

    @Override
    @Transactional(readOnly = true)
    public PreferencesNotification preferences(String compte) {
        return preferences.findById(compte).orElseGet(() -> PreferencesNotification.defauts(compte, horloge.instant()));
    }

    @Override
    public PreferencesNotification modifierPreferences(String compte, PreferencesSaisie saisie) {
        if (saisie == null) {
            throw NotificationException.champ("preferences", "Corps de la requête absent");
        }
        Map<TypeNotification, Boolean> types = regles.types(saisie.types());
        Instant le = horloge.instant();
        PreferencesNotification p = preferences.findById(compte).orElseGet(() -> PreferencesNotification.defauts(compte, le));
        p.modifier(saisie.systeme(), saisie.son(), types, le);
        return preferences.save(p);
    }

    private static boolean vide(String s) {
        return s == null || s.isBlank();
    }
}
