package cm.gimac.des.collaboration.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import cm.gimac.des.collaboration.application.port.in.ConsulterMessagerieUseCase.CitationLue;
import cm.gimac.des.collaboration.application.port.in.ConsulterMessagerieUseCase.LectureMembre;
import cm.gimac.des.collaboration.application.port.in.ConsulterMessagerieUseCase.MentionLue;
import cm.gimac.des.collaboration.application.port.out.ChiffrementPort;
import cm.gimac.des.collaboration.application.port.out.CollaboratorDirectoryPort;
import cm.gimac.des.collaboration.application.port.out.CollaboratorDirectoryPort.PageAnnuaire;
import cm.gimac.des.collaboration.application.port.out.DocumentStoragePort;
import cm.gimac.des.collaboration.application.port.out.DocumentStoragePort.ContenuDocument;
import cm.gimac.des.collaboration.domain.exception.CollaborationException;
import cm.gimac.des.collaboration.domain.messagerie.Collaborateur;
import cm.gimac.des.collaboration.domain.messagerie.Conversation;
import cm.gimac.des.collaboration.domain.messagerie.Etiquette;
import cm.gimac.des.collaboration.domain.messagerie.Lecture;
import cm.gimac.des.collaboration.domain.messagerie.Message;
import cm.gimac.des.collaboration.domain.messagerie.PieceJointe;
import cm.gimac.des.collaboration.domain.messagerie.TypeConversation;
import cm.gimac.des.collaboration.domain.service.ReglesMessagerie;
import cm.gimac.des.collaboration.repository.ConversationRepository;
import cm.gimac.des.collaboration.repository.LectureConversationRepository;
import cm.gimac.des.collaboration.repository.MessageCite;
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
    LectureConversationRepository lectures;

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
        queries = new MessagerieQueries(conversations, messages, lectures, pieces, annuaire, documents, chiffrement, new ReglesMessagerie());
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

    @Test
    @DisplayName("mon-compte.md §1 : versions des photos relayées ; annuaire en panne : vide, la lecture n'échoue pas")
    void versionsAvatar() {
        when(annuaire.versionsAvatar(java.util.Set.of("a"))).thenReturn(java.util.Map.of("a", "v1"));
        assertThat(queries.versionsAvatar(java.util.Set.of("a"))).containsEntry("a", "v1");
        when(annuaire.versionsAvatar(java.util.Set.of("b"))).thenThrow(cm.gimac.des.collaboration.domain.exception.CollaborationException.indisponible("COL_DIRECTORY_UNAVAILABLE", "panne"));
        assertThat(queries.versionsAvatar(java.util.Set.of("b"))).isEmpty();
        assertThat(queries.versionsAvatar(java.util.Set.of())).isEmpty();
        assertThat(queries.versionsAvatar(null)).isEmpty();
    }

    @Test
    @DisplayName("D-82 : messages cités d'une page lus en une requête ; extrait, pièces, auteur ; purgé ou illisible : « supprime » ; mentions et mentionneMoi")
    void citationsEtMentions() {
        UUID id = conversation.getId();
        Message cite = message(1);
        Message reponse1 = new Message(UUID.randomUUID(), conversation, MOI, "2".getBytes(), T.plusSeconds(2));
        reponse1.repondreA(cite);
        reponse1.mentionner(conversation.membre("aminata").orElseThrow());
        Message purge = message(9);
        Message reponse2 = new Message(UUID.randomUUID(), conversation, AMINATA, "3".getBytes(), T.plusSeconds(3));
        reponse2.repondreA(purge);
        Message illisible = message(8);
        Message reponse3 = new Message(UUID.randomUUID(), conversation, AMINATA, "4".getBytes(), T.plusSeconds(4));
        reponse3.repondreA(illisible);
        when(messages.plusRecents(eq(id), any())).thenReturn(List.of(reponse3, reponse2, reponse1, cite));
        when(messages.cites(any())).thenReturn(
            List.of(
                new MessageCite(cite.getId(), id, "aminata", "Aminata Ngono", "x".repeat(150).getBytes(), cite.getEnvoyeLe(), null, 2),
                new MessageCite(illisible.getId(), id, "aminata", "Aminata Ngono", "KO".getBytes(), illisible.getEnvoyeLe(), null, 0)
            )
        );
        org.mockito.Mockito.doAnswer(i -> {
            String brut = new String((byte[]) i.getArgument(0));
            if ("KO".equals(brut)) {
                throw new IllegalStateException("étiquette GCM invalide");
            }
            return "clair-" + brut;
        })
            .when(chiffrement)
            .dechiffrer(any(), any());

        var p = queries.messages("moi", id, null, 50);

        ArgumentCaptor<java.util.Collection<UUID>> ids = ArgumentCaptor.forClass(java.util.Collection.class);
        verify(messages, org.mockito.Mockito.times(1)).cites(ids.capture());
        assertThat(ids.getValue()).containsExactlyInAnyOrder(cite.getId(), purge.getId(), illisible.getId());
        var lu1 = p.contenu().get(1);
        assertThat(lu1.reponseA()).satisfies(c -> {
            assertThat(c.id()).isEqualTo(cite.getId());
            assertThat(c.auteurId()).isEqualTo("aminata");
            assertThat(c.auteurNom()).isEqualTo("Aminata Ngono");
            assertThat(c.extrait()).hasSize(120).startsWith("clair-x").endsWith("…");
            assertThat(c.piecesJointes()).isEqualTo(2);
            assertThat(c.supprime()).isFalse();
        });
        assertThat(lu1.mentions()).containsExactly(new MentionLue("aminata", "Aminata Ngono"));
        // Aminata est mentionnée et citée ; l'auteur du message n'est jamais « concerné » par le sien.
        assertThat(lu1.concerne("aminata")).isTrue();
        assertThat(lu1.concerne("moi")).isFalse();
        assertThat(p.contenu().get(2).reponseA()).isEqualTo(CitationLue.supprimee(purge.getId()));
        assertThat(p.contenu().get(3).reponseA().supprime()).isTrue();
        assertThat(p.contenu().get(3).reponseA().extrait()).isEmpty();
        assertThat(p.contenu().get(0).reponseA()).isNull();
        assertThat(p.contenu().get(0).mentions()).isEmpty();
        assertThat(p.contenu().get(0).concerne("moi")).isFalse();
    }

    // D-88 : accusés de lecture.

    static final Collaborateur SERGE = new Collaborateur("serge", "Serge Ndzana", "Ingénieur", Set.of(), null, true);

    Message demoi(int n) {
        return new Message(UUID.randomUUID(), conversation, MOI, ("moi" + n).getBytes(), T.plusSeconds(n));
    }

    @Test
    @DisplayName("D-88 : état de lecture sur mes messages du fil seulement, sans requête par message")
    void lectureDansLeFil() {
        UUID id = conversation.getId();
        conversation.ajouterMembre(SERGE, T);
        Message m1 = demoi(1);
        Message m2 = demoi(2);
        Message deux = message(3);
        conversation.membre("aminata").orElseThrow().marquerLu(m2.getId(), m2.getEnvoyeLe());
        conversation.membre("serge").orElseThrow().marquerLu(m1.getId(), m1.getEnvoyeLe());
        when(messages.plusRecents(eq(id), any())).thenReturn(List.of(deux, m2, m1));

        var p = queries.messages("moi", id, null, 10);

        assertThat(p.contenu().get(0).lecture()).isEqualTo(new Lecture(Lecture.EtatLecture.LU, 2, 2));
        assertThat(p.contenu().get(1).lecture()).isEqualTo(new Lecture(Lecture.EtatLecture.LU_PAR_CERTAINS, 1, 2));
        assertThat(p.contenu().get(2).lecture()).isNull();
        verifyNoInteractions(lectures);
    }

    @Test
    @DisplayName("D-88 : dernierMessage.lecture pour mon dernier message, absente pour celui d'un autre")
    void lectureDuDernierMessage() {
        Message mien = demoi(1);
        conversation.messageAjoute(mien.getId(), mien.getEnvoyeLe());
        when(messages.findById(mien.getId())).thenReturn(Optional.of(mien));
        assertThat(queries.conversation("moi", conversation.getId()).dernierMessage().lecture()).isEqualTo(new Lecture(Lecture.EtatLecture.ENVOYE, 0, 1));
        Message autre = message(2);
        conversation.messageAjoute(autre.getId(), autre.getEnvoyeLe());
        when(messages.findById(autre.getId())).thenReturn(Optional.of(autre));
        assertThat(queries.conversation("moi", conversation.getId()).dernierMessage().lecture()).isNull();
    }

    @Test
    @DisplayName("D-88 : « Lu par » : lecteurs d'abord avec l'heure (inconnue avant D-88), puis les autres ; 404 indistinct")
    void lectures() {
        UUID id = conversation.getId();
        conversation.ajouterMembre(SERGE, T);
        Collaborateur fatime = new Collaborateur("fatime", "Fatime Oumarou", null, Set.of(), null, true);
        conversation.ajouterMembre(fatime, T);
        Collaborateur tard = new Collaborateur("tard", "Arrivé Tard", null, Set.of(), null, true);
        Message mien = demoi(5);
        conversation.ajouterMembre(tard, T.plusSeconds(10));
        conversation.membre("aminata").orElseThrow().marquerLu(UUID.randomUUID(), T.plusSeconds(8));
        conversation.membre("serge").orElseThrow().marquerLu(UUID.randomUUID(), T.plusSeconds(5));
        conversation.membre("tard").orElseThrow().marquerLu(UUID.randomUUID(), T.plusSeconds(20));
        when(messages.findById(mien.getId())).thenReturn(Optional.of(mien));
        when(lectures.premieresLectures(id, mien.getEnvoyeLe())).thenReturn(
            List.of(new LectureConversationRepository.PremiereLecture("serge", T.plusSeconds(100)), new LectureConversationRepository.PremiereLecture("tard", T.plusSeconds(30)))
        );

        var l = queries.lectures("moi", id, mien.getId());

        assertThat(l).containsExactly(
            new LectureMembre("serge", "Serge Ndzana", true, T.plusSeconds(100)),
            new LectureMembre("aminata", "Aminata Ngono", true, null),
            new LectureMembre("fatime", "Fatime Oumarou", false, null)
        );

        Message autre = message(6);
        when(messages.findById(autre.getId())).thenReturn(Optional.of(autre));
        introuvable(() -> queries.lectures("moi", id, autre.getId()), "COL_MESSAGE_NOT_FOUND");
        UUID inconnu = UUID.randomUUID();
        introuvable(() -> queries.lectures("moi", id, inconnu), "COL_MESSAGE_NOT_FOUND");
        introuvable(() -> queries.lectures("intrus", id, mien.getId()), "COL_MESSAGE_NOT_FOUND");
        Conversation ailleurs = new Conversation(TypeConversation.DIRECTE, null, null, null, "x|moi", "moi", T);
        when(conversations.pourMembre(ailleurs.getId(), "moi")).thenReturn(Optional.of(ailleurs));
        introuvable(() -> queries.lectures("moi", ailleurs.getId(), mien.getId()), "COL_MESSAGE_NOT_FOUND");
    }
}

