package cm.gimac.des.notification.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cm.gimac.des.notification.application.port.in.BoiteNotificationsUseCase.ReferenceSaisie;
import cm.gimac.des.notification.application.port.in.RecevoirEvenementsUseCase.DemandeDecidee;
import cm.gimac.des.notification.application.port.in.RecevoirEvenementsUseCase.DemandeSoumise;
import cm.gimac.des.notification.application.port.in.RecevoirEvenementsUseCase.EvenementProcessus;
import cm.gimac.des.notification.application.port.in.RecevoirEvenementsUseCase.MessageEnvoye;
import cm.gimac.des.notification.application.port.in.RecevoirEvenementsUseCase.RapportSoumis;
import cm.gimac.des.notification.application.port.in.RecevoirEvenementsUseCase.TacheAffectee;
import cm.gimac.des.notification.application.port.in.RecevoirEvenementsUseCase.TacheOfferte;
import cm.gimac.des.notification.application.port.in.RecevoirEvenementsUseCase.TacheRelancee;
import cm.gimac.des.notification.application.port.in.RecevoirEvenementsUseCase.TransitionTranchee;
import cm.gimac.des.notification.application.port.out.AnnuairePort;
import cm.gimac.des.notification.application.port.out.ConversationsPort;
import cm.gimac.des.notification.domain.boite.NotificationUtilisateur;
import cm.gimac.des.notification.domain.boite.PreferencesNotification;
import cm.gimac.des.notification.domain.boite.Reference;
import cm.gimac.des.notification.domain.boite.TypeNotification;
import cm.gimac.des.notification.domain.event.NotificationCreeePayload;
import cm.gimac.des.notification.domain.exception.NotificationException;
import cm.gimac.des.notification.domain.service.ReglesNotifications;
import cm.gimac.des.notification.domain.service.ReglesNotifications.FaitsProcessus;
import cm.gimac.des.notification.domain.service.ReglesNotifications.NatureProcessus;
import cm.gimac.des.notification.infrastructure.messaging.outbox.OutboxPublisher;
import cm.gimac.des.notification.repository.NotificationUtilisateurRepository;
import cm.gimac.des.notification.repository.PreferencesNotificationRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BoiteNotificationsServiceTest {

    static final Instant T = Instant.parse("2026-10-05T08:00:00Z");

    @Mock
    NotificationUtilisateurRepository notifications;

    @Mock
    PreferencesNotificationRepository preferences;

    @Mock
    AnnuairePort annuaire;

    @Mock
    ConversationsPort conversations;

    @Mock
    OutboxPublisher outbox;

    BoiteNotificationsService service;

    @BeforeEach
    void init() {
        service = new BoiteNotificationsService(notifications, preferences, annuaire, conversations, outbox, new ReglesNotifications(), Clock.fixed(T, ZoneOffset.UTC));
        lenient().when(notifications.save(any())).thenAnswer(i -> i.getArgument(0));
        lenient().when(preferences.findById(anyString())).thenReturn(Optional.empty());
        lenient().when(annuaire.nomAffiche("paul")).thenReturn(Optional.of("Paul Mbarga Atangana"));
        lenient().when(annuaire.nomAffiche("serge")).thenReturn(Optional.of("Serge Ndzana"));
        lenient().when(conversations.conversation(anyString())).thenReturn(Optional.empty());
        lenient().when(notifications.nonLuePour(anyString(), any(), anyString(), anyString(), anyString())).thenReturn(Optional.empty());
    }

    List<NotificationUtilisateur> enregistrees(int n) {
        ArgumentCaptor<NotificationUtilisateur> c = ArgumentCaptor.forClass(NotificationUtilisateur.class);
        verify(notifications, times(n)).save(c.capture());
        return c.getAllValues();
    }

    List<NotificationCreeePayload> publiees(int n) {
        ArgumentCaptor<Object> c = ArgumentCaptor.forClass(Object.class);
        verify(outbox, times(n)).publish(eq("notification"), any(UUID.class), eq(NotificationCreeePayload.TYPE), c.capture());
        return c.getAllValues().stream().map(NotificationCreeePayload.class::cast).toList();
    }

    @Test
    @DisplayName("Message : une notification par destinataire, jamais l'auteur ; événement publié au seul destinataire")
    void messageParDestinataireSansAuteur() {
        when(conversations.conversation("c-1")).thenReturn(Optional.of(new ConversationsPort.Conversation("c-1", "GROUPE", "Groupe Cadrage L-12", List.of())));
        int n = service.messageEnvoye(new MessageEnvoye("c-1", "m-1", "paul", List.of("aminata", "paul", "serge", "aminata"), T, null));
        assertThat(n).isEqualTo(2);
        List<NotificationUtilisateur> l = enregistrees(2);
        assertThat(l).extracting(NotificationUtilisateur::getDestinataireId).containsExactly("aminata", "serge");
        NotificationUtilisateur a = l.get(0);
        assertThat(a.getType()).isEqualTo(TypeNotification.MESSAGE);
        assertThat(a.getTitre()).isEqualTo("Paul Mbarga Atangana");
        assertThat(a.getSousTitre()).isEqualTo("Groupe Cadrage L-12");
        assertThat(a.getLien()).isEqualTo("/des/messagerie?conversation=c-1");
        assertThat(a.getReference()).isEqualTo(Reference.conversation("c-1"));
        assertThat(a.getActeurId()).isEqualTo("paul");
        assertThat(a.getNombre()).isEqualTo(1);
        List<NotificationCreeePayload> p = publiees(2);
        assertThat(p).extracting(NotificationCreeePayload::destinataires).containsExactly(List.of("aminata"), List.of("serge"));
        assertThat(p.get(0).type()).isEqualTo("MESSAGE");
        assertThat(p.get(0).referenceId()).isEqualTo("c-1");
        verify(annuaire, times(1)).nomAffiche("paul");
    }

    @Test
    @DisplayName("Consolidation : la notification non lue de la conversation est incrémentée et remontée, pas recréée")
    void consolidation() {
        NotificationUtilisateur existante = new NotificationUtilisateur(
            "aminata",
            TypeNotification.MESSAGE,
            "Paul Mbarga Atangana",
            null,
            "/des/messagerie?conversation=c-1",
            Reference.conversation("c-1"),
            "paul",
            "Paul Mbarga Atangana",
            T.minusSeconds(60)
        );
        when(notifications.nonLuePour("aminata", TypeNotification.MESSAGE, "collaboration", "CONVERSATION", "c-1")).thenReturn(Optional.of(existante));
        service.messageEnvoye(new MessageEnvoye("c-1", "m-2", "serge", List.of("aminata"), T, null));
        NotificationUtilisateur n = enregistrees(1).get(0);
        assertThat(n).isSameAs(existante);
        assertThat(n.getNombre()).isEqualTo(2);
        assertThat(n.getTitre()).isEqualTo("Serge Ndzana");
        assertThat(n.getMisAJourLe()).isEqualTo(T);
        assertThat(publiees(1).get(0).nombre()).isEqualTo(2);
    }

    @Test
    @DisplayName("Événement de groupe (ajout, retrait, renommage) : aucune notification")
    void evenementDeGroupeIgnore() {
        assertThat(service.messageEnvoye(new MessageEnvoye("c-1", "m-1", "paul", List.of("aminata"), T, "MEMBRES_AJOUTES"))).isZero();
        verify(notifications, never()).save(any());
        verify(outbox, never()).publish(anyString(), any(UUID.class), anyString(), any());
    }

    @Test
    @DisplayName("Préférences : un type désactivé ne crée plus de notification pour ce destinataire")
    void typeDesactive() {
        PreferencesNotification p = PreferencesNotification.defauts("aminata", T);
        p.modifier(null, null, Map.of(TypeNotification.MESSAGE, false), T);
        when(preferences.findById("aminata")).thenReturn(Optional.of(p));
        assertThat(service.messageEnvoye(new MessageEnvoye("c-1", "m-1", "paul", List.of("aminata", "serge"), T, null))).isEqualTo(1);
        assertThat(enregistrees(1).get(0).getDestinataireId()).isEqualTo("serge");
    }

    @Test
    @DisplayName("Annuaire et messagerie injoignables : notification créée avec un titre neutre, sans sous-titre")
    void degradation() {
        when(annuaire.nomAffiche("paul")).thenThrow(NotificationException.indisponible("NOT_ANNUAIRE_INDISPONIBLE", "x"));
        when(conversations.conversation("c-1")).thenThrow(NotificationException.indisponible("NOT_COLLABORATION_INDISPONIBLE", "x"));
        service.messageEnvoye(new MessageEnvoye("c-1", "m-1", "paul", List.of("aminata"), T, null));
        NotificationUtilisateur n = enregistrees(1).get(0);
        assertThat(n.getTitre()).isEqualTo("Nouveau message");
        assertThat(n.getSousTitre()).isNull();
    }

    @Test
    @DisplayName("Tâche : au nouveau responsable, sauf s'il est l'acteur ; rapport : au destinataire, jamais à l'auteur")
    void tacheEtRapport() {
        assertThat(service.tacheAffectee(new TacheAffectee("t-1", "paul", "paul", T, null, null))).isZero();
        assertThat(service.tacheAffectee(new TacheAffectee("t-1", null, "paul", T, null, null))).isZero();
        assertThat(service.tacheAffectee(new TacheAffectee("t-1", "aminata", "paul", T, null, "2026-10-09"))).isEqualTo(1);
        assertThat(service.rapportSoumis(new RapportSoumis("r-1", "QUOTIDIEN", "2026-10-05", "serge", null, "serge", T))).isZero();
        assertThat(service.rapportSoumis(new RapportSoumis("r-1", "QUOTIDIEN", "2026-10-05", "serge", "serge", "serge", T))).isZero();
        assertThat(service.rapportSoumis(new RapportSoumis("r-1", "QUOTIDIEN", "2026-10-05", "serge", "paul", "serge", T))).isEqualTo(1);
        List<NotificationUtilisateur> l = enregistrees(2);
        assertThat(l.get(0).getType()).isEqualTo(TypeNotification.TACHE);
        assertThat(l.get(0).getTitre()).isEqualTo("Tâche confiée par Paul Mbarga Atangana");
        assertThat(l.get(0).getSousTitre()).isEqualTo("Échéance : 2026-10-09");
        assertThat(l.get(0).getLien()).isEqualTo("/des/taches?tache=t-1");
        assertThat(l.get(1).getDestinataireId()).isEqualTo("paul");
        assertThat(l.get(1).getTitre()).isEqualTo("Rapport de Serge Ndzana");
        assertThat(l.get(1).getReference()).isEqualTo(Reference.rapport("r-1"));
        verify(notifications, never()).nonLuePour(anyString(), eq(TypeNotification.TACHE), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Lecture : notification d'un autre en 404 ; marquage par type et objet ; référence incomplète refusée")
    void lecture() {
        UUID id = UUID.randomUUID();
        when(notifications.findByIdAndDestinataireId(id, "aminata")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.marquerLue("aminata", id)).isInstanceOf(NotificationException.class).extracting("code").isEqualTo("NOT_NOTIFICATION_INTROUVABLE");

        NotificationUtilisateur c1 = new NotificationUtilisateur("aminata", TypeNotification.MESSAGE, "P", null, "/", Reference.conversation("c-1"), "paul", "P", T).dansConversation("c-1");
        NotificationUtilisateur c2 = new NotificationUtilisateur("aminata", TypeNotification.MESSAGE, "P", null, "/", Reference.conversation("c-2"), "paul", "P", T).dansConversation("c-2");
        NotificationUtilisateur t1 = new NotificationUtilisateur("aminata", TypeNotification.TACHE, "T", null, "/", Reference.tache("t-1"), "paul", "P", T);
        when(notifications.findByDestinataireIdAndLueLeIsNull("aminata")).thenReturn(List.of(c1, c2, t1));
        when(notifications.findByDestinataireIdAndConversationIdAndLueLeIsNull("aminata", "c-1")).thenReturn(List.of(c1));
        assertThat(service.marquerLues("aminata", "MESSAGE", new ReferenceSaisie("collaboration", "CONVERSATION", "c-1"))).isEqualTo(1);
        assertThat(c1.estLue()).isTrue();
        assertThat(c2.estLue()).isFalse();
        assertThat(service.marquerLues("aminata", "TACHE", null)).isEqualTo(1);
        assertThat(t1.estLue()).isTrue();
        assertThatThrownBy(() -> service.marquerLues("aminata", null, new ReferenceSaisie("collaboration", null, "c-1")))
            .isInstanceOf(NotificationException.class)
            .extracting("code")
            .isEqualTo("NOT_REQUETE_INVALIDE");
    }

    @Test
    @DisplayName("Purge : limite de conservation à 90 jours")
    void purge() {
        when(notifications.purger(Instant.parse("2026-07-07T08:00:00Z"))).thenReturn(3);
        assertThat(service.purger()).isEqualTo(3);
    }

    @Test
    @DisplayName("Tâche à prendre : une notification TACHE par membre de l'unité, sauf l'acteur, lien vers les tâches à prendre")
    void tacheOfferte() {
        int n = service.tacheOfferte(new TacheOfferte("t-1", "Sauvegardes", "POLE_RESEAU", List.of("aminata", "paul", "serge"), "paul", T));
        assertThat(n).isEqualTo(2);
        List<NotificationUtilisateur> l = enregistrees(2);
        assertThat(l).extracting(NotificationUtilisateur::getDestinataireId).containsExactly("aminata", "serge");
        assertThat(l.get(0).getType()).isEqualTo(TypeNotification.TACHE);
        assertThat(l.get(0).getTitre()).isEqualTo("Sauvegardes");
        assertThat(l.get(0).getSousTitre()).isEqualTo("À prendre : POLE_RESEAU");
        assertThat(l.get(0).getLien()).isEqualTo("/des/taches?vue=unite");
        assertThat(l.get(0).getReference()).isEqualTo(Reference.tache("t-1"));
        assertThat(publiees(2)).extracting(NotificationCreeePayload::type).containsOnly("TACHE");
    }

    @Test
    @DisplayName("D-98 : offre d'une décision (transition à prononcer) : lien profond vers l'écran de la décision")
    void tacheOfferteAvecLien() {
        int n = service.tacheOfferte(
            new TacheOfferte("t-9", "Prononcer l'adhésion : Banque B", null, List.of("chef", "demandeur"), "demandeur", T, "/des/institutions/i-1?onglet=transitions")
        );
        assertThat(n).isEqualTo(1);
        NotificationUtilisateur l = enregistrees(1).get(0);
        assertThat(l.getDestinataireId()).isEqualTo("chef");
        assertThat(l.getTitre()).isEqualTo("Prononcer l'adhésion : Banque B");
        assertThat(l.getSousTitre()).isEqualTo("À prendre");
        assertThat(l.getLien()).isEqualTo("/des/institutions/i-1?onglet=transitions");
        assertThat(l.getReference()).isEqualTo(Reference.tache("t-9"));
    }

    @Test
    @DisplayName("Relance : notification TACHE au responsable, lien vers la tâche ; relance de soi-même : rien")
    void tacheRelancee() {
        assertThat(service.tacheRelancee(new TacheRelancee("t-2", "Sauvegardes", "serge", "paul", T))).isEqualTo(1);
        NotificationUtilisateur n = enregistrees(1).get(0);
        assertThat(n.getDestinataireId()).isEqualTo("serge");
        assertThat(n.getTitre()).isEqualTo("Relance : Sauvegardes");
        assertThat(n.getSousTitre()).isEqualTo("Relancée par Paul Mbarga Atangana");
        assertThat(n.getLien()).isEqualTo("/des/taches?tache=t-2");
        assertThat(service.tacheRelancee(new TacheRelancee("t-3", "x", "paul", "paul", T))).isZero();
    }

    @Test
    @DisplayName("Demande soumise : notification DEMANDE à l'administration (sauf le demandeur), lien vers les demandes à traiter")
    void demandeSoumise() {
        int n = service.demandeSoumise(new DemandeSoumise("d-1", "CREATION_COMPTE", "paul", List.of("admin", "rssi", "paul"), T));
        assertThat(n).isEqualTo(2);
        List<NotificationUtilisateur> l = enregistrees(2);
        assertThat(l).extracting(NotificationUtilisateur::getDestinataireId).containsExactly("admin", "rssi");
        assertThat(l.get(0).getType()).isEqualTo(TypeNotification.DEMANDE);
        assertThat(l.get(0).getTitre()).isEqualTo("Demande de Paul Mbarga Atangana");
        assertThat(l.get(0).getSousTitre()).isEqualTo("Création de compte");
        assertThat(l.get(0).getLien()).isEqualTo("/des/administration/demandes?demande=d-1");
        assertThat(l.get(0).getReference()).isEqualTo(Reference.demande("d-1"));
        assertThat(publiees(2)).extracting(NotificationCreeePayload::type).containsOnly("DEMANDE");
    }

    @Test
    @DisplayName("Demande décidée : notification DEMANDE au demandeur, lien vers ses demandes ; type DEMANDE désactivé : rien")
    void demandeDecidee() {
        assertThat(service.demandeDecidee(new DemandeDecidee("d-1", "PROFILS", "ACCEPTEE", "serge", List.of("paul"), T))).isEqualTo(1);
        NotificationUtilisateur n = enregistrees(1).get(0);
        assertThat(n.getDestinataireId()).isEqualTo("paul");
        assertThat(n.getTitre()).isEqualTo("Demande acceptée");
        assertThat(n.getLien()).isEqualTo("/des/equipe/demandes?demande=d-1");
        assertThat(n.getActeurNom()).isEqualTo("Serge Ndzana");
        PreferencesNotification p = PreferencesNotification.defauts("aminata", T);
        p.modifier(null, null, Map.of(TypeNotification.DEMANDE, false), T);
        when(preferences.findById("aminata")).thenReturn(Optional.of(p));
        assertThat(service.demandeDecidee(new DemandeDecidee("d-2", "STATUT", "REFUSEE", "serge", List.of("aminata"), T))).isZero();
    }

    @Test
    @DisplayName("DDR-08 : proposition de statut refusée puis prononcée, notifiée à son proposant (jamais à l'approbateur), lien vers les propositions")
    void transitionTranchee() {
        String lien = "/des/institutions/i-1?onglet=transitions";
        assertThat(service.transitionTranchee(new TransitionTranchee("i-1", "t-1", "Banque Atlantique", "ADHERENT", "REJETEE", "Statuts à jour exigés", "aminata", "paul", lien, T))).isEqualTo(1);
        assertThat(service.transitionTranchee(new TransitionTranchee("i-1", "t-2", "Banque Atlantique", "ADHERENT", "APPROUVEE", null, "aminata", "paul", "https://ailleurs/x", T))).isEqualTo(1);
        List<NotificationUtilisateur> l = enregistrees(2);
        assertThat(l).extracting(NotificationUtilisateur::getDestinataireId).containsOnly("aminata");
        assertThat(l.get(0).getType()).isEqualTo(TypeNotification.DEMANDE);
        assertThat(l.get(0).getTitre()).isEqualTo("Proposition de statut refusée : Banque Atlantique");
        assertThat(l.get(0).getSousTitre()).isEqualTo("Motif : Statuts à jour exigés");
        assertThat(l.get(0).getLien()).isEqualTo(lien);
        assertThat(l.get(0).getReference()).isEqualTo(Reference.transition("t-1"));
        assertThat(l.get(0).getActeurNom()).isEqualTo("Paul Mbarga Atangana");
        assertThat(l.get(1).getTitre()).isEqualTo("Adhésion prononcée : Banque Atlantique");
        assertThat(l.get(1).getSousTitre()).isNull();
        // Lien hors de l'application interne : la fiche de l'institution le remplace.
        assertThat(l.get(1).getLien()).isEqualTo(lien);
        // Le proposant qui trancherait lui-même (impossible : maker-checker) ne serait pas notifié.
        assertThat(service.transitionTranchee(new TransitionTranchee("i-1", "t-3", "B", "ADHERENT", "APPROUVEE", null, "paul", "paul", lien, T))).isZero();
    }

    // ------------------------------------------------------------------ mentions et réponses (D-82)

    @Test
    @DisplayName("Mention : notification MENTION non regroupée, en plus du message ; titre, sous-titre du groupe, lien et référence du message")
    void mention() {
        when(conversations.conversation("c-1")).thenReturn(Optional.of(new ConversationsPort.Conversation("c-1", "GROUPE", "Groupe Cadrage L-12", List.of())));
        int n = service.messageEnvoye(new MessageEnvoye("c-1", "m-1", "paul", List.of("aminata", "serge"), T, null, null, List.of("serge")));
        assertThat(n).isEqualTo(3);
        List<NotificationUtilisateur> l = enregistrees(3);
        assertThat(l).extracting(NotificationUtilisateur::getType).containsExactly(TypeNotification.MESSAGE, TypeNotification.MESSAGE, TypeNotification.MENTION);
        NotificationUtilisateur m = l.get(2);
        assertThat(m.getDestinataireId()).isEqualTo("serge");
        assertThat(m.getTitre()).isEqualTo("Paul Mbarga Atangana vous a mentionné");
        assertThat(m.getSousTitre()).isEqualTo("Groupe Cadrage L-12");
        assertThat(m.getLien()).isEqualTo("/des/messagerie?conversation=c-1&message=m-1");
        assertThat(m.getReference()).isEqualTo(Reference.message("m-1"));
        assertThat(m.getConversationId()).isEqualTo("c-1");
        assertThat(m.getActeurId()).isEqualTo("paul");
        assertThat(m.getNombre()).isEqualTo(1);
        assertThat(l.get(0).getConversationId()).isEqualTo("c-1");
        List<NotificationCreeePayload> p = publiees(3);
        assertThat(p.get(2).type()).isEqualTo("MENTION");
        assertThat(p.get(2).destinataires()).containsExactly("serge");
        assertThat(p.get(2).referenceType()).isEqualTo("MESSAGE");
        assertThat(p.get(2).referenceId()).isEqualTo("m-1");
        // Jamais regroupée : aucune recherche de notification non lue pour ce type.
        verify(notifications, never()).nonLuePour(anyString(), eq(TypeNotification.MENTION), anyString(), anyString(), anyString());
        verify(annuaire, times(1)).nomAffiche("paul");
        verify(conversations, times(1)).conversation("c-1");
    }

    @Test
    @DisplayName("Réponse : REPONSE à l'auteur cité ; cumul mention et réponse : une seule notification, MENTION ; deux messages : deux notifications")
    void reponseEtCumul() {
        service.messageEnvoye(new MessageEnvoye("c-1", "m-1", "paul", List.of("aminata", "serge"), T, null, "aminata", List.of()));
        service.messageEnvoye(new MessageEnvoye("c-1", "m-2", "paul", List.of("aminata", "serge"), T.plusSeconds(1), null, "aminata", List.of("aminata", "serge")));
        List<NotificationUtilisateur> propres = enregistrees(7).stream().filter(n -> n.getType() != TypeNotification.MESSAGE).toList();
        assertThat(propres).hasSize(3);
        NotificationUtilisateur r = propres.get(0);
        assertThat(r.getType()).isEqualTo(TypeNotification.REPONSE);
        assertThat(r.getDestinataireId()).isEqualTo("aminata");
        assertThat(r.getTitre()).isEqualTo("Paul Mbarga Atangana a répondu à votre message");
        assertThat(r.getSousTitre()).isNull();
        assertThat(r.getLien()).isEqualTo("/des/messagerie?conversation=c-1&message=m-1");
        assertThat(propres.subList(1, 3)).extracting(NotificationUtilisateur::getDestinataireId, NotificationUtilisateur::getType).containsExactly(
            org.assertj.core.groups.Tuple.tuple("aminata", TypeNotification.MENTION),
            org.assertj.core.groups.Tuple.tuple("serge", TypeNotification.MENTION)
        );
        assertThat(propres).extracting(NotificationUtilisateur::getReference).containsExactly(Reference.message("m-1"), Reference.message("m-2"), Reference.message("m-2"));
    }

    @Test
    @DisplayName("Mention et réponse : jamais l'auteur ; événement de groupe ignoré ; ancien événement : message seul ; non-membre écarté")
    void exclusions() {
        // Réponse à soi et mention de soi : rien de plus que le message.
        assertThat(service.messageEnvoye(new MessageEnvoye("c-1", "m-1", "paul", List.of("aminata"), T, null, "paul", List.of("paul")))).isEqualTo(1);
        // Événement de groupe : rien, même avec des champs D-82.
        assertThat(service.messageEnvoye(new MessageEnvoye("c-1", "m-2", "paul", List.of("aminata"), T, "MEMBRES_AJOUTES", "aminata", List.of("aminata")))).isZero();
        // Ancien événement (sans les champs) : message seul.
        assertThat(service.messageEnvoye(new MessageEnvoye("c-1", "m-3", "paul", List.of("aminata"), T, null))).isEqualTo(1);
        // Mention d'un compte qui n'est pas destinataire : écartée.
        assertThat(service.messageEnvoye(new MessageEnvoye("c-1", "m-4", "paul", List.of("aminata"), T, null, "rachel", List.of("rachel")))).isEqualTo(1);
        assertThat(enregistrees(3)).extracting(NotificationUtilisateur::getType).containsOnly(TypeNotification.MESSAGE);
    }

    @Test
    @DisplayName("Préférences : MENTION désactivé, rien pour la mention mais le message reste ; MESSAGE désactivé, la mention reste")
    void preferencesMentionReponse() {
        PreferencesNotification a = PreferencesNotification.defauts("aminata", T);
        a.modifier(null, null, Map.of(TypeNotification.MENTION, false, TypeNotification.REPONSE, false), T);
        PreferencesNotification s = PreferencesNotification.defauts("serge", T);
        s.modifier(null, null, Map.of(TypeNotification.MESSAGE, false), T);
        when(preferences.findById("aminata")).thenReturn(Optional.of(a));
        when(preferences.findById("serge")).thenReturn(Optional.of(s));
        assertThat(service.messageEnvoye(new MessageEnvoye("c-1", "m-1", "paul", List.of("aminata", "serge"), T, null, "aminata", List.of("aminata", "serge")))).isEqualTo(2);
        assertThat(enregistrees(2)).extracting(NotificationUtilisateur::getDestinataireId, NotificationUtilisateur::getType).containsExactly(
            org.assertj.core.groups.Tuple.tuple("aminata", TypeNotification.MESSAGE),
            org.assertj.core.groups.Tuple.tuple("serge", TypeNotification.MENTION)
        );
    }

    @Test
    @DisplayName("Lecture d'une conversation : marque aussi ses mentions et réponses ; le filtre de type reste appliqué")
    void lectureConversation() {
        NotificationUtilisateur msg = new NotificationUtilisateur("aminata", TypeNotification.MESSAGE, "P", null, "/", Reference.conversation("c-1"), "paul", "P", T).dansConversation("c-1");
        NotificationUtilisateur men = new NotificationUtilisateur("aminata", TypeNotification.MENTION, "P", null, "/", Reference.message("m-1"), "paul", "P", T).dansConversation("c-1");
        NotificationUtilisateur rep = new NotificationUtilisateur("aminata", TypeNotification.REPONSE, "P", null, "/", Reference.message("m-2"), "paul", "P", T).dansConversation("c-1");
        when(notifications.findByDestinataireIdAndConversationIdAndLueLeIsNull("aminata", "c-1")).thenReturn(List.of(msg, men, rep));
        assertThat(service.marquerLues("aminata", "MENTION", new ReferenceSaisie("collaboration", "CONVERSATION", "c-1"))).isEqualTo(1);
        assertThat(men.estLue()).isTrue();
        assertThat(service.marquerLues("aminata", null, new ReferenceSaisie("collaboration", "CONVERSATION", "c-1"))).isEqualTo(2);
        assertThat(msg.estLue()).isTrue();
        assertThat(rep.estLue()).isTrue();
        verify(notifications, never()).findByDestinataireIdAndLueLeIsNull("aminata");
    }

    // ------------------------------------------------------------------ processus d'unité (D-87)

    static final String INSTANCE = "5b3c7a10-0000-4000-8000-0000000000aa";
    static final String LIEN = "/des/processus/instances/" + INSTANCE + "?etape=ANALYSE";

    static EvenementProcessus processus(NatureProcessus nature, String precision, String acteur, List<String> destinataires) {
        return new EvenementProcessus(
            INSTANCE,
            new FaitsProcessus(nature, "OUVERTURE_FLUX_RESEAU", "Flux vers la banque X", "ANALYSE", "Analyse de la demande", precision, false, false, "2026-10-09"),
            acteur,
            null,
            destinataires,
            LIEN,
            T
        );
    }

    @Test
    @DisplayName("D-87 : étape soumise, une notification PROCESSUS par destinataire (jamais l'acteur), titre, sous-titre, lien rendu tel quel, référence de l'instance, non regroupée")
    void processusEtapeSoumise() {
        int n = service.evenementProcessus(processus(NatureProcessus.ETAPE_SOUMISE, "SIGNATURE", "paul", List.of("serge", "paul", "aminata", "serge")));
        assertThat(n).isEqualTo(2);
        List<NotificationUtilisateur> l = enregistrees(2);
        assertThat(l).extracting(NotificationUtilisateur::getDestinataireId).containsExactly("serge", "aminata");
        NotificationUtilisateur s = l.get(0);
        assertThat(s.getType()).isEqualTo(TypeNotification.PROCESSUS);
        assertThat(s.getTitre()).isEqualTo("Signature attendue : Analyse de la demande");
        assertThat(s.getSousTitre()).isEqualTo("Flux vers la banque X, soumise par Paul Mbarga Atangana");
        assertThat(s.getLien()).isEqualTo(LIEN);
        assertThat(s.getReference()).isEqualTo(Reference.processus(INSTANCE));
        assertThat(s.getActeurId()).isEqualTo("paul");
        assertThat(s.getActeurNom()).isEqualTo("Paul Mbarga Atangana");
        assertThat(s.getNombre()).isEqualTo(1);
        assertThat(publiees(2)).allSatisfy(p -> {
            assertThat(p.type()).isEqualTo("PROCESSUS");
            assertThat(p.referenceService()).isEqualTo("pilotage");
            assertThat(p.referenceType()).isEqualTo("PROCESSUS");
            assertThat(p.referenceId()).isEqualTo(INSTANCE);
        });
        // Jamais regroupée : aucune recherche de notification non lue à regrouper.
        verify(notifications, never()).nonLuePour(anyString(), eq(TypeNotification.PROCESSUS), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("D-87 : étape activée (sans acteur) ; départ avec nom de la personne partie ; instance close au lanceur ; lien étranger remplacé")
    void processusAutresNatures() {
        assertThat(service.evenementProcessus(processus(NatureProcessus.ETAPE_ACTIVEE, null, null, List.of("aminata")))).isEqualTo(1);
        var depart = new EvenementProcessus(
            INSTANCE,
            new FaitsProcessus(NatureProcessus.ETAPE_REAFFECTEE, "OUVERTURE_FLUX_RESEAU", "Flux vers la banque X", "MISE_EN_OEUVRE", null, "DEPART", false, false, null),
            null,
            "serge",
            List.of("aminata", "chef"),
            "https://ailleurs.example/piege",
            T
        );
        assertThat(service.evenementProcessus(depart)).isEqualTo(2);
        assertThat(service.evenementProcessus(processus(NatureProcessus.INSTANCE_CLOSE, "ANNULEE", "serge", List.of("paul")))).isEqualTo(1);
        List<NotificationUtilisateur> l = enregistrees(4);
        assertThat(l.get(0).getTitre()).isEqualTo("Étape à réaliser : Analyse de la demande");
        assertThat(l.get(0).getSousTitre()).isEqualTo("Flux vers la banque X, échéance 2026-10-09");
        assertThat(l.get(0).getActeurId()).isNull();
        assertThat(l.get(1).getTitre()).isEqualTo("Étape à reprendre : MISE_EN_OEUVRE");
        assertThat(l.get(1).getSousTitre()).isEqualTo("Flux vers la banque X, après le départ de Serge Ndzana");
        assertThat(l.get(1).getLien()).isEqualTo("/des/processus/instances/" + INSTANCE);
        assertThat(l.get(3).getDestinataireId()).isEqualTo("paul");
        assertThat(l.get(3).getTitre()).isEqualTo("Processus annulé : Flux vers la banque X");
        assertThat(l.get(3).getSousTitre()).isEqualTo("OUVERTURE_FLUX_RESEAU, annulé par Serge Ndzana");
    }

    @Test
    @DisplayName("D-87 : type PROCESSUS désactivé, acteur seul destinataire, ou instance absente : aucune notification")
    void processusSansNotification() {
        PreferencesNotification p = PreferencesNotification.defauts("aminata", T);
        p.modifier(null, null, Map.of(TypeNotification.PROCESSUS, false), T);
        when(preferences.findById("aminata")).thenReturn(Optional.of(p));
        assertThat(service.evenementProcessus(processus(NatureProcessus.ETAPE_ACTIVEE, null, null, List.of("aminata")))).isZero();
        assertThat(service.evenementProcessus(processus(NatureProcessus.ETAPE_SIGNEE, "TERMINEE", "paul", List.of("paul")))).isZero();
        assertThat(service.evenementProcessus(new EvenementProcessus(null, null, null, null, List.of("serge"), LIEN, T))).isZero();
        assertThat(service.evenementProcessus(new EvenementProcessus(" ", null, null, null, List.of("serge"), LIEN, T))).isZero();
        verify(notifications, never()).save(any());
    }
}
