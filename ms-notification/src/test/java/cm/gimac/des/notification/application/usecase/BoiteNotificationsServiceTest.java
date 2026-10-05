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
import cm.gimac.des.notification.application.port.in.RecevoirEvenementsUseCase.MessageEnvoye;
import cm.gimac.des.notification.application.port.in.RecevoirEvenementsUseCase.RapportSoumis;
import cm.gimac.des.notification.application.port.in.RecevoirEvenementsUseCase.TacheAffectee;
import cm.gimac.des.notification.application.port.out.AnnuairePort;
import cm.gimac.des.notification.application.port.out.ConversationsPort;
import cm.gimac.des.notification.domain.boite.NotificationUtilisateur;
import cm.gimac.des.notification.domain.boite.PreferencesNotification;
import cm.gimac.des.notification.domain.boite.Reference;
import cm.gimac.des.notification.domain.boite.TypeNotification;
import cm.gimac.des.notification.domain.event.NotificationCreeePayload;
import cm.gimac.des.notification.domain.exception.NotificationException;
import cm.gimac.des.notification.domain.service.ReglesNotifications;
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

        NotificationUtilisateur c1 = new NotificationUtilisateur("aminata", TypeNotification.MESSAGE, "P", null, "/", Reference.conversation("c-1"), "paul", "P", T);
        NotificationUtilisateur c2 = new NotificationUtilisateur("aminata", TypeNotification.MESSAGE, "P", null, "/", Reference.conversation("c-2"), "paul", "P", T);
        NotificationUtilisateur t1 = new NotificationUtilisateur("aminata", TypeNotification.TACHE, "T", null, "/", Reference.tache("t-1"), "paul", "P", T);
        when(notifications.findByDestinataireIdAndLueLeIsNull("aminata")).thenReturn(List.of(c1, c2, t1));
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
}
