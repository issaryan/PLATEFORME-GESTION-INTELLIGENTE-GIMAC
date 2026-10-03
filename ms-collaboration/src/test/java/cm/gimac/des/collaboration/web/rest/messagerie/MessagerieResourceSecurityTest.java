package cm.gimac.des.collaboration.web.rest.messagerie;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cm.gimac.des.collaboration.application.port.in.ConsulterMessagerieUseCase;
import cm.gimac.des.collaboration.application.port.in.ConsulterMessagerieUseCase.ConversationLue;
import cm.gimac.des.collaboration.application.port.in.ConsulterMessagerieUseCase.DernierMessage;
import cm.gimac.des.collaboration.application.port.in.ConsulterMessagerieUseCase.MessageLu;
import cm.gimac.des.collaboration.application.port.in.ConsulterMessagerieUseCase.Page;
import cm.gimac.des.collaboration.application.port.in.ConsulterMessagerieUseCase.PageMessages;
import cm.gimac.des.collaboration.application.port.in.ConsulterMessagerieUseCase.PieceATelecharger;
import cm.gimac.des.collaboration.application.port.in.EnvoyerMessageUseCase;
import cm.gimac.des.collaboration.application.port.in.EnvoyerMessageUseCase.Envoi;
import cm.gimac.des.collaboration.application.port.in.EnvoyerMessageUseCase.Expediteur;
import cm.gimac.des.collaboration.application.port.in.GererGroupeUseCase;
import cm.gimac.des.collaboration.application.port.in.SignalerSaisieUseCase;
import cm.gimac.des.collaboration.application.port.out.DocumentStoragePort.ContenuDocument;
import cm.gimac.des.collaboration.config.SecurityConfiguration;
import cm.gimac.des.collaboration.config.TestSecurityConfiguration;
import cm.gimac.des.collaboration.domain.exception.CollaborationException;
import cm.gimac.des.collaboration.domain.messagerie.Collaborateur;
import cm.gimac.des.collaboration.domain.messagerie.Conversation;
import cm.gimac.des.collaboration.domain.messagerie.Etiquette;
import cm.gimac.des.collaboration.domain.messagerie.EvenementGroupe;
import cm.gimac.des.collaboration.domain.messagerie.FichierJoint;
import cm.gimac.des.collaboration.domain.messagerie.Message;
import cm.gimac.des.collaboration.domain.messagerie.PieceJointe;
import cm.gimac.des.collaboration.domain.messagerie.TypeConversation;
import cm.gimac.des.collaboration.web.rest.errors.CollaborationExceptionHandler;
import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.AbstractMockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import tech.jhipster.config.JHipsterProperties;

/**
 * Sécurité de chaque route de la messagerie (D-61, D-64) avec la vraie chaîne de filtres : 401 sans
 * jeton, 403 sans second facteur, 403 sans la permission, accès avec elle ; formes des réponses.
 */
@WebMvcTest(controllers = MessagerieResource.class)
@Import({ SecurityConfiguration.class, TestSecurityConfiguration.class, CollaborationExceptionHandler.class })
@EnableConfigurationProperties(JHipsterProperties.class)
class MessagerieResourceSecurityTest {

    static final String MOI = "00000000-0000-4000-8000-00000000c001";
    static final Instant T = Instant.parse("2026-09-30T08:00:00Z");
    static final Collaborateur PAUL = new Collaborateur(MOI, "Paul Mbarga Atangana", "Chef du DES", Set.of("ROLE_CHEF_DES"), null, true);
    static final Collaborateur AMINATA = new Collaborateur("aminata", "Aminata Ngono", "Conductrice", Set.of(), null, true);
    static final Conversation CONVERSATION = new Conversation(TypeConversation.GROUPE, "Recette", UUID.randomUUID(), "LIA-2026-027", null, MOI, T);
    static final Message MESSAGE = new Message(UUID.randomUUID(), CONVERSATION, PAUL, new byte[] { 1 }, T);
    static final PieceJointe PIECE = MESSAGE.joindre(UUID.randomUUID(), "pv de recette.pdf", 3, "application/pdf", "e".repeat(64));
    static final String CREATION = "{\"destinataires\":[\"aminata\"],\"titre\":null,\"liaisonId\":null,\"contenu\":\"Bonjour\",\"etiquettes\":[]}";
    static final String ENVOI = "{\"contenu\":\"Suite\",\"etiquettes\":[{\"liaisonId\":\"" + UUID.randomUUID() + "\",\"reference\":\"LIA-2026-027\"}]}";
    static final List<String> TOUTES = List.of("COL_MESSAGE_READ", "COL_MESSAGE_SEND", "HOM_LIAISON_READ", "ROLE_CHEF_DES");

    static {
        CONVERSATION.ajouterMembre(PAUL, T);
        CONVERSATION.ajouterMembre(AMINATA, T);
    }

    @Autowired
    MockMvc mvc;

    @MockitoBean
    EnvoyerMessageUseCase envoi;

    @MockitoBean
    ConsulterMessagerieUseCase lecture;

    @MockitoBean
    GererGroupeUseCase groupes;

    @MockitoBean
    SignalerSaisieUseCase saisie;

    @BeforeEach
    void init() {
        UUID cid = CONVERSATION.getId();
        var lue = new ConversationLue(CONVERSATION, CONVERSATION.getMembres(), new DernierMessage(MESSAGE.getId(), "Paul Mbarga Atangana", "Bonjour", T, 1, null, java.util.List.of()), 2);
        var lu = new MessageLu(MESSAGE, "Bonjour", List.of(new Etiquette(CONVERSATION.getLiaisonId(), "LIA-2026-027")), List.of(PIECE));
        when(lecture.annuaire(anyString(), any(), anyInt(), anyInt())).thenReturn(new Page<>(List.of(AMINATA), 1, 0, 20));
        when(lecture.conversations(anyString(), any(), any(), anyInt(), anyInt())).thenReturn(new Page<>(List.of(lue), 1, 0, 20));
        when(lecture.conversation(anyString(), eq(cid))).thenReturn(lue);
        when(lecture.messages(anyString(), eq(cid), any(), anyInt())).thenReturn(new PageMessages(List.of(lu), true));
        when(lecture.message(anyString(), eq(MESSAGE.getId()))).thenReturn(lu);
        when(lecture.nonLus(anyString())).thenReturn(3L);
        when(lecture.piece(anyString(), eq(PIECE.getId()))).thenAnswer(i ->
            new PieceATelecharger(PIECE, new ContenuDocument(new ByteArrayInputStream(new byte[] { 'a', 'b', 'c' }), 3))
        );
        when(envoi.creerConversation(any(), any(), any())).thenReturn(new Envoi(cid, MESSAGE.getId(), true));
        when(envoi.envoyer(any(), eq(cid), any(), any())).thenReturn(new Envoi(cid, MESSAGE.getId(), false));
    }

    static Supplier<AbstractMockHttpServletRequestBuilder<?>> requete(HttpMethod m, String uri, String message) {
        if (message == null) {
            return () -> request(m, uri);
        }
        // Corps JSON (gestion des groupes, D-68) : « json: » suivi du document.
        if (message.startsWith("json:")) {
            return () -> request(m, uri).contentType(MediaType.APPLICATION_JSON).content(message.substring(5));
        }
        return () -> multipart(uri).file(new MockMultipartFile("message", "", MediaType.APPLICATION_JSON_VALUE, message.getBytes()));
    }

    static Stream<Arguments> routes() {
        String c = "/api/v1/messagerie/conversations/" + CONVERSATION.getId();
        return Stream.of(
            Arguments.of(HttpMethod.GET, "/api/v1/messagerie/annuaire?recherche=ngo", null, "COL_MESSAGE_READ", 200),
            Arguments.of(HttpMethod.GET, "/api/v1/messagerie/conversations", null, "COL_MESSAGE_READ", 200),
            Arguments.of(HttpMethod.POST, "/api/v1/messagerie/conversations", CREATION, "COL_MESSAGE_SEND", 201),
            Arguments.of(HttpMethod.GET, c, null, "COL_MESSAGE_READ", 200),
            Arguments.of(HttpMethod.GET, c + "/messages?taille=20", null, "COL_MESSAGE_READ", 200),
            Arguments.of(HttpMethod.POST, c + "/messages", ENVOI, "COL_MESSAGE_SEND", 201),
            Arguments.of(HttpMethod.POST, c + "/lu", null, "COL_MESSAGE_READ", 204),
            // « En train d'écrire » (D-72) : relève de l'envoi.
            Arguments.of(HttpMethod.POST, c + "/saisie", null, "COL_MESSAGE_SEND", 204),
            Arguments.of(HttpMethod.GET, "/api/v1/messagerie/non-lus", null, "COL_MESSAGE_READ", 200),
            Arguments.of(HttpMethod.GET, "/api/v1/messagerie/pieces/" + PIECE.getId(), null, "COL_MESSAGE_READ", 200),
            // Groupes de discussion (D-68) : gérer un groupe relève de l'envoi.
            Arguments.of(HttpMethod.PATCH, c, "json:{\"titre\":\"Comité\"}", "COL_MESSAGE_SEND", 200),
            Arguments.of(HttpMethod.POST, c + "/membres", "json:{\"membres\":[\"serge\"]}", "COL_MESSAGE_SEND", 200),
            Arguments.of(HttpMethod.DELETE, c + "/membres/serge", null, "COL_MESSAGE_SEND", 204)
        );
    }

    static RequestPostProcessor jeton(String acr, String... permissions) {
        return jwt()
            .jwt(j -> {
                j.subject(MOI).claim("name", "Paul Mbarga Atangana");
                if (acr != null) j.claim("acr", acr);
            })
            .authorities(Stream.of(permissions).map(SimpleGrantedAuthority::new).toArray(GrantedAuthority[]::new));
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("routes")
    @DisplayName("401 sans jeton, 403 sans second facteur, 403 sans la permission, accès avec elle")
    void matrice(HttpMethod m, String uri, String message, String permission, int ok) throws Exception {
        var r = requete(m, uri, message);
        mvc.perform(r.get()).andExpect(status().isUnauthorized());
        mvc.perform(r.get().with(jeton(null, permission))).andExpect(status().isForbidden());
        mvc.perform(r.get().with(jeton("1", permission))).andExpect(status().isForbidden());
        String[] autres = TOUTES.stream().filter(p -> !p.equals(permission)).toArray(String[]::new);
        mvc.perform(r.get().with(jeton("2", autres))).andExpect(status().isForbidden());
        mvc.perform(r.get().with(jeton("2", permission))).andExpect(status().is(ok));
        mvc.perform(r.get().with(jeton("3", permission))).andExpect(status().is(ok));
    }

    @Test
    @DisplayName("Formes : pages, conversation, fil, non-lus, création (201 ou 200 si réutilisée), téléchargement en pièce jointe")
    void formes() throws Exception {
        UUID cid = CONVERSATION.getId();
        mvc
            .perform(request(HttpMethod.GET, "/api/v1/messagerie/annuaire").with(jeton("2", "COL_MESSAGE_READ")))
            .andExpect(jsonPath("$.contenu[0].nom").value("Aminata Ngono"))
            .andExpect(jsonPath("$.contenu[0].fonction").value("Conductrice"))
            .andExpect(jsonPath("$.total").value(1))
            .andExpect(jsonPath("$.taille").value(20));
        verify(lecture).annuaire(MOI, null, 0, 20);
        mvc
            .perform(request(HttpMethod.GET, "/api/v1/messagerie/conversations?recherche=rec&liaisonId=" + CONVERSATION.getLiaisonId()).with(jeton("2", "COL_MESSAGE_READ")))
            .andExpect(jsonPath("$.contenu[0].type").value("GROUPE"))
            .andExpect(jsonPath("$.contenu[0].liaison.reference").value("LIA-2026-027"))
            .andExpect(jsonPath("$.contenu[0].membres.length()").value(2))
            .andExpect(jsonPath("$.contenu[0].dernierMessage.piecesJointes").value(1))
            .andExpect(jsonPath("$.contenu[0].nonLus").value(2))
            .andExpect(jsonPath("$.contenu[0].creeeLe").value("2026-09-30T08:00:00Z"));
        verify(lecture).conversations(MOI, "rec", CONVERSATION.getLiaisonId(), 0, 20);
        mvc
            .perform(request(HttpMethod.GET, "/api/v1/messagerie/conversations/" + cid + "/messages?avant=" + MESSAGE.getId()).with(jeton("2", "COL_MESSAGE_READ")))
            .andExpect(jsonPath("$.plusAnciens").value(true))
            .andExpect(jsonPath("$.contenu[0].moi").value(true))
            .andExpect(jsonPath("$.contenu[0].auteur.nom").value("Paul Mbarga Atangana"))
            .andExpect(jsonPath("$.contenu[0].etiquettes[0].reference").value("LIA-2026-027"))
            .andExpect(jsonPath("$.contenu[0].piecesJointes[0].empreinte").value("e".repeat(64)))
            .andExpect(jsonPath("$.contenu[0].conversationId").value(cid.toString()));
        verify(lecture).messages(MOI, cid, MESSAGE.getId(), 50);
        mvc.perform(request(HttpMethod.GET, "/api/v1/messagerie/non-lus").with(jeton("2", "COL_MESSAGE_READ"))).andExpect(jsonPath("$.total").value(3));
        mvc
            .perform(requete(HttpMethod.POST, "/api/v1/messagerie/conversations", CREATION).get().with(jeton("2", "COL_MESSAGE_SEND")))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "/api/v1/messagerie/conversations/" + cid))
            .andExpect(jsonPath("$.id").value(cid.toString()));
        when(envoi.creerConversation(any(), any(), any())).thenReturn(new Envoi(cid, MESSAGE.getId(), false));
        mvc.perform(requete(HttpMethod.POST, "/api/v1/messagerie/conversations", CREATION).get().with(jeton("2", "COL_MESSAGE_SEND"))).andExpect(status().isOk());
        mvc
            .perform(request(HttpMethod.GET, "/api/v1/messagerie/pieces/" + PIECE.getId()).with(jeton("2", "COL_MESSAGE_READ")))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.startsWith("attachment; filename=")))
            .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("pv")))
            .andExpect(header().string("X-Content-Type-Options", "nosniff"))
            .andExpect(header().string("Repr-Digest", "sha-256=:" + java.util.Base64.getEncoder().encodeToString(java.util.HexFormat.of().parseHex("e".repeat(64))) + ":"))
            .andExpect(content().contentType(MediaType.APPLICATION_PDF))
            .andExpect(content().bytes(new byte[] { 'a', 'b', 'c' }));
    }

    @Test
    @DisplayName("Groupes (D-68) : nom, membres ajoutés, retrait transmis ; drapeau « groupe » de la création ; administrateur et événement exposés")
    void groupes() throws Exception {
        UUID cid = CONVERSATION.getId();
        when(lecture.conversation(MOI, cid)).thenReturn(
            new ConversationLue(
                CONVERSATION,
                CONVERSATION.getMembres(),
                new DernierMessage(MESSAGE.getId(), "Paul Mbarga Atangana", "", T, 0, EvenementGroupe.GROUPE_RENOMME, List.of("Comité")),
                0
            )
        );
        mvc
            .perform(requete(HttpMethod.PATCH, "/api/v1/messagerie/conversations/" + cid, "json:{\"titre\":\"Comité\"}").get().with(jeton("2", "COL_MESSAGE_SEND")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.membres[0].administrateur").isBoolean())
            .andExpect(jsonPath("$.dernierMessage.evenement.type").value("GROUPE_RENOMME"))
            .andExpect(jsonPath("$.dernierMessage.evenement.cibles[0]").value("Comité"));
        verify(groupes).renommer(new Expediteur(MOI, "Paul Mbarga Atangana"), cid, "Comité");
        mvc.perform(requete(HttpMethod.POST, "/api/v1/messagerie/conversations/" + cid + "/membres", "json:{\"membres\":[\"serge\",\"fatime\"]}").get().with(jeton("2", "COL_MESSAGE_SEND")));
        verify(groupes).ajouterMembres(new Expediteur(MOI, "Paul Mbarga Atangana"), cid, List.of("serge", "fatime"));
        mvc.perform(request(HttpMethod.DELETE, "/api/v1/messagerie/conversations/" + cid + "/membres/" + MOI).with(jeton("2", "COL_MESSAGE_SEND"))).andExpect(status().isNoContent());
        verify(groupes).retirerMembre(new Expediteur(MOI, "Paul Mbarga Atangana"), cid, MOI);
        ArgumentCaptor<EnvoyerMessageUseCase.NouvelleConversation> d = ArgumentCaptor.forClass(EnvoyerMessageUseCase.NouvelleConversation.class);
        String groupe = CREATION.replaceFirst("\\{", "{\"groupe\":true,");
        mvc.perform(requete(HttpMethod.POST, "/api/v1/messagerie/conversations", groupe).get().with(jeton("2", "COL_MESSAGE_SEND")));
        verify(envoi, org.mockito.Mockito.atLeastOnce()).creerConversation(any(), d.capture(), any());
        assertThat(d.getValue().groupe()).isTrue();
    }

    @Test
    @DisplayName("Envoi multipart : message JSON et pièces jointes transmis au cas d'utilisation")
    @SuppressWarnings("unchecked")
    void multipartAvecPieces() throws Exception {
        UUID cid = CONVERSATION.getId();
        mvc
            .perform(
                multipart("/api/v1/messagerie/conversations/" + cid + "/messages")
                    .file(new MockMultipartFile("message", "", MediaType.APPLICATION_JSON_VALUE, ENVOI.getBytes()))
                    .file(new MockMultipartFile("fichiers", "pv.pdf", "application/pdf", new byte[] { 1, 2 }))
                    .file(new MockMultipartFile("fichiers", "note.txt", "text/plain", new byte[] { 3 }))
                    .with(jeton("2", "COL_MESSAGE_SEND"))
            )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.contenu").value("Bonjour"));
        ArgumentCaptor<Expediteur> e = ArgumentCaptor.forClass(Expediteur.class);
        ArgumentCaptor<EnvoyerMessageUseCase.NouveauMessage> n = ArgumentCaptor.forClass(EnvoyerMessageUseCase.NouveauMessage.class);
        ArgumentCaptor<List<FichierJoint>> f = ArgumentCaptor.forClass(List.class);
        verify(envoi).envoyer(e.capture(), eq(cid), n.capture(), f.capture());
        assertThat(e.getValue()).isEqualTo(new Expediteur(MOI, "Paul Mbarga Atangana"));
        assertThat(n.getValue().contenu()).isEqualTo("Suite");
        assertThat(n.getValue().etiquettes()).extracting(Etiquette::reference).containsExactly("LIA-2026-027");
        assertThat(f.getValue()).extracting(FichierJoint::nom).containsExactly("pv.pdf", "note.txt");
        assertThat(f.getValue().get(0).taille()).isEqualTo(2);
        assertThat(f.getValue().get(0).source().ouvrir().readAllBytes()).containsExactly(1, 2);
    }

    @Test
    @DisplayName("D-74 : tickets et transferts dans la partie « message », sans aucune partie « fichiers » (envoi et création)")
    @SuppressWarnings("unchecked")
    void multipartSansFichier() throws Exception {
        UUID cid = CONVERSATION.getId();
        UUID ticket = UUID.randomUUID();
        UUID piece = UUID.randomUUID();
        String corps = "{\"contenu\":\"\",\"documents\":[\"" + ticket + "\"],\"transferts\":[\"" + piece + "\"]}";
        mvc
            .perform(requete(HttpMethod.POST, "/api/v1/messagerie/conversations/" + cid + "/messages", corps).get().with(jeton("2", "COL_MESSAGE_SEND")))
            .andExpect(status().isCreated());
        ArgumentCaptor<EnvoyerMessageUseCase.NouveauMessage> n = ArgumentCaptor.forClass(EnvoyerMessageUseCase.NouveauMessage.class);
        ArgumentCaptor<List<FichierJoint>> f = ArgumentCaptor.forClass(List.class);
        verify(envoi).envoyer(any(), eq(cid), n.capture(), f.capture());
        assertThat(n.getValue().documents()).containsExactly(ticket);
        assertThat(n.getValue().transferts()).containsExactly(piece);
        assertThat(n.getValue().etiquettes()).isNull();
        assertThat(f.getValue()).isEmpty();

        // Sans les champs : listes vides transmises.
        mvc.perform(requete(HttpMethod.POST, "/api/v1/messagerie/conversations/" + cid + "/messages", ENVOI).get().with(jeton("2", "COL_MESSAGE_SEND")));
        verify(envoi, org.mockito.Mockito.times(2)).envoyer(any(), eq(cid), n.capture(), any());
        assertThat(n.getValue().documents()).isEmpty();
        assertThat(n.getValue().transferts()).isEmpty();

        String creation = CREATION.replaceFirst("\\{", "{\"documents\":[\"" + ticket + "\"],\"transferts\":[\"" + piece + "\"],");
        mvc.perform(requete(HttpMethod.POST, "/api/v1/messagerie/conversations", creation).get().with(jeton("2", "COL_MESSAGE_SEND"))).andExpect(status().isCreated());
        ArgumentCaptor<EnvoyerMessageUseCase.NouvelleConversation> d = ArgumentCaptor.forClass(EnvoyerMessageUseCase.NouvelleConversation.class);
        verify(envoi).creerConversation(any(), d.capture(), f.capture());
        assertThat(d.getValue().documents()).containsExactly(ticket);
        assertThat(d.getValue().transferts()).containsExactly(piece);
        assertThat(f.getValue()).isEmpty();

        when(envoi.envoyer(any(), eq(cid), any(), any())).thenThrow(CollaborationException.invalide("COL_DOCUMENT_INVALID", "x", Map.of("id", ticket.toString())));
        mvc
            .perform(requete(HttpMethod.POST, "/api/v1/messagerie/conversations/" + cid + "/messages", corps).get().with(jeton("2", "COL_MESSAGE_SEND")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("COL_DOCUMENT_INVALID"));
    }

    /** Flux de la taille donnée, produit à la demande (rien en mémoire) ; retient la plus grande lecture demandée et sa fermeture. */
    static final class FluxGenere extends java.io.InputStream {

        final long taille;
        long lus;
        int plusGrandeLecture;
        boolean ferme;

        FluxGenere(long taille) {
            this.taille = taille;
        }

        @Override
        public int read() {
            if (lus >= taille) {
                return -1;
            }
            return (int) (lus++ % 251);
        }

        @Override
        public int read(byte[] b, int off, int len) {
            plusGrandeLecture = Math.max(plusGrandeLecture, len);
            if (lus >= taille) {
                return -1;
            }
            int n = (int) Math.min(len, taille - lus);
            for (int i = 0; i < n; i++) {
                b[off + i] = (byte) ((lus + i) % 251);
            }
            lus += n;
            return n;
        }

        @Override
        public void close() {
            ferme = true;
        }
    }

    @Test
    @DisplayName("D-74 : téléchargement relayé en flux par tampons, au-delà de 20 Mo ; Content-Length, Repr-Digest et Content-Disposition conservés")
    void telechargementEnFlux() throws Exception {
        long taille = 24L * 1024 * 1024 + 7;
        Message m = new Message(UUID.randomUUID(), CONVERSATION, PAUL, new byte[] { 1 }, T);
        PieceJointe grosse = m.joindre(UUID.randomUUID(), "atelier.mp4", taille, "video/mp4", "c".repeat(64));
        FluxGenere flux = new FluxGenere(taille);
        when(lecture.piece(anyString(), eq(grosse.getId()))).thenReturn(new PieceATelecharger(grosse, new ContenuDocument(flux, -1)));

        var r = mvc
            .perform(request(HttpMethod.GET, "/api/v1/messagerie/pieces/" + grosse.getId()).with(jeton("2", "COL_MESSAGE_READ")))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Length", String.valueOf(taille)))
            .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("atelier.mp4")))
            .andExpect(header().exists("Repr-Digest"))
            .andExpect(content().contentType("video/mp4"))
            .andReturn();
        assertThat(r.getResponse().getContentAsByteArray()).hasSize((int) taille);
        assertThat(flux.lus).isEqualTo(taille);
        // Recopie par tampons : jamais une lecture du fichier entier.
        assertThat(flux.plusGrandeLecture).isPositive().isLessThanOrEqualTo(64 * 1024);
        assertThat(flux.ferme).isTrue();
    }

    @Test
    @DisplayName("Erreurs métier en problem+json avec code ; non-membre : 404 ; annuaire indisponible : 503")
    void erreurs() throws Exception {
        UUID inconnue = UUID.randomUUID();
        when(lecture.conversation(anyString(), eq(inconnue))).thenThrow(CollaborationException.introuvable("COL_CONVERSATION_NOT_FOUND", inconnue));
        mvc
            .perform(request(HttpMethod.GET, "/api/v1/messagerie/conversations/" + inconnue).with(jeton("2", "COL_MESSAGE_READ")))
            .andExpect(status().isNotFound())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.code").value("COL_CONVERSATION_NOT_FOUND"))
            .andExpect(jsonPath("$.correlationId").isNotEmpty());
        when(envoi.creerConversation(any(), any(), any())).thenThrow(CollaborationException.invalide("COL_RECIPIENT_SELF", "x", Map.of()));
        mvc
            .perform(requete(HttpMethod.POST, "/api/v1/messagerie/conversations", CREATION).get().with(jeton("2", "COL_MESSAGE_SEND")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("COL_RECIPIENT_SELF"));
        when(lecture.annuaire(anyString(), any(), anyInt(), anyInt())).thenThrow(CollaborationException.indisponible("COL_DIRECTORY_UNAVAILABLE", "x"));
        mvc
            .perform(request(HttpMethod.GET, "/api/v1/messagerie/annuaire").with(jeton("2", "COL_MESSAGE_READ")))
            .andExpect(status().isServiceUnavailable())
            .andExpect(jsonPath("$.code").value("COL_DIRECTORY_UNAVAILABLE"));
        var pd = new CollaborationExceptionHandler().tropVolumineux(new MaxUploadSizeExceededException(1));
        assertThat(pd.getStatusCode().value()).isEqualTo(400);
        assertThat(pd.getBody().getProperties()).containsEntry("code", "COL_ATTACHMENT_TOO_LARGE");
    }

    @Test
    @DisplayName("CRUD généré réservé à l'administration technique ; routes internes au jeton de service")
    void cloisonnement() throws Exception {
        mvc.perform(request(HttpMethod.GET, "/api/rooms").with(jeton("2", TOUTES.toArray(String[]::new)))).andExpect(status().isForbidden());
        mvc.perform(request(HttpMethod.GET, "/api/v1/interne/quelconque").with(jeton("2", TOUTES.toArray(String[]::new)))).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Repr-Digest : empreinte SHA-256 en base 64 ; absente si l'empreinte est mal formée")
    void reprDigest() {
        assertThat(MessagerieResource.reprDigest("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad")).isEqualTo(
            "sha-256=:ungWv48Bz+pBQUDeXa4iI7ADYaOWF3qctBD/YfIAFa0=:"
        );
        assertThat(MessagerieResource.reprDigest(null)).isNull();
        assertThat(MessagerieResource.reprDigest("pas-une-empreinte")).isNull();
    }

    @Test
    @DisplayName("Appelant : sujet du jeton, nom affiché ou identifiant de connexion ; sans jeton utilisateur : refus")
    void appelant() {
        Jwt avecNom = Jwt.withTokenValue("t").header("alg", "none").subject("s").claim("name", "Nom Complet").build();
        Jwt sansNom = Jwt.withTokenValue("t").header("alg", "none").subject("s").claim("preferred_username", "identifiant").build();
        Jwt nomVide = Jwt.withTokenValue("t").header("alg", "none").subject("s").claim("name", " ").build();
        Jwt sansSujet = Jwt.withTokenValue("t").header("alg", "none").claim("name", "x").build();
        Jwt sujetVide = Jwt.withTokenValue("t").header("alg", "none").subject(" ").build();
        assertThat(MessagerieResource.appelant(new JwtAuthenticationToken(avecNom))).isEqualTo(new Expediteur("s", "Nom Complet"));
        assertThat(MessagerieResource.appelant(new JwtAuthenticationToken(sansNom))).isEqualTo(new Expediteur("s", "identifiant"));
        assertThat(MessagerieResource.appelant(new JwtAuthenticationToken(nomVide)).nomJeton()).isNull();
        for (var auth : List.of(new JwtAuthenticationToken(sansSujet), new JwtAuthenticationToken(sujetVide), new TestingAuthenticationToken("u", "p"))) {
            assertThatThrownBy(() -> MessagerieResource.appelant(auth)).isInstanceOfSatisfying(CollaborationException.class, e ->
                assertThat(e.code()).isEqualTo("COL_USER_TOKEN_REQUIRED")
            );
        }
    }

    @Test
    @DisplayName("mon-compte.md §1 : avatarVersion dans l'annuaire, les membres des conversations et les auteurs des messages")
    void avatarVersions() throws Exception {
        UUID cid = CONVERSATION.getId();
        when(lecture.annuaire(anyString(), any(), anyInt(), anyInt())).thenReturn(
            new Page<>(List.of(new Collaborateur("aminata", "Aminata Ngono", "Conductrice", Set.of(), null, true, "0123456789abcdef")), 1, 0, 20)
        );
        when(lecture.versionsAvatar(any())).thenReturn(java.util.Map.of(MOI, "fedcba9876543210", "aminata", "0123456789abcdef"));
        mvc.perform(request(HttpMethod.GET, "/api/v1/messagerie/annuaire").with(jeton("2", "COL_MESSAGE_READ"))).andExpect(jsonPath("$.contenu[0].avatarVersion").value("0123456789abcdef"));
        mvc
            .perform(request(HttpMethod.GET, "/api/v1/messagerie/conversations").with(jeton("2", "COL_MESSAGE_READ")))
            .andExpect(jsonPath("$.contenu[0].membres[?(@.id == 'aminata')].avatarVersion").value(org.hamcrest.Matchers.hasItem("0123456789abcdef")))
            .andExpect(jsonPath("$.contenu[0].membres[?(@.id == '" + MOI + "')].avatarVersion").value(org.hamcrest.Matchers.hasItem("fedcba9876543210")));
        mvc
            .perform(request(HttpMethod.GET, "/api/v1/messagerie/conversations/" + cid).with(jeton("2", "COL_MESSAGE_READ")))
            .andExpect(jsonPath("$.membres[?(@.id == 'aminata')].avatarVersion").value(org.hamcrest.Matchers.hasItem("0123456789abcdef")));
        mvc
            .perform(request(HttpMethod.GET, "/api/v1/messagerie/conversations/" + cid + "/messages").with(jeton("2", "COL_MESSAGE_READ")))
            .andExpect(jsonPath("$.contenu[0].auteur.avatarVersion").value("fedcba9876543210"));
        verify(lecture, org.mockito.Mockito.atLeastOnce()).versionsAvatar(Set.of(MOI, "aminata"));
        verify(lecture, org.mockito.Mockito.atLeastOnce()).versionsAvatar(Set.of(MOI));
        when(lecture.versionsAvatar(any())).thenReturn(java.util.Map.of());
        mvc
            .perform(request(HttpMethod.GET, "/api/v1/messagerie/conversations/" + cid + "/messages").with(jeton("2", "COL_MESSAGE_READ")))
            .andExpect(jsonPath("$.contenu[0].auteur.avatarVersion").value(org.hamcrest.Matchers.nullValue()));
    }
}
