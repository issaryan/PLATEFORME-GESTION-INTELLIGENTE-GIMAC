package cm.gimac.des.document.web.rest.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cm.gimac.des.document.application.port.in.DeposerDocumentUseCase;
import cm.gimac.des.document.application.port.in.DeposerDocumentUseCase.Commande;
import cm.gimac.des.document.application.port.in.DeposerDocumentUseCase.Deposant;
import cm.gimac.des.document.application.port.in.DeposerDocumentUseCase.Resultat;
import cm.gimac.des.document.config.SecurityConfiguration;
import cm.gimac.des.document.config.TestSecurityConfiguration;
import cm.gimac.des.document.domain.DocumentException;
import cm.gimac.des.document.web.rest.errors.DocumentExceptionHandler;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
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
 * chacune ; interface interne refusée à tout jeton utilisateur. Exécutés sans Docker (tranche
 * web, cas d'utilisation simulé).
 */
@WebMvcTest(controllers = { DocumentDepotResource.class, DocumentInterneResource.class })
@Import({ SecurityConfiguration.class, TestSecurityConfiguration.class, DocumentExceptionHandler.class })
@EnableConfigurationProperties(JHipsterProperties.class)
class DocumentResourceSecurityTest {

    static final UUID ID = UUID.randomUUID();
    static final String SHA = "0f".repeat(32);
    static final String DEPOT = "/api/v1/documents";
    static final String EMPREINTE = "/api/v1/interne/documents/" + ID + "/empreinte";
    static final String[] PERMISSIONS_DE_DEPOT = { "PAR_ADHESION_INSTRUCT", "HOM_EVIDENCE_SUBMIT", "HOM_PORTAL_SUBMIT", "DOC_DEPOSIT" };

    @Autowired
    MockMvc mvc;

    @MockitoBean
    DeposerDocumentUseCase useCase;

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

    /** Jeton de service (client_credentials) : pas d'acr, permission interne. */
    static RequestPostProcessor jetonDeService() {
        return jwt().jwt(j -> j.subject("service-account-participant")).authorities(new SimpleGrantedAuthority("INTERNAL_DOCUMENT_READ"));
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
    @ValueSource(strings = { "PAR_ADHESION_INSTRUCT", "HOM_EVIDENCE_SUBMIT", "HOM_PORTAL_SUBMIT", "DOC_DEPOSIT" })
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
