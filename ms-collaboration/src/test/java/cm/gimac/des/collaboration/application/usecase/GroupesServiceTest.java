package cm.gimac.des.collaboration.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cm.gimac.des.collaboration.application.port.in.EnvoyerMessageUseCase.Expediteur;
import cm.gimac.des.collaboration.application.port.in.EnvoyerMessageUseCase.NouvelleConversation;
import cm.gimac.des.collaboration.application.port.out.ChiffrementPort;
import cm.gimac.des.collaboration.application.port.out.CollaboratorDirectoryPort;
import cm.gimac.des.collaboration.application.port.out.DocumentStoragePort;
import cm.gimac.des.collaboration.domain.event.MessageEnvoyePayload;
import cm.gimac.des.collaboration.domain.exception.CollaborationException;
import cm.gimac.des.collaboration.domain.messagerie.Collaborateur;
import cm.gimac.des.collaboration.domain.messagerie.Conversation;
import cm.gimac.des.collaboration.domain.messagerie.ConversationMembre;
import cm.gimac.des.collaboration.domain.messagerie.EvenementGroupe;
import cm.gimac.des.collaboration.domain.messagerie.Message;
import cm.gimac.des.collaboration.domain.messagerie.TypeConversation;
import cm.gimac.des.collaboration.domain.service.MasquagePan;
import cm.gimac.des.collaboration.domain.service.ReglesMessagerie;
import cm.gimac.des.collaboration.infrastructure.messaging.outbox.OutboxPublisher;
import cm.gimac.des.collaboration.repository.ConversationMembreRepository;
import cm.gimac.des.collaboration.repository.ConversationRepository;
import cm.gimac.des.collaboration.repository.LectureConversationRepository;
import cm.gimac.des.collaboration.repository.MessageRepository;
import cm.gimac.des.collaboration.repository.PieceJointeRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Groupes de discussion (D-68) : création nommée, administrateur, nom, membres, départ, événements. */
@ExtendWith(MockitoExtension.class)
class GroupesServiceTest {

    static final Instant T = Instant.parse("2026-10-02T08:00:00Z");
    static final Collaborateur MOI = new Collaborateur("moi", "Paul Mbarga", "Chef du DES", Set.of(), null, true);
    static final Collaborateur AMINATA = new Collaborateur("aminata", "Aminata Ngono", "Conductrice", Set.of(), null, true);
    static final Collaborateur SERGE = new Collaborateur("serge", "Serge Ndzana", "Ingénieur", Set.of(), null, true);
    static final Collaborateur FATIME = new Collaborateur("fatime", "Fatimé Abakar", "SNA", Set.of(), null, true);
    static final Expediteur EXP = new Expediteur("moi", "Paul");

    @Mock
    ConversationRepository conversations;

    @Mock
    ConversationMembreRepository membres;

    @Mock
    MessageRepository messages;

    @Mock
    PieceJointeRepository piecesJointes;

    @Mock
    CollaboratorDirectoryPort annuaire;

    @Mock
    DocumentStoragePort documents;

    @Mock
    ChiffrementPort chiffrement;

    @Mock
    OutboxPublisher outbox;

    @Mock
    LectureConversationRepository lectures;

    MessagerieService service;

    @BeforeEach
    void init() {
        service = new MessagerieService(
            conversations,
            membres,
            messages,
            lectures,
            piecesJointes,
            annuaire,
            documents,
            chiffrement,
            outbox,
            new ReglesMessagerie(),
            new MasquagePan(),
            Clock.fixed(T, ZoneOffset.UTC)
        );
        for (Collaborateur c : List.of(MOI, AMINATA, SERGE, FATIME)) {
            lenient().when(annuaire.trouver(c.id())).thenReturn(Optional.of(c));
        }
        lenient().when(chiffrement.chiffrer(anyString(), any())).thenReturn(new byte[] { 1 });
    }

    /** Groupe « Recette EP A » : moi (administrateur), Aminata et Serge. */
    Conversation groupe() {
        Conversation c = new Conversation(TypeConversation.GROUPE, "Recette EP A", null, null, null, "moi", T.minusSeconds(60));
        c.ajouterMembre(MOI, T.minusSeconds(60)).nommerAdministrateur();
        c.ajouterMembre(AMINATA, T.minusSeconds(50));
        c.ajouterMembre(SERGE, T.minusSeconds(40));
        lenient().when(conversations.pourMembre(eq(c.getId()), anyString())).thenAnswer(i -> c.membre(i.getArgument(1)).map(m -> c));
        return c;
    }

    List<Message> messagesEnregistres() {
        ArgumentCaptor<Message> m = ArgumentCaptor.forClass(Message.class);
        verify(messages, atLeastOnce()).save(m.capture());
        return m.getAllValues();
    }

    MessageEnvoyePayload dernierEvenement() {
        ArgumentCaptor<Object> p = ArgumentCaptor.forClass(Object.class);
        verify(outbox, atLeastOnce()).publish(eq("conversation"), any(java.util.UUID.class), eq(MessageEnvoyePayload.TYPE), p.capture());
        return (MessageEnvoyePayload) p.getAllValues().get(p.getAllValues().size() - 1);
    }

    @Test
    void groupeNommeMemeAvecUnSeulAutreMembreEtSansMessage() {
        var r = service.creerConversation(EXP, new NouvelleConversation(List.of("aminata"), "  Comité  ", null, null, null, List.of(), true), List.of());
        ArgumentCaptor<Conversation> c = ArgumentCaptor.forClass(Conversation.class);
        verify(conversations).save(c.capture());
        assertThat(r.creee()).isTrue();
        assertThat(c.getValue().getType()).isEqualTo(TypeConversation.GROUPE);
        assertThat(c.getValue().getTitre()).isEqualTo("Comité");
        assertThat(c.getValue().getCleDirecte()).isNull();
        assertThat(c.getValue().membre("moi")).get().extracting(ConversationMembre::isAdministrateur).isEqualTo(true);
        assertThat(c.getValue().membre("aminata")).get().extracting(ConversationMembre::isAdministrateur).isEqualTo(false);
        List<Message> m = messagesEnregistres();
        assertThat(m).hasSize(1);
        assertThat(m.get(0).getEvenement()).isEqualTo(EvenementGroupe.GROUPE_CREE);
        assertThat(m.get(0).getEvenementCibles()).containsExactly("Comité");
        assertThat(dernierEvenement().destinataires()).containsExactly("aminata");
        verify(conversations, never()).findByCleDirecte(anyString());
    }

    @Test
    void groupeAvecPremierMessageApresLEvenementDeCreation() {
        service.creerConversation(EXP, new NouvelleConversation(List.of("aminata", "serge"), "Comité", null, null, "Bonjour à tous", List.of(), true), List.of());
        List<Message> m = messagesEnregistres();
        assertThat(m).extracting(Message::getEvenement).containsExactly(EvenementGroupe.GROUPE_CREE, null);
        assertThat(m.get(1).getEnvoyeLe()).isAfter(m.get(0).getEnvoyeLe());
        // D-80 : l'événement de groupe est nommé dans collaboration.message-sent.v1, un vrai message n'en porte aucun.
        ArgumentCaptor<Object> p = ArgumentCaptor.forClass(Object.class);
        verify(outbox, org.mockito.Mockito.times(2)).publish(eq("conversation"), any(java.util.UUID.class), eq(MessageEnvoyePayload.TYPE), p.capture());
        assertThat(p.getAllValues()).extracting(e -> ((MessageEnvoyePayload) e).evenement()).containsExactly("GROUPE_CREE", null);
    }

    @Test
    void groupeSansNomRefuse() {
        assertThatThrownBy(() -> service.creerConversation(EXP, new NouvelleConversation(List.of("aminata"), " ", null, null, "x", List.of(), true), List.of()))
            .isInstanceOf(CollaborationException.class)
            .extracting("code")
            .isEqualTo("COL_GROUP_TITLE_REQUIRED");
    }

    @Test
    void lAdministrateurRenommeLeGroupe() {
        Conversation c = groupe();
        service.renommer(EXP, c.getId(), "  Recette EP B ");
        assertThat(c.getTitre()).isEqualTo("Recette EP B");
        Message m = messagesEnregistres().get(0);
        assertThat(m.getEvenement()).isEqualTo(EvenementGroupe.GROUPE_RENOMME);
        assertThat(m.getEvenementCibles()).containsExactly("Recette EP B");
        assertThat(dernierEvenement().destinataires()).containsExactlyInAnyOrder("aminata", "serge");
    }

    @Test
    void memeNomSansEvenement() {
        Conversation c = groupe();
        service.renommer(EXP, c.getId(), "Recette EP A");
        verify(messages, never()).save(any());
    }

    @Test
    void unMembreNonAdministrateurNeGerePasLeGroupe() {
        Conversation c = groupe();
        Expediteur aminata = new Expediteur("aminata", "Aminata");
        assertThatThrownBy(() -> service.renommer(aminata, c.getId(), "Autre")).extracting("code").isEqualTo("COL_GROUP_ADMIN_REQUIRED");
        assertThatThrownBy(() -> service.ajouterMembres(aminata, c.getId(), List.of("fatime"))).extracting("code").isEqualTo("COL_GROUP_ADMIN_REQUIRED");
        assertThatThrownBy(() -> service.retirerMembre(aminata, c.getId(), "serge")).extracting("code").isEqualTo("COL_GROUP_ADMIN_REQUIRED");
    }

    @Test
    void uneConversationDirecteNeSeGerePas() {
        Conversation d = new Conversation(TypeConversation.DIRECTE, null, null, null, "aminata|moi", "moi", T);
        d.ajouterMembre(MOI, T);
        d.ajouterMembre(AMINATA, T);
        when(conversations.pourMembre(d.getId(), "moi")).thenReturn(Optional.of(d));
        assertThatThrownBy(() -> service.renommer(EXP, d.getId(), "X")).extracting("code").isEqualTo("COL_NOT_A_GROUP");
        assertThatThrownBy(() -> service.retirerMembre(EXP, d.getId(), "moi")).extracting("code").isEqualTo("COL_NOT_A_GROUP");
    }

    @Test
    void ajoutDeMembresIgnoreCeuxDejaPresents() {
        Conversation c = groupe();
        service.ajouterMembres(EXP, c.getId(), List.of("serge", "fatime"));
        assertThat(c.membre("fatime")).isPresent();
        Message m = messagesEnregistres().get(0);
        assertThat(m.getEvenement()).isEqualTo(EvenementGroupe.MEMBRES_AJOUTES);
        assertThat(m.getEvenementCibles()).containsExactly("Fatimé Abakar");
        assertThat(dernierEvenement().destinataires()).containsExactlyInAnyOrder("aminata", "serge", "fatime");
        assertThat(dernierEvenement().evenement()).isEqualTo("MEMBRES_AJOUTES");
    }

    @Test
    void ajoutSansNouveauMembreSansEvenement() {
        Conversation c = groupe();
        service.ajouterMembres(EXP, c.getId(), List.of("aminata"));
        verify(messages, never()).save(any());
    }

    @Test
    void ajoutAuDelaDeCinquanteEtUnMembresRefuse() {
        Conversation c = groupe();
        List<String> ids = IntStream.range(0, 49).mapToObj(i -> "p" + i).toList();
        ids.forEach(id -> lenient().when(annuaire.trouver(id)).thenReturn(Optional.of(new Collaborateur(id, id, null, Set.of(), null, true))));
        assertThatThrownBy(() -> service.ajouterMembres(EXP, c.getId(), ids)).extracting("code").isEqualTo("COL_TOO_MANY_RECIPIENTS");
    }

    @Test
    void retraitParLAdministrateurPrevientLeMembreRetire() {
        Conversation c = groupe();
        service.retirerMembre(EXP, c.getId(), "serge");
        assertThat(c.membre("serge")).isEmpty();
        Message m = messagesEnregistres().get(0);
        assertThat(m.getEvenement()).isEqualTo(EvenementGroupe.MEMBRE_RETIRE);
        assertThat(m.getEvenementCibles()).containsExactly("Serge Ndzana");
        assertThat(dernierEvenement().destinataires()).containsExactlyInAnyOrder("aminata", "serge");
        assertThat(dernierEvenement().evenement()).isEqualTo("MEMBRE_RETIRE");
    }

    @Test
    void unMembreQuitteLeGroupe() {
        Conversation c = groupe();
        service.retirerMembre(new Expediteur("serge", "Serge"), c.getId(), "serge");
        assertThat(c.membre("serge")).isEmpty();
        Message m = messagesEnregistres().get(0);
        assertThat(m.getEvenement()).isEqualTo(EvenementGroupe.MEMBRE_PARTI);
        assertThat(m.getAuteurNom()).isEqualTo("Serge Ndzana");
        assertThat(m.getEvenementCibles()).isEmpty();
        assertThat(dernierEvenement().destinataires()).containsExactlyInAnyOrder("moi", "aminata");
    }

    @Test
    void lAdministrateurQuiPartTransmetLaGestionAuPlusAncien() {
        Conversation c = groupe();
        service.retirerMembre(EXP, c.getId(), "moi");
        assertThat(c.membre("aminata")).get().extracting(ConversationMembre::isAdministrateur).isEqualTo(true);
        assertThat(c.membre("serge")).get().extracting(ConversationMembre::isAdministrateur).isEqualTo(false);
    }

    @Test
    void retraitDUnNonMembre() {
        Conversation c = groupe();
        assertThatThrownBy(() -> service.retirerMembre(EXP, c.getId(), "fatime")).extracting("code").isEqualTo("COL_MEMBER_NOT_FOUND");
    }

    @Test
    void conversationInconnue() {
        var id = java.util.UUID.randomUUID();
        when(conversations.pourMembre(id, "moi")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.retirerMembre(EXP, id, "moi")).extracting("code").isEqualTo("COL_CONVERSATION_NOT_FOUND");
        assertThatThrownBy(() -> service.ajouterMembres(EXP, id, List.of("serge"))).extracting("code").isEqualTo("COL_CONVERSATION_NOT_FOUND");
    }
}
