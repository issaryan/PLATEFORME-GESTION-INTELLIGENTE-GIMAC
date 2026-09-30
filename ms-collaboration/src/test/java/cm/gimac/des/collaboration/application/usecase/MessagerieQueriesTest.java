package cm.gimac.des.collaboration.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cm.gimac.des.collaboration.application.port.out.ChiffrementPort;
import cm.gimac.des.collaboration.application.port.out.CollaboratorDirectoryPort;
import cm.gimac.des.collaboration.application.port.out.CollaboratorDirectoryPort.PageAnnuaire;
import cm.gimac.des.collaboration.application.port.out.DocumentStoragePort;
import cm.gimac.des.collaboration.application.port.out.DocumentStoragePort.ContenuDocument;
import cm.gimac.des.collaboration.domain.exception.CollaborationException;
import cm.gimac.des.collaboration.domain.messagerie.Collaborateur;
import cm.gimac.des.collaboration.domain.messagerie.Conversation;
import cm.gimac.des.collaboration.domain.messagerie.Etiquette;
import cm.gimac.des.collaboration.domain.messagerie.Message;
import cm.gimac.des.collaboration.domain.messagerie.PieceJointe;
import cm.gimac.des.collaboration.domain.messagerie.TypeConversation;
import cm.gimac.des.collaboration.domain.service.ReglesMessagerie;
import cm.gimac.des.collaboration.repository.ConversationRepository;
import cm.gimac.des.collaboration.repository.MessageRepository;
import cm.gimac.des.collaboration.repository.PieceJointeRepository;
import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

/** Lectures de la messagerie (D-64) : annuaire, conversations, fil, non-lus, pièces ; 404 hors appartenance. */
@ExtendWith(MockitoExtension.class)
class MessagerieQueriesTest {

    static final Instant T = Instant.parse("2026-09-30T08:00:00Z");
    static final Collaborateur MOI = new Collaborateur("moi", "Paul Mbarga", "Chef du DES", Set.of(), null, true);
    static final Collaborateur AMINATA = new Collaborateur("aminata", "Aminata Ngono", "Conductrice", Set.of(), null, true);

    @Mock
    ConversationRepository conversations;

    @Mock
    MessageRepository messages;

    @Mock
    PieceJointeRepository pieces;

    @Mock
    CollaboratorDirectoryPort annuaire;

    @Mock
    DocumentStoragePort documents;

    @Mock
    ChiffrementPort chiffrement;

    MessagerieQueries queries;
    Conversation conversation;

    @BeforeEach
    void init() {
        queries = new MessagerieQueries(conversations, messages, pieces, annuaire, documents, chiffrement, new ReglesMessagerie());
        conversation = new Conversation(TypeConversation.GROUPE, "Recette", UUID.randomUUID(), "LIA-2026-027", null, "moi", T);
        conversation.ajouterMembre(MOI, T);
        conversation.ajouterMembre(AMINATA, T);
        lenient().when(conversations.pourMembre(conversation.getId(), "moi")).thenReturn(Optional.of(conversation));
        lenient().when(chiffrement.dechiffrer(any(), any())).thenAnswer(i -> "clair-" + new String((byte[]) i.getArgument(0)));
    }

    Message message(int n) {
        return new Message(UUID.randomUUID(), conversation, AMINATA, String.valueOf(n).getBytes(), T.plusSeconds(n));
    }

    static void introuvable(ThrowingCallable appel, String code) {
        assertThatThrownBy(appel).isInstanceOfSatisfying(CollaborationException.class, e -> {
            assertThat(e.code()).isEqualTo(code);
            assertThat(e.status()).isEqualTo(404);
        });
    }

    @Test
    @DisplayName("Annuaire : l'appelant et les comptes non joignables sont exclus ; pagination bornée")
    void annuaire() {
        var externe = new Collaborateur("ext", "Externe", null, Set.of(), "inst", true);
        when(annuaire.rechercher("ngo", 0, 100)).thenReturn(new PageAnnuaire(List.of(MOI, AMINATA, externe), 3, 0, 100));
        when(annuaire.rechercher("", 2, 1)).thenReturn(new PageAnnuaire(List.of(AMINATA), 5, 2, 1));
        var p = queries.annuaire("moi", " ngo ", -1, 500);
        assertThat(p.contenu()).containsExactly(AMINATA);
        assertThat(p.total()).isEqualTo(2);
        assertThat(p.page()).isZero();
        assertThat(p.taille()).isEqualTo(100);
        var q = queries.annuaire("moi", null, 2, 0);
        assertThat(q.total()).isEqualTo(5);
        assertThat(q.taille()).isEqualTo(1);
    }

    @Test
    @DisplayName("Conversations : page triée par activité, non-lus et dernier message déchiffré en extrait")
    @SuppressWarnings("unchecked")
    void conversations() {
        Message dernier = message(1);
        dernier.joindre(UUID.randomUUID(), "pv.pdf", 3, "application/pdf", "e".repeat(64));
        conversation.messageAjoute(dernier.getId(), dernier.getEnvoyeLe());
        Conversation vide = new Conversation(TypeConversation.DIRECTE, null, null, null, "a|moi", "moi", T);
        when(conversations.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(conversation, vide), PageRequest.of(0, 20), 2));
        when(messages.findById(dernier.getId())).thenReturn(Optional.of(dernier));
        List<Object[]> lignes = new ArrayList<>();
        lignes.add(new Object[] { conversation.getId(), 3L });
        when(messages.compterNonLusPar(eq("moi"), any())).thenReturn(lignes);

        var p = queries.conversations("moi", "rec", null, 0, 20);

        assertThat(p.total()).isEqualTo(2);
        var premiere = p.contenu().get(0);
        assertThat(premiere.nonLus()).isEqualTo(3);
        assertThat(premiere.membres()).hasSize(2);
        assertThat(premiere.dernierMessage().extrait()).isEqualTo("clair-1");
        assertThat(premiere.dernierMessage().piecesJointes()).isEqualTo(1);
        assertThat(premiere.dernierMessage().auteurNom()).isEqualTo("Aminata Ngono");
        assertThat(p.contenu().get(1).nonLus()).isZero();
        assertThat(p.contenu().get(1).dernierMessage()).isNull();
        ArgumentCaptor<Pageable> page = ArgumentCaptor.forClass(Pageable.class);
        verify(conversations).findAll(any(Specification.class), page.capture());
        assertThat(page.getValue().getSort().getOrderFor("misAJourLe")).isNotNull();
    }

    @Test
    @DisplayName("Conversations : page vide sans calcul des non-lus")
    @SuppressWarnings("unchecked")
    void conversationsVides() {
        when(conversations.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));
        assertThat(queries.conversations("moi", null, UUID.randomUUID(), 0, 20).contenu()).isEmpty();
        org.mockito.Mockito.verify(messages, org.mockito.Mockito.never()).compterNonLusPar(any(), any());
    }

    @Test
    @DisplayName("Une conversation : membre seulement ; dernier message disparu toléré")
    void conversation() {
        conversation.messageAjoute(UUID.randomUUID(), T);
        when(messages.findById(any())).thenReturn(Optional.empty());
        when(messages.compterNonLusPar(eq("moi"), any())).thenReturn(List.of());
        var l = queries.conversation("moi", conversation.getId());
        assertThat(l.conversation()).isSameAs(conversation);
        assertThat(l.dernierMessage()).isNull();
        UUID id = conversation.getId();
        introuvable(() -> queries.conversation("intrus", id), "COL_CONVERSATION_NOT_FOUND");
    }

    @Test
    @DisplayName("Fil : du plus ancien au plus récent, indicateur plusAnciens, curseur « avant »")
    void fil() {
        Message m1 = message(1), m2 = message(2), m3 = message(3);
        m3.etiqueter(new Etiquette(UUID.randomUUID(), "LIA-2026-027"));
        UUID id = conversation.getId();
        when(messages.plusRecents(eq(id), any())).thenReturn(List.of(m3, m2, m1));
        var p = queries.messages("moi", id, null, 2);
        assertThat(p.plusAnciens()).isTrue();
        assertThat(p.contenu()).extracting(l -> l.message().getId()).containsExactly(m2.getId(), m3.getId());
        assertThat(p.contenu().get(1).contenu()).isEqualTo("clair-3");
        assertThat(p.contenu().get(1).etiquettes()).extracting(Etiquette::reference).containsExactly("LIA-2026-027");

        when(messages.findById(m2.getId())).thenReturn(Optional.of(m2));
        when(messages.anterieurs(eq(id), eq(m2.getEnvoyeLe()), eq(m2.getId()), any())).thenReturn(List.of(m1));
        var q = queries.messages("moi", id, m2.getId(), 50);
        assertThat(q.plusAnciens()).isFalse();
        assertThat(q.contenu()).extracting(l -> l.message().getId()).containsExactly(m1.getId());

        Conversation autre = new Conversation(TypeConversation.DIRECTE, null, null, null, "x|y", "x", T);
        Message ailleurs = new Message(UUID.randomUUID(), autre, AMINATA, new byte[] { 1 }, T);
        when(messages.findById(ailleurs.getId())).thenReturn(Optional.of(ailleurs));
        UUID a = ailleurs.getId();
        introuvable(() -> queries.messages("moi", id, a, 50), "COL_MESSAGE_NOT_FOUND");
        UUID inconnu = UUID.randomUUID();
        introuvable(() -> queries.messages("moi", id, inconnu, 50), "COL_MESSAGE_NOT_FOUND");
        introuvable(() -> queries.messages("intrus", id, null, 50), "COL_CONVERSATION_NOT_FOUND");
    }

    @Test
    @DisplayName("Un message : lisible par les membres seulement")
    void unMessage() {
        Message m = message(7);
        when(messages.findById(m.getId())).thenReturn(Optional.of(m));
        assertThat(queries.message("moi", m.getId()).contenu()).isEqualTo("clair-7");
        UUID id = m.getId();
        introuvable(() -> queries.message("intrus", id), "COL_MESSAGE_NOT_FOUND");
        UUID inconnu = UUID.randomUUID();
        introuvable(() -> queries.message("moi", inconnu), "COL_MESSAGE_NOT_FOUND");
    }

    @Test
    @DisplayName("Non-lus : total de l'appelant")
    void nonLus() {
        when(messages.compterNonLus("moi")).thenReturn(4L);
        assertThat(queries.nonLus("moi")).isEqualTo(4);
    }

    @Test
    @DisplayName("Pièce jointe : contenu lu dans MS-DOCUMENT pour un membre ; 404 sinon")
    void piece() {
        Message m = message(1);
        UUID doc = UUID.randomUUID();
        PieceJointe p = m.joindre(doc, "pv.pdf", 3, "application/pdf", "e".repeat(64));
        when(pieces.avecConversation(p.getId())).thenReturn(Optional.of(p));
        ContenuDocument contenu = new ContenuDocument(new ByteArrayInputStream(new byte[] { 1, 2, 3 }), 3);
        when(documents.lire(doc)).thenReturn(contenu);

        var t = queries.piece("moi", p.getId());
        assertThat(t.piece()).isSameAs(p);
        assertThat(t.contenu()).isSameAs(contenu);

        UUID id = p.getId();
        introuvable(() -> queries.piece("intrus", id), "COL_ATTACHMENT_NOT_FOUND");
        UUID inconnue = UUID.randomUUID();
        introuvable(() -> queries.piece("moi", inconnue), "COL_ATTACHMENT_NOT_FOUND");
        org.mockito.Mockito.verify(documents, org.mockito.Mockito.times(1)).lire(any());
    }
}
