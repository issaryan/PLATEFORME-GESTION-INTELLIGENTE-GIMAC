package cm.gimac.des.collaboration.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import cm.gimac.des.collaboration.application.port.in.EnvoyerMessageUseCase.Expediteur;
import cm.gimac.des.collaboration.application.port.in.EnvoyerMessageUseCase.NouveauMessage;
import cm.gimac.des.collaboration.application.port.in.EnvoyerMessageUseCase.NouvelleConversation;
import cm.gimac.des.collaboration.application.port.out.ChiffrementPort;
import cm.gimac.des.collaboration.application.port.out.CollaboratorDirectoryPort;
import cm.gimac.des.collaboration.application.port.out.DocumentStoragePort;
import cm.gimac.des.collaboration.application.port.out.DocumentStoragePort.DocumentDepose;
import cm.gimac.des.collaboration.application.port.out.DocumentStoragePort.MetadonneesDocument;
import cm.gimac.des.collaboration.domain.event.MessageEnvoyePayload;
import cm.gimac.des.collaboration.domain.exception.CollaborationException;
import cm.gimac.des.collaboration.domain.messagerie.Collaborateur;
import cm.gimac.des.collaboration.domain.messagerie.Conversation;
import cm.gimac.des.collaboration.domain.messagerie.ConversationMembre;
import cm.gimac.des.collaboration.domain.messagerie.Etiquette;
import cm.gimac.des.collaboration.domain.messagerie.FichierJoint;
import cm.gimac.des.collaboration.domain.messagerie.Message;
import cm.gimac.des.collaboration.domain.messagerie.PieceJointe;
import cm.gimac.des.collaboration.domain.messagerie.TypeConversation;
import cm.gimac.des.collaboration.domain.service.MasquagePan;
import cm.gimac.des.collaboration.domain.service.ReglesMessagerie;
import cm.gimac.des.collaboration.infrastructure.messaging.outbox.OutboxPublisher;
import cm.gimac.des.collaboration.repository.ConversationMembreRepository;
import cm.gimac.des.collaboration.repository.ConversationRepository;
import cm.gimac.des.collaboration.repository.MessageRepository;
import cm.gimac.des.collaboration.repository.PieceJointeRepository;
import java.io.ByteArrayInputStream;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Envoi des messages (D-64) : destinataires, conversation directe, RG-52, pièces, événement. */
@ExtendWith(MockitoExtension.class)
class MessagerieServiceTest {

    static final Instant T = Instant.parse("2026-09-30T08:00:00.123456789Z");
    static final Instant T_BASE = Instant.parse("2026-09-30T08:00:00.123456Z");
    static final Collaborateur MOI = new Collaborateur("moi", "Paul Mbarga", "Chef du DES", Set.of("ROLE_CHEF_DES"), null, true);
    static final Collaborateur AMINATA = new Collaborateur("aminata", "Aminata Ngono", "Conductrice", Set.of(), null, true);
    static final Collaborateur SERGE = new Collaborateur("serge", "Serge Ndzana", "Ingénieur", Set.of(), null, true);
    static final Expediteur EXP = new Expediteur("moi", "Paul (jeton)");
    static final UUID LIAISON = UUID.randomUUID();

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

    MessagerieService service;

    @BeforeEach
    void init() {
        service = new MessagerieService(
            conversations,
            membres,
            messages,
            piecesJointes,
            annuaire,
            documents,
            chiffrement,
            outbox,
            new ReglesMessagerie(),
            new MasquagePan(),
            Clock.fixed(T, ZoneOffset.UTC)
        );
        lenient().when(annuaire.trouver("moi")).thenReturn(Optional.of(MOI));
        lenient().when(annuaire.trouver("aminata")).thenReturn(Optional.of(AMINATA));
        lenient().when(annuaire.trouver("serge")).thenReturn(Optional.of(SERGE));
        lenient().when(chiffrement.chiffrer(anyString(), any())).thenReturn(new byte[] { 1, 2, 3 });
    }

    static NouvelleConversation directe(String contenu) {
        return new NouvelleConversation(List.of("aminata"), null, null, null, contenu, List.of());
    }

    @Test
    @DisplayName("Première conversation directe : créée avec les deux membres, message chiffré, événement sans contenu")
    void directeCreee() {
        when(conversations.findByCleDirecte("aminata|moi")).thenReturn(Optional.empty());
        var r = service.creerConversation(EXP, directe("Bonjour Aminata"), List.of());

        assertThat(r.creee()).isTrue();
        ArgumentCaptor<Conversation> c = ArgumentCaptor.forClass(Conversation.class);
        verify(conversations).save(c.capture());
        assertThat(c.getValue().getType()).isEqualTo(TypeConversation.DIRECTE);
        assertThat(c.getValue().getCleDirecte()).isEqualTo("aminata|moi");
        assertThat(c.getValue().getMembres()).extracting(ConversationMembre::getNom).containsExactly("Paul Mbarga", "Aminata Ngono");
        assertThat(c.getValue().getDernierMessageId()).isEqualTo(r.messageId());
        assertThat(c.getValue().getMisAJourLe()).isEqualTo(T_BASE);
        assertThat(c.getValue().membre("moi")).get().extracting(ConversationMembre::getDernierLuMessageId).isEqualTo(r.messageId());
        assertThat(c.getValue().membre("aminata")).get().extracting(ConversationMembre::getDernierLuLe).isNull();

        ArgumentCaptor<Message> m = ArgumentCaptor.forClass(Message.class);
        verify(messages).save(m.capture());
        assertThat(m.getValue().getId()).isEqualTo(r.messageId());
        assertThat(m.getValue().getContenuChiffre()).containsExactly(1, 2, 3);
        assertThat(m.getValue().getEnvoyeLe()).isEqualTo(T_BASE);
        verify(chiffrement).chiffrer("Bonjour Aminata", r.messageId());

        ArgumentCaptor<Object> p = ArgumentCaptor.forClass(Object.class);
        verify(outbox).publish(eq("conversation"), eq(r.conversationId()), eq("collaboration.message-sent.v1"), p.capture());
        assertThat(p.getValue()).isEqualTo(new MessageEnvoyePayload(r.conversationId(), r.messageId(), "moi", List.of("aminata"), T_BASE));
    }

    @Test
    @DisplayName("Conversation directe existante réutilisée : aucun doublon, message ajouté")
    void directeReutilisee() {
        Conversation existante = new Conversation(TypeConversation.DIRECTE, null, null, null, "aminata|moi", "aminata", T.minusSeconds(60));
        existante.ajouterMembre(AMINATA, T.minusSeconds(60));
        existante.ajouterMembre(MOI, T.minusSeconds(60));
        when(conversations.findByCleDirecte("aminata|moi")).thenReturn(Optional.of(existante));

        var r = service.creerConversation(EXP, directe("Encore moi"), List.of());

        assertThat(r.creee()).isFalse();
        assertThat(r.conversationId()).isEqualTo(existante.getId());
        verify(conversations, never()).save(any());
        verify(messages).save(any());
        assertThat(existante.getMembres()).hasSize(2);
    }

    @Test
    @DisplayName("Groupe rattaché à une liaison : référence reprise d'une étiquette ou fournie ; RG-52 appliquée avant le chiffrement")
    void groupe() {
        var etiquette = new Etiquette(LIAISON, "LIA-2026-027");
        var demande = new NouvelleConversation(List.of("aminata", "serge"), " Recette EP A ", LIAISON, null, "Carte 4111 1111 1111 1111 en échec", List.of(etiquette));
        var r = service.creerConversation(EXP, demande, List.of());

        ArgumentCaptor<Conversation> c = ArgumentCaptor.forClass(Conversation.class);
        verify(conversations).save(c.capture());
        assertThat(c.getValue().getType()).isEqualTo(TypeConversation.GROUPE);
        assertThat(c.getValue().getTitre()).isEqualTo("Recette EP A");
        assertThat(c.getValue().getLiaisonReference()).isEqualTo("LIA-2026-027");
        assertThat(c.getValue().getCleDirecte()).isNull();
        assertThat(c.getValue().getMembres()).hasSize(3);
        verify(chiffrement).chiffrer("Carte 411111******1111 en échec", r.messageId());
        ArgumentCaptor<Message> m = ArgumentCaptor.forClass(Message.class);
        verify(messages).save(m.capture());
        assertThat(m.getValue().getEtiquettes()).extracting(e -> e.vue()).containsExactly(etiquette);
        verify(conversations, never()).findByCleDirecte(anyString());

        var fournie = new NouvelleConversation(List.of("aminata"), null, LIAISON, "LIA-2026-031", "Point", List.of());
        service.creerConversation(EXP, fournie, List.of());
        verify(conversations, org.mockito.Mockito.times(2)).save(c.capture());
        assertThat(c.getValue().getLiaisonReference()).isEqualTo("LIA-2026-031");
        assertThat(c.getValue().getType()).isEqualTo(TypeConversation.GROUPE);

        var sansReference = new NouvelleConversation(List.of("aminata"), null, LIAISON, " ", "Point", List.of());
        service.creerConversation(EXP, sansReference, List.of());
        verify(conversations, org.mockito.Mockito.times(3)).save(c.capture());
        assertThat(c.getValue().getLiaisonReference()).isNull();
    }

    @Test
    @DisplayName("Destinataire inconnu, désactivé ou externe : COL_RECIPIENT_UNKNOWN, rien n'est écrit")
    void destinataireInconnu() {
        when(annuaire.trouver("externe")).thenReturn(Optional.of(new Collaborateur("externe", "X", null, Set.of(), "inst", true)));
        when(annuaire.trouver("fantome")).thenReturn(Optional.empty());
        for (String id : List.of("externe", "fantome")) {
            var demande = new NouvelleConversation(List.of(id), null, null, null, "Bonjour", List.of());
            assertThatThrownBy(() -> service.creerConversation(EXP, demande, List.of())).isInstanceOfSatisfying(CollaborationException.class, e ->
                assertThat(e.code()).isEqualTo("COL_RECIPIENT_UNKNOWN")
            );
        }
        verifyNoInteractions(messages, outbox, documents);
    }

    @Test
    @DisplayName("Expéditeur absent de l'annuaire : nom du jeton, à défaut son identifiant")
    void expediteurHorsAnnuaire() {
        when(annuaire.trouver("moi")).thenReturn(Optional.empty());
        ArgumentCaptor<Conversation> c = ArgumentCaptor.forClass(Conversation.class);
        service.creerConversation(EXP, directe("Bonjour"), List.of());
        service.creerConversation(new Expediteur("moi", " "), directe("Bonjour"), List.of());
        service.creerConversation(new Expediteur("moi", null), directe("Bonjour"), List.of());
        verify(conversations, org.mockito.Mockito.times(3)).save(c.capture());
        assertThat(c.getAllValues().get(0).membre("moi")).get().extracting(ConversationMembre::getNom).isEqualTo("Paul (jeton)");
        assertThat(c.getAllValues().get(1).membre("moi")).get().extracting(ConversationMembre::getNom).isEqualTo("moi");
        assertThat(c.getAllValues().get(2).membre("moi")).get().extracting(ConversationMembre::getNom).isEqualTo("moi");
    }

    @Test
    @DisplayName("Pièces jointes déposées dans MS-DOCUMENT, référencées par le message")
    void pieces() {
        UUID doc = UUID.randomUUID();
        when(documents.deposer(any(), any(), eq("moi"))).thenReturn(new DocumentDepose(doc, "ab".repeat(32), "pv.pdf", 42, "application/pdf"));
        FichierJoint f = new FichierJoint("pv.pdf", "application/pdf", 42, () -> new ByteArrayInputStream(new byte[42]));

        var r = service.creerConversation(EXP, new NouvelleConversation(List.of("aminata"), null, null, null, "", null), List.of(f));

        verify(documents).deposer(any(), eq(r.messageId()), eq("moi"));
        ArgumentCaptor<Message> m = ArgumentCaptor.forClass(Message.class);
        verify(messages).save(m.capture());
        assertThat(m.getValue().getPiecesJointes()).singleElement().satisfies(p -> {
            assertThat(p.getDocumentRef()).isEqualTo(doc);
            assertThat(p.getEmpreinte()).isEqualTo("ab".repeat(32));
            assertThat(p.getTaille()).isEqualTo(42);
            assertThat(p.getTypeMime()).isEqualTo("application/pdf");
            assertThat(p.getNom()).isEqualTo("pv.pdf");
            assertThat(p.getMessage()).isSameAs(m.getValue());
        });
    }

    static final long QUATRE_GO = 4L * 1024 * 1024 * 1024;

    /** Groupe dont « moi », Aminata et Serge sont membres, connu du dépôt des conversations. */
    Conversation groupeConnu() {
        Conversation c = new Conversation(TypeConversation.GROUPE, "Atelier", null, null, null, "moi", T.minusSeconds(60));
        c.ajouterMembre(MOI, T.minusSeconds(60));
        c.ajouterMembre(AMINATA, T.minusSeconds(60));
        c.ajouterMembre(SERGE, T.minusSeconds(60));
        lenient().when(conversations.pourMembre(c.getId(), "moi")).thenReturn(Optional.of(c));
        return c;
    }

    static MetadonneesDocument ticket(UUID id, String nom, String type, String finalite, String deposant) {
        return new MetadonneesDocument(id, nom, QUATRE_GO, type, "CD".repeat(32), finalite, "ref", deposant);
    }

    static void erreurMetier(ThrowingCallable appel, String code, int statut) {
        assertThatThrownBy(appel).isInstanceOfSatisfying(CollaborationException.class, e -> {
            assertThat(e.code()).isEqualTo(code);
            assertThat(e.status()).isEqualTo(statut);
        });
    }

    @Test
    @DisplayName("D-74 : ticket d'un dépôt fragmenté joint sans nouvel envoi (nom, taille, type canonique, empreinte des métadonnées) ; événement inchangé")
    void documentTicket() {
        Conversation c = groupeConnu();
        UUID video = UUID.randomUUID();
        UUID son = UUID.randomUUID();
        when(documents.metadonnees(video)).thenReturn(Optional.of(ticket(video, "C:\\ateliers\\atelier.mp4", "video/mp4", "MESSAGERIE", "moi")));
        // application/octet-stream admis quand l'extension est connue : type canonique retenu.
        when(documents.metadonnees(son)).thenReturn(Optional.of(ticket(son, "compte-rendu.m4a", "application/octet-stream", "MESSAGERIE", "moi")));

        var r = service.envoyer(EXP, c.getId(), new NouveauMessage(null, List.of(), List.of(video, son, video), null), List.of());

        verify(documents, never()).deposer(any(), any(), any());
        ArgumentCaptor<Message> m = ArgumentCaptor.forClass(Message.class);
        verify(messages).save(m.capture());
        assertThat(m.getValue().getPiecesJointes()).hasSize(2);
        assertThat(m.getValue().getPiecesJointes().get(0)).satisfies(p -> {
            assertThat(p.getDocumentRef()).isEqualTo(video);
            assertThat(p.getNom()).isEqualTo("atelier.mp4");
            assertThat(p.getTaille()).isEqualTo(QUATRE_GO);
            assertThat(p.getTypeMime()).isEqualTo("video/mp4");
            assertThat(p.getEmpreinte()).isEqualTo("cd".repeat(32));
        });
        assertThat(m.getValue().getPiecesJointes().get(1).getTypeMime()).isEqualTo("audio/mp4");
        verify(chiffrement).chiffrer("", r.messageId());
        ArgumentCaptor<Object> p = ArgumentCaptor.forClass(Object.class);
        verify(outbox).publish(eq("conversation"), eq(c.getId()), eq("collaboration.message-sent.v1"), p.capture());
        assertThat(p.getValue()).isEqualTo(new MessageEnvoyePayload(c.getId(), r.messageId(), "moi", List.of("aminata", "serge"), T_BASE));
    }

    @Test
    @DisplayName("D-74 : ticket inconnu, d'un autre déposant, d'une autre finalité ou non vérifié : 400 COL_DOCUMENT_INVALID, rien n'est écrit")
    void documentInvalide() {
        Conversation c = groupeConnu();
        UUID inconnu = UUID.randomUUID();
        UUID autre = UUID.randomUUID();
        UUID dossier = UUID.randomUUID();
        UUID nonVerifie = UUID.randomUUID();
        when(documents.metadonnees(inconnu)).thenReturn(Optional.empty());
        when(documents.metadonnees(autre)).thenReturn(Optional.of(ticket(autre, "secret.pdf", "application/pdf", "MESSAGERIE", "aminata")));
        when(documents.metadonnees(dossier)).thenReturn(Optional.of(ticket(dossier, "dossier.pdf", "application/pdf", "ADHESION", "moi")));
        when(documents.metadonnees(nonVerifie)).thenReturn(
            Optional.of(new MetadonneesDocument(nonVerifie, "a.pdf", 3, "application/pdf", null, "MESSAGERIE", null, "moi"))
        );
        UUID id = c.getId();
        for (UUID t : List.of(inconnu, autre, dossier, nonVerifie)) {
            var demande = new NouveauMessage("Voir pièce", List.of(), List.of(t), List.of());
            assertThatThrownBy(() -> service.envoyer(EXP, id, demande, List.of())).isInstanceOfSatisfying(CollaborationException.class, e -> {
                assertThat(e.code()).isEqualTo("COL_DOCUMENT_INVALID");
                assertThat(e.status()).isEqualTo(400);
                // Rien du document d'un autre : seulement le ticket fourni.
                assertThat(e.details()).containsOnlyKeys("id").containsEntry("id", t.toString());
            });
        }
        // Création : aucune conversation enregistrée.
        var creation = new NouvelleConversation(List.of("aminata"), null, null, null, "", List.of(), false, List.of(autre), List.of());
        erreurMetier(() -> service.creerConversation(EXP, creation, List.of()), "COL_DOCUMENT_INVALID", 400);
        verify(conversations, never()).save(any());
        verify(documents, never()).deposer(any(), any(), any());
        verifyNoInteractions(messages, outbox);
    }

    @Test
    @DisplayName("D-74 : formats des tickets, liste étendue (vidéo, audio, archives) ; type contraire à l'extension ou extension inconnue : COL_ATTACHMENT_TYPE")
    void documentFormats() {
        Conversation c = groupeConnu();
        UUID id = c.getId();
        Map<String, String> admis = Map.of(
            "atelier.mov",
            "video/quicktime",
            "atelier.webm",
            "video/webm",
            "atelier.mkv",
            "application/octet-stream",
            "atelier.avi",
            "video/x-msvideo",
            "son.mp3",
            "audio/mpeg",
            "son.wav",
            "audio/x-wav",
            "archive.7z",
            "application/x-7z-compressed",
            "archive.tar",
            "application/x-tar",
            "archive.tar.gz",
            "application/gzip",
            "pv.pdf",
            "application/pdf"
        );
        admis.forEach((nom, type) -> {
            UUID t = UUID.randomUUID();
            when(documents.metadonnees(t)).thenReturn(Optional.of(ticket(t, nom, type, "MESSAGERIE", "moi")));
            service.envoyer(EXP, id, new NouveauMessage("", List.of(), List.of(t), List.of()), List.of());
        });
        verify(messages, times(admis.size())).save(any());
        for (String[] refuse : List.of(
            new String[] { "outil.exe", "application/octet-stream" },
            new String[] { "atelier.mp4", "application/pdf" },
            new String[] { "sans-extension", "video/mp4" }
        )) {
            UUID t = UUID.randomUUID();
            when(documents.metadonnees(t)).thenReturn(Optional.of(ticket(t, refuse[0], refuse[1], "MESSAGERIE", "moi")));
            var demande = new NouveauMessage("", List.of(), List.of(t), List.of());
            erreurMetier(() -> service.envoyer(EXP, id, demande, List.of()), "COL_ATTACHMENT_TYPE", 400);
        }
        // Les fichiers multipart gardent leur liste : une vidéo y est refusée.
        FichierJoint video = new FichierJoint("atelier.mp4", "video/mp4", 42, () -> new ByteArrayInputStream(new byte[42]));
        var demande = new NouveauMessage("", List.of());
        erreurMetier(() -> service.envoyer(EXP, id, demande, List.of(video)), "COL_ATTACHMENT_TYPE", 400);
    }

    @Test
    @DisplayName("D-74 : transfert d'une pièce d'une conversation dont l'expéditeur est membre : même document, sans copie ni dépôt")
    void transfert() {
        Conversation origine = groupeConnu();
        Conversation cible = new Conversation(TypeConversation.DIRECTE, null, null, null, "moi|serge", "moi", T.minusSeconds(60));
        cible.ajouterMembre(MOI, T.minusSeconds(60));
        cible.ajouterMembre(SERGE, T.minusSeconds(60));
        when(conversations.pourMembre(cible.getId(), "moi")).thenReturn(Optional.of(cible));
        UUID doc = UUID.randomUUID();
        Message ancien = new Message(UUID.randomUUID(), origine, AMINATA, new byte[] { 1 }, T.minusSeconds(30));
        PieceJointe source = ancien.joindre(doc, "atelier.mp4", QUATRE_GO, "video/mp4", "ef".repeat(32));
        when(piecesJointes.avecConversation(source.getId())).thenReturn(Optional.of(source));

        var r = service.envoyer(EXP, cible.getId(), new NouveauMessage("Pour toi", List.of(), List.of(), List.of(source.getId())), List.of());

        verify(documents, never()).deposer(any(), any(), any());
        verify(documents, never()).metadonnees(any());
        ArgumentCaptor<Message> m = ArgumentCaptor.forClass(Message.class);
        verify(messages).save(m.capture());
        assertThat(m.getValue().getPiecesJointes()).singleElement().satisfies(p -> {
            assertThat(p.getId()).isNotEqualTo(source.getId());
            assertThat(p.getDocumentRef()).isEqualTo(doc);
            assertThat(p.getNom()).isEqualTo("atelier.mp4");
            assertThat(p.getTaille()).isEqualTo(QUATRE_GO);
            assertThat(p.getTypeMime()).isEqualTo("video/mp4");
            assertThat(p.getEmpreinte()).isEqualTo("ef".repeat(32));
            assertThat(p.getMessage()).isSameAs(m.getValue());
        });
        ArgumentCaptor<Object> p = ArgumentCaptor.forClass(Object.class);
        verify(outbox).publish(eq("conversation"), eq(cible.getId()), eq("collaboration.message-sent.v1"), p.capture());
        assertThat(p.getValue()).isEqualTo(new MessageEnvoyePayload(cible.getId(), r.messageId(), "moi", List.of("serge"), T_BASE));
    }

    @Test
    @DisplayName("D-74 : transfert d'une pièce inconnue ou d'une conversation dont l'expéditeur n'est pas membre : 404 COL_ATTACHMENT_NOT_FOUND")
    void transfertHorsConversation() {
        Conversation cible = groupeConnu();
        Conversation etrangere = new Conversation(TypeConversation.DIRECTE, null, null, null, "aminata|serge", "aminata", T.minusSeconds(60));
        etrangere.ajouterMembre(AMINATA, T.minusSeconds(60));
        etrangere.ajouterMembre(SERGE, T.minusSeconds(60));
        when(conversations.pourMembre(etrangere.getId(), "moi")).thenReturn(Optional.empty());
        Message prive = new Message(UUID.randomUUID(), etrangere, AMINATA, new byte[] { 1 }, T);
        PieceJointe source = prive.joindre(UUID.randomUUID(), "secret.pdf", 3, "application/pdf", "ab".repeat(32));
        UUID inconnue = UUID.randomUUID();
        when(piecesJointes.avecConversation(source.getId())).thenReturn(Optional.of(source));
        when(piecesJointes.avecConversation(inconnue)).thenReturn(Optional.empty());
        UUID id = cible.getId();
        for (UUID piece : List.of(source.getId(), inconnue)) {
            var demande = new NouveauMessage("x", List.of(), List.of(), List.of(piece));
            erreurMetier(() -> service.envoyer(EXP, id, demande, List.of()), "COL_ATTACHMENT_NOT_FOUND", 404);
        }
        var creation = new NouvelleConversation(List.of("aminata"), null, null, null, "x", List.of(), false, List.of(), List.of(source.getId()));
        erreurMetier(() -> service.creerConversation(EXP, creation, List.of()), "COL_ATTACHMENT_NOT_FOUND", 404);
        verify(conversations, never()).save(any());
        verifyNoInteractions(messages, outbox);
    }

    @Test
    @DisplayName("D-74 : 5 pièces au plus toutes sources confondues (fichiers, tickets, transferts), contrôlé avant tout appel ; doublons comptés une fois")
    void limiteToutesSources() {
        Conversation c = groupeConnu();
        UUID id = c.getId();
        List<FichierJoint> trois = IntStream.range(0, 3)
            .mapToObj(i -> new FichierJoint("p" + i + ".pdf", "application/pdf", 1, () -> new ByteArrayInputStream(new byte[1])))
            .toList();
        UUID t1 = UUID.randomUUID();
        UUID t2 = UUID.randomUUID();
        UUID transfert = UUID.randomUUID();
        var six = new NouveauMessage("x", List.of(), List.of(t1, t2), List.of(transfert));
        assertThatThrownBy(() -> service.envoyer(EXP, id, six, trois)).isInstanceOfSatisfying(CollaborationException.class, e -> {
            assertThat(e.code()).isEqualTo("COL_TOO_MANY_ATTACHMENTS");
            assertThat(e.details()).containsEntry("nombre", 6).containsEntry("maximum", 5);
        });
        var creation = new NouvelleConversation(List.of("aminata"), "G", null, null, "x", List.of(), true, List.of(t1, t2), List.of(transfert));
        erreurMetier(() -> service.creerConversation(EXP, creation, trois), "COL_TOO_MANY_ATTACHMENTS", 400);
        verifyNoInteractions(documents, piecesJointes, messages, outbox);

        // Cinq exactement, un ticket répété : admis.
        when(documents.metadonnees(t1)).thenReturn(Optional.of(ticket(t1, "a.mp4", "video/mp4", "MESSAGERIE", "moi")));
        when(documents.metadonnees(t2)).thenReturn(Optional.of(ticket(t2, "b.mp4", "video/mp4", "MESSAGERIE", "moi")));
        when(documents.deposer(any(), any(), eq("moi"))).thenAnswer(i -> new DocumentDepose(UUID.randomUUID(), "ab".repeat(32), "p.pdf", 1, "application/pdf"));
        service.envoyer(EXP, id, new NouveauMessage("", List.of(), List.of(t1, t2, t1), List.of()), trois);
        ArgumentCaptor<Message> m = ArgumentCaptor.forClass(Message.class);
        verify(messages).save(m.capture());
        assertThat(m.getValue().getPiecesJointes()).hasSize(5);
    }

    @Test
    @DisplayName("D-74 : conversation créée avec un ticket seul (ni texte, ni fichier)")
    void creationAvecTicketSeul() {
        UUID t = UUID.randomUUID();
        when(conversations.findByCleDirecte("aminata|moi")).thenReturn(Optional.empty());
        when(documents.metadonnees(t)).thenReturn(Optional.of(ticket(t, "atelier.mp4", "video/mp4", "MESSAGERIE", "moi")));
        var demande = new NouvelleConversation(List.of("aminata"), null, null, null, null, List.of(), false, List.of(t), List.of());

        var r = service.creerConversation(EXP, demande, List.of());

        assertThat(r.creee()).isTrue();
        ArgumentCaptor<Message> m = ArgumentCaptor.forClass(Message.class);
        verify(messages).save(m.capture());
        assertThat(m.getValue().getPiecesJointes()).extracting(PieceJointe::getDocumentRef).containsExactly(t);
    }

    @Test
    @DisplayName("Envoi dans une conversation : membre seulement (404 sinon) ; auteur tiré de l'instantané du membre")
    void envoyer() {
        Conversation c = new Conversation(TypeConversation.GROUPE, "Groupe", null, null, null, "moi", T.minusSeconds(60));
        c.ajouterMembre(MOI, T.minusSeconds(60));
        c.ajouterMembre(AMINATA, T.minusSeconds(60));
        c.ajouterMembre(SERGE, T.minusSeconds(60));
        when(conversations.pourMembre(c.getId(), "moi")).thenReturn(Optional.of(c));
        when(conversations.pourMembre(c.getId(), "intrus")).thenReturn(Optional.empty());

        var r = service.envoyer(EXP, c.getId(), new NouveauMessage("Suite", List.of()), null);

        assertThat(r.creee()).isFalse();
        ArgumentCaptor<Object> p = ArgumentCaptor.forClass(Object.class);
        verify(outbox).publish(eq("conversation"), eq(c.getId()), eq("collaboration.message-sent.v1"), p.capture());
        assertThat(((MessageEnvoyePayload) p.getValue()).destinataires()).containsExactly("aminata", "serge");
        ArgumentCaptor<Message> m = ArgumentCaptor.forClass(Message.class);
        verify(messages).save(m.capture());
        assertThat(m.getValue().getAuteurNom()).isEqualTo("Paul Mbarga");
        assertThat(m.getValue().getAuteurFonction()).isEqualTo("Chef du DES");
        assertThat(m.getValue().getConversation()).isSameAs(c);

        UUID id = c.getId();
        var intrus = new Expediteur("intrus", null);
        var nouveau = new NouveauMessage("x", List.of());
        assertThatThrownBy(() -> service.envoyer(intrus, id, nouveau, List.of())).isInstanceOfSatisfying(CollaborationException.class, e -> {
            assertThat(e.code()).isEqualTo("COL_CONVERSATION_NOT_FOUND");
            assertThat(e.status()).isEqualTo(404);
        });
        var vide = new NouveauMessage(" ", List.of());
        assertThatThrownBy(() -> service.envoyer(EXP, id, vide, List.of())).isInstanceOfSatisfying(CollaborationException.class, e ->
            assertThat(e.code()).isEqualTo("COL_MESSAGE_EMPTY")
        );
    }

    @Test
    @DisplayName("Lu : marque posée sur le dernier message ; conversation vide sans effet ; non-membre 404")
    void marquerLu() {
        Conversation c = new Conversation(TypeConversation.DIRECTE, null, null, null, "aminata|moi", "aminata", T);
        ConversationMembre moi = c.ajouterMembre(MOI, T);
        Message dernier = new Message(UUID.randomUUID(), c, AMINATA, new byte[] { 1 }, T);
        when(membres.findByConversationIdAndMembreId(c.getId(), "moi")).thenReturn(Optional.of(moi));
        when(messages.findFirstByConversationIdOrderByEnvoyeLeDescIdDesc(c.getId())).thenReturn(Optional.of(dernier), Optional.empty());

        service.marquerLu("moi", c.getId());
        assertThat(moi.getDernierLuMessageId()).isEqualTo(dernier.getId());
        assertThat(moi.getDernierLuLe()).isEqualTo(T);
        service.marquerLu("moi", c.getId());
        assertThat(moi.getDernierLuMessageId()).isEqualTo(dernier.getId());

        // La marque ne recule pas.
        moi.marquerLu(UUID.randomUUID(), T.minusSeconds(5));
        assertThat(moi.getDernierLuMessageId()).isEqualTo(dernier.getId());

        UUID id = c.getId();
        assertThatThrownBy(() -> service.marquerLu("intrus", id)).isInstanceOfSatisfying(CollaborationException.class, e ->
            assertThat(e.code()).isEqualTo("COL_CONVERSATION_NOT_FOUND")
        );
    }
}
