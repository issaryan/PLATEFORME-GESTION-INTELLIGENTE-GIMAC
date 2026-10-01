package cm.gimac.des.document.web.rest.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cm.gimac.des.document.application.port.in.DeposerDocumentUseCase;
import cm.gimac.des.document.application.port.in.DeposerDocumentUseCase.Commande;
import cm.gimac.des.document.application.port.in.DeposerDocumentUseCase.Deposant;
import cm.gimac.des.document.application.port.in.DeposerDocumentUseCase.Resultat;
import cm.gimac.des.document.application.port.in.LireDocumentUseCase;
import cm.gimac.des.document.application.port.in.LireDocumentUseCase.Contenu;
import cm.gimac.des.document.config.SecurityConfiguration;
import cm.gimac.des.document.config.TestSecurityConfiguration;
import cm.gimac.des.document.domain.DocumentException;
import cm.gimac.des.document.domain.service.EmpreinteSha256;
import cm.gimac.des.document.web.rest.errors.DocumentExceptionHandler;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tech.jhipster.config.JHipsterProperties;

/**
 * Tests de sécurité des routes de MS-DOCUMENT (D-61) avec la vraie chaîne de filtres : 401 sans
 * jeton ; 403 sans second facteur (acr) ; 403 sans l'une des permissions de dépôt ; accès avec
 * chacune ; interface interne refusée à tout jeton utilisateur et à tout jeton de service sans la
 * permission interne de la route. Exécutés sans Docker (tranche web, cas d'utilisation simulé).
 */
@WebMvcTest(controllers = { DocumentDepotResource.class, DocumentInterneResource.class })
@Import({ SecurityConfiguration.class, TestSecurityConfiguration.class, DocumentExceptionHandler.class })
@EnableConfigurationProperties(JHipsterProperties.class)
class DocumentResourceSecurityTest {

    static final UUID ID = UUID.randomUUID();
    static final String SHA = "0f".repeat(32);
    static final String DEPOT = "/api/v1/documents";
    static final String EMPREINTE = "/api/v1/interne/documents/" + ID + "/empreinte";
    static final String[] PERMISSIONS_DE_DEPOT = { "PAR_ADHESION_INSTRUCT", "HOM_EVIDENCE_SUBMIT", "HOM_STEP_MANAGE", "CER_CHANGE_RECORD_LINK", "HOM_PORTAL_SUBMIT", "DOC_DEPOSIT" };

    @Autowired
    MockMvc mvc;

    @MockitoBean
    DeposerDocumentUseCase useCase;

    @MockitoBean
    LireDocumentUseCase lecture;

    @BeforeEach
    void init() {
        when(useCase.deposer(any(), any())).thenReturn(new Resultat(ID, SHA, "statuts.pdf", 1234, "application/pdf"));
        when(useCase.empreinte(ID)).thenReturn(SHA);
    }

    static MockMultipartHttpServletRequestBuilder depot() {
        MockMultipartHttpServletRequestBuilder b = multipart(DEPOT);
        b.file(new MockMultipartFile("fichier", "statuts.pdf", "application/pdf", "%PDF-1.7".getBytes(StandardCharsets.US_ASCII)));
        b.param("finalite", "ADHESION");
        b.param("reference", ID + "/STATUTS");
        return b;
    }

    /** Jeton utilisateur : sujet, niveau d'assurance acr, permissions de la matrice des rôles. */
    static RequestPostProcessor jeton(String acr, String... permissions) {
        return jwt()
            .jwt(j -> {
                j.subject("agent-ddrm").claim("institution_id", "inst-1");
                if (acr != null) {
                    j.claim("acr", acr);
                }
            })
            .authorities(Stream.of(permissions).map(SimpleGrantedAuthority::new).toArray(GrantedAuthority[]::new));
    }

    /** Jeton de service (client_credentials) : compte de service du client, pas d'acr, permissions internes. */
    static RequestPostProcessor jetonDeService(String client, String... permissions) {
        return jwt()
            .jwt(j -> j.subject(UUID.randomUUID().toString()).claim("azp", client).claim("preferred_username", "service-account-" + client))
            .authorities(Stream.of(permissions).map(SimpleGrantedAuthority::new).toArray(GrantedAuthority[]::new));
    }

    static RequestPostProcessor jetonDeService() {
        return jetonDeService("participant-service", "INTERNAL_DOCUMENT_READ");
    }

    // ------------------------------------------------------------------ POST /api/v1/documents

    @Test
    @DisplayName("Dépôt sans jeton : 401")
    void depotSansJeton() throws Exception {
        mvc.perform(depot()).andExpect(status().isUnauthorized());
        verifyNoInteractions(useCase);
    }

    @Test
    @DisplayName("D-61 : dépôt avec un jeton sans second facteur (acr absent ou 1) : 403, même avec la permission")
    void depotSansDoubleFacteur() throws Exception {
        mvc.perform(depot().with(jeton(null, PERMISSIONS_DE_DEPOT))).andExpect(status().isForbidden());
        mvc.perform(depot().with(jeton("1", PERMISSIONS_DE_DEPOT))).andExpect(status().isForbidden());
        verifyNoInteractions(useCase);
    }

    @Test
    @DisplayName("Matrice des rôles : dépôt sans aucune permission de dépôt, 403")
    void depotSansPermission() throws Exception {
        mvc.perform(depot().with(jeton("2", "PAR_INSTITUTION_READ", "INTERNAL_DOCUMENT_READ", "HOM_PORTAL_READ"))).andExpect(status().isForbidden());
        verifyNoInteractions(useCase);
    }

    @ParameterizedTest
    @ValueSource(strings = { "PAR_ADHESION_INSTRUCT", "HOM_EVIDENCE_SUBMIT", "HOM_STEP_MANAGE", "CER_CHANGE_RECORD_LINK", "HOM_PORTAL_SUBMIT", "DOC_DEPOSIT" })
    @DisplayName("Dépôt avec le second facteur et l'une des permissions : 201")
    void depotAvecPermission(String permission) throws Exception {
        mvc
            .perform(depot().with(jeton("2", permission)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(ID.toString()))
            .andExpect(jsonPath("$.sha256").value(SHA))
            .andExpect(jsonPath("$.nomFichier").value("statuts.pdf"))
            .andExpect(jsonPath("$.taille").value(1234))
            .andExpect(jsonPath("$.typeMime").value("application/pdf"));
        mvc.perform(depot().with(jeton("3", permission))).andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Jeton de service sans acr : aucun dépôt par la route utilisateur")
    void depotJetonDeService() throws Exception {
        mvc.perform(depot().with(jetonDeService())).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Commande transmise au cas d'utilisation : fichier, finalité, référence, déposant et institution du jeton")
    void commandeTransmise() throws Exception {
        mvc.perform(depot().with(jeton("2", "DOC_DEPOSIT"))).andExpect(status().isCreated());

        ArgumentCaptor<Commande> commande = ArgumentCaptor.forClass(Commande.class);
        ArgumentCaptor<Deposant> deposant = ArgumentCaptor.forClass(Deposant.class);
        verify(useCase).deposer(commande.capture(), deposant.capture());
        assertThat(commande.getValue().finalite()).isEqualTo("ADHESION");
        assertThat(commande.getValue().reference()).isEqualTo(ID + "/STATUTS");
        assertThat(commande.getValue().fichier().nomFichier()).isEqualTo("statuts.pdf");
        assertThat(commande.getValue().fichier().typeMime()).isEqualTo("application/pdf");
        assertThat(commande.getValue().fichier().contenu()).asString(StandardCharsets.US_ASCII).isEqualTo("%PDF-1.7");
        assertThat(deposant.getValue()).isEqualTo(new Deposant("agent-ddrm", "inst-1"));
    }

    @Test
    @DisplayName("Partie « fichier » absente : transmise comme absente (le cas d'utilisation répond DOC_FILE_MISSING)")
    void fichierAbsent() throws Exception {
        when(useCase.deposer(any(), any())).thenThrow(DocumentException.invalide("DOC_FILE_MISSING", "La partie « fichier » est obligatoire"));
        mvc
            .perform(multipart(DEPOT).param("finalite", "ADHESION").param("reference", "r").with(jeton("2", "DOC_DEPOSIT")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("DOC_FILE_MISSING"));
        ArgumentCaptor<Commande> commande = ArgumentCaptor.forClass(Commande.class);
        verify(useCase).deposer(commande.capture(), any());
        assertThat(commande.getValue().fichier()).isNull();
    }

    @Test
    @DisplayName("Jeton sans sujet : 403 DOC_USER_TOKEN_REQUIRED")
    void sansSujet() throws Exception {
        mvc
            .perform(depot().with(jwt().jwt(j -> j.subject(" ").claim("acr", "2")).authorities(new SimpleGrantedAuthority("DOC_DEPOSIT"))))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("DOC_USER_TOKEN_REQUIRED"));
    }

    @Test
    @DisplayName("Fichier illisible à la réception : 400 DOC_FILE_UNREADABLE")
    void fichierIllisible() throws Exception {
        MockMultipartFile illisible = new MockMultipartFile("fichier", "x.pdf", "application/pdf", new byte[] { 1 }) {
            @Override
            public byte[] getBytes() throws IOException {
                throw new IOException("flux interrompu");
            }

            @Override
            public InputStream getInputStream() throws IOException {
                throw new IOException("flux interrompu");
            }
        };
        mvc
            .perform(multipart(DEPOT).file(illisible).param("finalite", "ADHESION").param("reference", "r").with(jeton("2", "DOC_DEPOSIT")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("DOC_FILE_UNREADABLE"));
    }

    @Test
    @DisplayName("Erreurs du dépôt en ProblemDetail : 413, 422, 503 avec code, détails et corrélation")
    void erreursDuDepot() throws Exception {
        when(useCase.deposer(any(), any())).thenThrow(DocumentException.tropVolumineux(20_971_521, 20_971_520));
        mvc
            .perform(depot().with(jeton("2", "DOC_DEPOSIT")).header("X-Correlation-Id", "corr-42"))
            .andExpect(status().isPayloadTooLarge())
            .andExpect(header().string("Content-Type", "application/problem+json"))
            .andExpect(jsonPath("$.code").value("DOC_FILE_TOO_LARGE"))
            .andExpect(jsonPath("$.message").value("Le fichier dépasse la taille maximale admise"))
            .andExpect(jsonPath("$.details.tailleMax").value(20_971_520))
            .andExpect(jsonPath("$.correlationId").value("corr-42"));

        doThrow(DocumentException.refuse("DOC_PAN_DETECTED", "Numéro de carte", Map.of("occurrences", 2))).when(useCase).deposer(any(), any());
        mvc
            .perform(depot().with(jeton("2", "DOC_DEPOSIT")))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("DOC_PAN_DETECTED"))
            .andExpect(jsonPath("$.details.occurrences").value(2))
            .andExpect(jsonPath("$.correlationId").exists());

        doThrow(DocumentException.indisponible("DOC_STORAGE_UNAVAILABLE", "Dépôt objet indisponible")).when(useCase).deposer(any(), any());
        mvc.perform(depot().with(jeton("2", "DOC_DEPOSIT"))).andExpect(status().isServiceUnavailable());
    }

    // ------------------------------------------------------------------ GET /api/v1/interne/documents/{id}/empreinte

    @Test
    @DisplayName("Empreinte sans jeton : 401")
    void empreinteSansJeton() throws Exception {
        mvc.perform(get(EMPREINTE)).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Interface interne refusée à un jeton utilisateur, même à deux facteurs et avec les permissions de dépôt")
    void empreinteRefuseeAUnUtilisateur() throws Exception {
        mvc.perform(get(EMPREINTE).with(jeton("2", PERMISSIONS_DE_DEPOT))).andExpect(status().isForbidden());
        verifyNoInteractions(useCase);
    }

    @Test
    @DisplayName("Jeton de service avec INTERNAL_DOCUMENT_READ : empreinte restituée")
    void empreinteAvecJetonDeService() throws Exception {
        mvc.perform(get(EMPREINTE).with(jetonDeService())).andExpect(status().isOk()).andExpect(jsonPath("$.sha256").value(SHA));
    }

    @Test
    @DisplayName("Document inconnu : 404 DOC_NOT_FOUND en ProblemDetail")
    void empreinteInconnue() throws Exception {
        UUID inconnu = UUID.randomUUID();
        when(useCase.empreinte(inconnu)).thenThrow(DocumentException.introuvable(inconnu));
        mvc
            .perform(get("/api/v1/interne/documents/" + inconnu + "/empreinte").with(jetonDeService()))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("DOC_NOT_FOUND"))
            .andExpect(jsonPath("$.details.id").value(inconnu.toString()))
            .andExpect(jsonPath("$.correlationId").exists());
    }

    @Test
    @DisplayName("Empreinte avec un jeton de service sans INTERNAL_DOCUMENT_READ : 403")
    void empreinteSansPermissionInterne() throws Exception {
        mvc.perform(get(EMPREINTE).with(jetonDeService(COLLABORATION, "INTERNAL_DOCUMENT_DEPOSIT"))).andExpect(status().isForbidden());
        verifyNoInteractions(useCase);
    }

    // ------------------------------------------------------------------ POST /api/v1/interne/documents

    static final String DEPOT_INTERNE = "/api/v1/interne/documents";
    static final String COLLABORATION = "collaboration-service";
    static final String CONTENU = "/api/v1/interne/documents/" + ID + "/contenu";

    static MockMultipartHttpServletRequestBuilder depotInterne() {
        MockMultipartHttpServletRequestBuilder b = multipart(DEPOT_INTERNE);
        b.file(new MockMultipartFile("fichier", "rapport.pdf", "application/pdf", "%PDF-1.7".getBytes(StandardCharsets.US_ASCII)));
        b.param("finalite", "MESSAGERIE");
        b.param("reference", "conversation/" + ID);
        b.param("deposantId", "7c1e0000-0000-0000-0000-00000000dd01");
        return b;
    }

    @Test
    @DisplayName("Dépôt interne sans jeton : 401")
    void depotInterneSansJeton() throws Exception {
        mvc.perform(depotInterne()).andExpect(status().isUnauthorized());
        verifyNoInteractions(useCase);
    }

    @Test
    @DisplayName("Dépôt interne refusé à un jeton utilisateur à deux facteurs, même porteur des permissions de dépôt ou de la permission interne")
    void depotInterneRefuseAUnUtilisateur() throws Exception {
        mvc.perform(depotInterne().with(jeton("2", PERMISSIONS_DE_DEPOT))).andExpect(status().isForbidden());
        mvc.perform(depotInterne().with(jeton("2", "INTERNAL_DOCUMENT_DEPOSIT"))).andExpect(status().isForbidden());
        verifyNoInteractions(useCase);
    }

    @Test
    @DisplayName("Dépôt interne : jeton dont le nom ne correspond pas au compte de service du client, 403")
    void depotInterneFauxCompteDeService() throws Exception {
        mvc
            .perform(
                depotInterne()
                    .with(
                        jwt()
                            .jwt(j -> j.subject("agent").claim("azp", "web_app").claim("preferred_username", "service-account-collaboration-service"))
                            .authorities(new SimpleGrantedAuthority("INTERNAL_DOCUMENT_DEPOSIT"))
                    )
            )
            .andExpect(status().isForbidden());
        verifyNoInteractions(useCase);
    }

    @Test
    @DisplayName("Dépôt interne avec un jeton de service sans INTERNAL_DOCUMENT_DEPOSIT : 403")
    void depotInterneSansPermission() throws Exception {
        mvc.perform(depotInterne().with(jetonDeService(COLLABORATION, "INTERNAL_DOCUMENT_READ", "DOC_DEPOSIT"))).andExpect(status().isForbidden());
        verifyNoInteractions(useCase);
    }

    @Test
    @DisplayName("Dépôt interne avec INTERNAL_DOCUMENT_DEPOSIT : 201, déposant = utilisateur transmis et client du service")
    void depotInterneAvecPermission() throws Exception {
        mvc
            .perform(depotInterne().with(jetonDeService(COLLABORATION, "INTERNAL_DOCUMENT_DEPOSIT")))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(ID.toString()))
            .andExpect(jsonPath("$.sha256").value(SHA))
            .andExpect(jsonPath("$.nomFichier").value("statuts.pdf"))
            .andExpect(jsonPath("$.taille").value(1234))
            .andExpect(jsonPath("$.typeMime").value("application/pdf"));

        ArgumentCaptor<Commande> commande = ArgumentCaptor.forClass(Commande.class);
        ArgumentCaptor<Deposant> deposant = ArgumentCaptor.forClass(Deposant.class);
        verify(useCase).deposer(commande.capture(), deposant.capture());
        assertThat(commande.getValue().finalite()).isEqualTo("MESSAGERIE");
        assertThat(commande.getValue().reference()).isEqualTo("conversation/" + ID);
        assertThat(commande.getValue().fichier().nomFichier()).isEqualTo("rapport.pdf");
        assertThat(commande.getValue().fichier().contenu()).asString(StandardCharsets.US_ASCII).isEqualTo("%PDF-1.7");
        assertThat(deposant.getValue()).isEqualTo(new Deposant("7c1e0000-0000-0000-0000-00000000dd01", null, COLLABORATION));
    }

    @Test
    @DisplayName("Dépôt interne : fichier absent transmis comme absent ; erreurs du dépôt en ProblemDetail (400, 413, 422)")
    void depotInterneErreurs() throws Exception {
        when(useCase.deposer(any(), any())).thenThrow(DocumentException.invalide("DOC_FILE_MISSING", "La partie « fichier » est obligatoire"));
        mvc
            .perform(
                multipart(DEPOT_INTERNE)
                    .param("finalite", "MESSAGERIE")
                    .param("reference", "r")
                    .param("deposantId", "u")
                    .with(jetonDeService(COLLABORATION, "INTERNAL_DOCUMENT_DEPOSIT"))
            )
            .andExpect(status().isBadRequest())
            .andExpect(header().string("Content-Type", "application/problem+json"))
            .andExpect(jsonPath("$.code").value("DOC_FILE_MISSING"));
        ArgumentCaptor<Commande> commande = ArgumentCaptor.forClass(Commande.class);
        verify(useCase).deposer(commande.capture(), any());
        assertThat(commande.getValue().fichier()).isNull();

        doThrow(DocumentException.tropVolumineux(20_971_521, 20_971_520)).when(useCase).deposer(any(), any());
        mvc
            .perform(depotInterne().with(jetonDeService(COLLABORATION, "INTERNAL_DOCUMENT_DEPOSIT")))
            .andExpect(status().isPayloadTooLarge())
            .andExpect(jsonPath("$.code").value("DOC_FILE_TOO_LARGE"));

        doThrow(DocumentException.refuse("DOC_PAN_DETECTED", "Numéro de carte", Map.of("occurrences", 1))).when(useCase).deposer(any(), any());
        mvc
            .perform(depotInterne().with(jetonDeService(COLLABORATION, "INTERNAL_DOCUMENT_DEPOSIT")))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("DOC_PAN_DETECTED"));
    }

    // ------------------------------------------------------------------ GET /api/v1/interne/documents/{id}/contenu

    static final byte[] OCTETS = "Compte rendu de la réunion".getBytes(StandardCharsets.UTF_8);

    void contenuConnu(String nom, String type) {
        when(lecture.lire(eq(ID), any())).thenReturn(new Contenu(ID, nom, type, EmpreinteSha256.de(OCTETS), OCTETS));
    }

    @Test
    @DisplayName("Contenu sans jeton : 401")
    void contenuSansJeton() throws Exception {
        mvc.perform(get(CONTENU)).andExpect(status().isUnauthorized());
        verifyNoInteractions(lecture);
    }

    @Test
    @DisplayName("Contenu refusé à un jeton utilisateur à deux facteurs, même porteur de INTERNAL_DOCUMENT_READ")
    void contenuRefuseAUnUtilisateur() throws Exception {
        mvc.perform(get(CONTENU).with(jeton("2", PERMISSIONS_DE_DEPOT))).andExpect(status().isForbidden());
        mvc.perform(get(CONTENU).with(jeton("2", "INTERNAL_DOCUMENT_READ"))).andExpect(status().isForbidden());
        verifyNoInteractions(lecture);
    }

    @Test
    @DisplayName("Contenu avec un jeton de service sans INTERNAL_DOCUMENT_READ : 403")
    void contenuSansPermission() throws Exception {
        mvc.perform(get(CONTENU).with(jetonDeService(COLLABORATION, "INTERNAL_DOCUMENT_DEPOSIT"))).andExpect(status().isForbidden());
        verifyNoInteractions(lecture);
    }

    @Test
    @DisplayName("Contenu avec INTERNAL_DOCUMENT_READ : 200, octets, type, taille, pièce jointe, Repr-Digest ; lecteur = client du service")
    void contenuAvecPermission() throws Exception {
        contenuConnu("compte-rendu.txt", "text/plain");
        byte[] corps = mvc
            .perform(get(CONTENU).with(jetonDeService(COLLABORATION, "INTERNAL_DOCUMENT_READ")))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type", "text/plain"))
            .andExpect(header().longValue("Content-Length", OCTETS.length))
            .andExpect(header().string("Content-Disposition", "attachment; filename*=UTF-8''compte-rendu.txt"))
            .andExpect(header().string("Repr-Digest", "sha-256=:" + Base64.getEncoder().encodeToString(sha256(OCTETS)) + ":"))
            .andExpect(header().string("X-Content-Type-Options", "nosniff"))
            .andReturn()
            .getResponse()
            .getContentAsByteArray();
        assertThat(corps).isEqualTo(OCTETS);
        verify(lecture).lire(ID, COLLABORATION);
    }

    @Test
    @DisplayName("Contenu : nom de fichier non ASCII encodé selon RFC 5987")
    void contenuNomNonAscii() throws Exception {
        contenuConnu("procès-verbal n°2.pdf", "application/pdf");
        mvc
            .perform(get(CONTENU).with(jetonDeService(COLLABORATION, "INTERNAL_DOCUMENT_READ")))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type", "application/pdf"))
            .andExpect(header().string("Content-Disposition", "attachment; filename*=UTF-8''proc%C3%A8s-verbal%20n%C2%B02.pdf"));
    }

    @Test
    @DisplayName("Contenu d'un document inconnu : 404 DOC_NOT_FOUND ; contenu altéré : 500 DOC_INTEGRITY_MISMATCH")
    void contenuErreurs() throws Exception {
        UUID inconnu = UUID.randomUUID();
        when(lecture.lire(eq(inconnu), any())).thenThrow(DocumentException.introuvable(inconnu));
        mvc
            .perform(get("/api/v1/interne/documents/" + inconnu + "/contenu").with(jetonDeService(COLLABORATION, "INTERNAL_DOCUMENT_READ")))
            .andExpect(status().isNotFound())
            .andExpect(header().string("Content-Type", "application/problem+json"))
            .andExpect(jsonPath("$.code").value("DOC_NOT_FOUND"))
            .andExpect(jsonPath("$.details.id").value(inconnu.toString()));

        when(lecture.lire(eq(ID), any())).thenThrow(DocumentException.altere("DOC_INTEGRITY_MISMATCH", "Contenu altéré", ID));
        mvc
            .perform(get(CONTENU).with(jetonDeService(COLLABORATION, "INTERNAL_DOCUMENT_READ")))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.code").value("DOC_INTEGRITY_MISMATCH"));
    }

    @Test
    @DisplayName("Autre chemin ou autre méthode de l'interface interne : refusé même à un jeton de service porteur des permissions internes")
    void autreCheminInterne() throws Exception {
        RequestPostProcessor service = jetonDeService(COLLABORATION, "INTERNAL_DOCUMENT_READ", "INTERNAL_DOCUMENT_DEPOSIT");
        mvc.perform(get("/api/v1/interne/documents/" + ID + "/versions").with(service)).andExpect(status().isForbidden());
        mvc.perform(delete("/api/v1/interne/documents/" + ID).with(service)).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/interne/documents").with(service)).andExpect(status().isForbidden());
        verifyNoInteractions(useCase, lecture);
    }

    static byte[] sha256(byte[] octets) throws Exception {
        return java.security.MessageDigest.getInstance("SHA-256").digest(octets);
    }

    // ------------------------------------------------------------------ autres chemins

    @Test
    @DisplayName("Ressources générées par JHipster : réservées à l'administration technique")
    void crudGenereReserveAdmin() throws Exception {
        mvc.perform(get("/api/documents").with(jeton("2", PERMISSIONS_DE_DEPOT))).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Portail : institution obligatoire dans le jeton (RG-59)")
    void portail() throws Exception {
        mvc
            .perform(get("/api/v1/portail/documents").with(jwt().jwt(j -> j.claim("acr", "2")).authorities(new SimpleGrantedAuthority("DOC_DEPOSIT"))))
            .andExpect(status().isForbidden());
        // Institution présente : l'accès passe la chaîne de sécurité (aucune route du portail n'est encore exposée).
        mvc.perform(get("/api/v1/portail/documents").with(jeton("2", "DOC_DEPOSIT"))).andExpect(status().isNotFound());
    }
}
