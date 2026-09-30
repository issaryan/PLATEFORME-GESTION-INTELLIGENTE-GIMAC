package cm.gimac.des.document.web.rest.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import cm.gimac.des.document.IntegrationTest;
import cm.gimac.des.document.application.port.out.StockageObjet;
import cm.gimac.des.document.application.usecase.LectureDocumentService;
import cm.gimac.des.document.config.S3TestContainer;
import cm.gimac.des.document.domain.service.EmpreinteSha256;
import cm.gimac.des.document.infrastructure.stockage.StockageObjetS3;
import cm.gimac.des.document.infrastructure.texte.PdfDeTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * Intégration de l'interface interne utilisée par MS-COLLABORATION pour les pièces jointes de la
 * messagerie (D-64) : service complet (sécurité, contrôleur, cas d'utilisation, PostgreSQL et
 * passerelle S3 par Testcontainers), adaptateur S3 réel sur un compartiment à verrouillage
 * d'objets (WORM, D-50). Dépôt pour le compte d'un utilisateur puis relecture du contenu à
 * l'identique ; refus BNF-16, taille maximale, document inconnu, jetons refusés.
 */
@IntegrationTest
@AutoConfigureMockMvc
class DocumentInterneIT {

    static final String COMPARTIMENT = "gimac-documents-interne-it";
    static final String UTILISATEUR = "7c1e0000-0000-0000-0000-00000000dd07";
    static final String CLIENT = "collaboration-service";

    @DynamicPropertySource
    static void stockageReel(DynamicPropertyRegistry registre) {
        try (S3Client client = S3TestContainer.client(COMPARTIMENT)) {
            client.createBucket(b -> b.bucket(COMPARTIMENT).objectLockEnabledForBucket(true));
        }
        registre.add("gimac.adapters.stockage", () -> "real");
        registre.add("gimac.stockage.s3.point-acces", () -> S3TestContainer.pointAcces().toString());
        registre.add("gimac.stockage.s3.cle-acces", () -> S3TestContainer.CLE_ACCES);
        registre.add("gimac.stockage.s3.cle-secrete", () -> S3TestContainer.CLE_SECRETE);
        registre.add("gimac.stockage.s3.compartiment", () -> COMPARTIMENT);
        registre.add("gimac.stockage.s3.exiger-verrouillage", () -> "true");
    }

    @Autowired
    MockMvc mvc;

    @Autowired
    StockageObjet stockage;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    ObjectMapper json;

    static RequestPostProcessor service(String... permissions) {
        return jwt()
            .jwt(j -> j.subject(UUID.randomUUID().toString()).claim("azp", CLIENT).claim("preferred_username", "service-account-" + CLIENT))
            .authorities(java.util.Arrays.stream(permissions).map(SimpleGrantedAuthority::new).toArray(GrantedAuthority[]::new));
    }

    static RequestPostProcessor utilisateur() {
        return jwt()
            .jwt(j -> j.subject(UTILISATEUR).claim("acr", "2").claim("azp", "web_app").claim("preferred_username", "agent.ddrm"))
            .authorities(new SimpleGrantedAuthority("COL_MESSAGE_SEND"), new SimpleGrantedAuthority("DOC_DEPOSIT"));
    }

    ResultActions deposer(String nom, String type, byte[] contenu, String reference, RequestPostProcessor jeton) throws Exception {
        return mvc.perform(
            multipart("/api/v1/interne/documents")
                .file(new MockMultipartFile("fichier", nom, type, contenu))
                .param("finalite", "MESSAGERIE")
                .param("reference", reference)
                .param("deposantId", UTILISATEUR)
                .with(jeton)
        );
    }

    int lignes(String reference) {
        return jdbc.queryForObject("select count(*) from depot_document where reference = ?", Integer.class, reference);
    }

    static String reprDigest(byte[] octets) throws Exception {
        return "sha-256=:" + Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-256").digest(octets)) + ":";
    }

    @Test
    @DisplayName("Dépôt interne puis relecture : 201, objet WORM, déposant et service inscrits ; contenu relu identique, empreinte, en-têtes, lecture journalisée")
    void depotPuisRelecture() throws Exception {
        assertThat(stockage).isInstanceOf(StockageObjetS3.class);
        byte[] contenu = PdfDeTest.avecTexte("Compte rendu de la reunion du comite de pilotage");
        String reference = "conversation/" + UUID.randomUUID();

        String corps = deposer("compte rendu é.pdf", "application/pdf", contenu, reference, service("INTERNAL_DOCUMENT_DEPOSIT"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.sha256").value(EmpreinteSha256.de(contenu)))
            .andExpect(jsonPath("$.nomFichier").value("compte rendu é.pdf"))
            .andExpect(jsonPath("$.taille").value(contenu.length))
            .andExpect(jsonPath("$.typeMime").value("application/pdf"))
            .andReturn()
            .getResponse()
            .getContentAsString();
        JsonNode r = json.readTree(corps);
        UUID id = UUID.fromString(r.get("id").asText());

        Map<String, Object> ligne = jdbc.queryForMap("select * from depot_document where id = ?", id);
        assertThat(ligne)
            .containsEntry("finalite", "MESSAGERIE")
            .containsEntry("reference", reference)
            .containsEntry("deposant", UTILISATEUR)
            .containsEntry("service_deposant", CLIENT)
            .containsEntry("institution_id", null);
        assertThat((String) ligne.get("cle_objet")).startsWith("messagerie/").endsWith("/" + id);
        assertThat((String) ligne.get("version_objet")).isNotBlank();

        Logger journal = (Logger) LoggerFactory.getLogger(LectureDocumentService.class);
        ListAppender<ILoggingEvent> lectures = new ListAppender<>();
        lectures.start();
        journal.addAppender(lectures);
        try {
            MockHttpServletResponse reponse = mvc
                .perform(get("/api/v1/interne/documents/" + id + "/contenu").with(service("INTERNAL_DOCUMENT_READ")))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().longValue("Content-Length", contenu.length))
                .andExpect(header().string("Content-Disposition", "attachment; filename*=UTF-8''compte%20rendu%20%C3%A9.pdf"))
                .andExpect(header().string("Repr-Digest", reprDigest(contenu)))
                .andReturn()
                .getResponse();
            assertThat(reponse.getContentAsByteArray()).isEqualTo(contenu);
            assertThat(EmpreinteSha256.de(reponse.getContentAsByteArray())).isEqualTo(r.get("sha256").asText());
        } finally {
            journal.detachAppender(lectures);
        }
        assertThat(lectures.list)
            .extracting(ILoggingEvent::getFormattedMessage)
            .anySatisfy(m -> assertThat(m).contains(id.toString()).contains("lu par le service " + CLIENT));

        // L'empreinte reste lisible par l'interface interne existante.
        mvc
            .perform(get("/api/v1/interne/documents/" + id + "/empreinte").with(service("INTERNAL_DOCUMENT_READ")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sha256").value(EmpreinteSha256.de(contenu)));
    }

    @Test
    @DisplayName("BNF-16 : pièce jointe portant un numéro de carte en clair, 422 DOC_PAN_DETECTED, rien n'est conservé")
    void numeroDeCarte() throws Exception {
        String reference = "conversation/" + UUID.randomUUID();
        deposer("note.txt", "text/plain", "Carte du client : 4111 1111 1111 1111".getBytes(StandardCharsets.UTF_8), reference, service("INTERNAL_DOCUMENT_DEPOSIT"))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(header().string("Content-Type", "application/problem+json"))
            .andExpect(jsonPath("$.code").value("DOC_PAN_DETECTED"));
        assertThat(lignes(reference)).isZero();
    }

    @Test
    @DisplayName("Taille maximale : au-delà de 20 Mo, 413 DOC_FILE_TOO_LARGE ; déposant absent, 400 DOC_DEPOSITOR_INVALID")
    void admission() throws Exception {
        String reference = "conversation/" + UUID.randomUUID();
        deposer("gros.bin", "application/octet-stream", new byte[20 * 1024 * 1024 + 1], reference, service("INTERNAL_DOCUMENT_DEPOSIT"))
            .andExpect(status().isPayloadTooLarge())
            .andExpect(jsonPath("$.code").value("DOC_FILE_TOO_LARGE"));
        mvc
            .perform(
                multipart("/api/v1/interne/documents")
                    .file(new MockMultipartFile("fichier", "a.txt", "text/plain", new byte[] { 'a' }))
                    .param("finalite", "MESSAGERIE")
                    .param("reference", reference)
                    .with(service("INTERNAL_DOCUMENT_DEPOSIT"))
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("DOC_DEPOSITOR_INVALID"));
        assertThat(lignes(reference)).isZero();
    }

    @Test
    @DisplayName("Contenu d'un document inconnu : 404 DOC_NOT_FOUND en ProblemDetail")
    void inconnu() throws Exception {
        UUID inconnu = UUID.randomUUID();
        mvc
            .perform(get("/api/v1/interne/documents/" + inconnu + "/contenu").with(service("INTERNAL_DOCUMENT_READ")))
            .andExpect(status().isNotFound())
            .andExpect(header().string("Content-Type", "application/problem+json"))
            .andExpect(jsonPath("$.code").value("DOC_NOT_FOUND"))
            .andExpect(jsonPath("$.details.id").value(inconnu.toString()));
    }

    @Test
    @DisplayName("Sécurité : 401 sans jeton ; 403 pour un jeton utilisateur ; 403 pour un jeton de service sans la permission de la route")
    void securite() throws Exception {
        String reference = "conversation/" + UUID.randomUUID();
        byte[] contenu = "Ordre du jour".getBytes(StandardCharsets.UTF_8);
        mvc
            .perform(
                multipart("/api/v1/interne/documents")
                    .file(new MockMultipartFile("fichier", "odj.txt", "text/plain", contenu))
                    .param("finalite", "MESSAGERIE")
                    .param("reference", reference)
                    .param("deposantId", UTILISATEUR)
            )
            .andExpect(status().isUnauthorized());
        deposer("odj.txt", "text/plain", contenu, reference, utilisateur()).andExpect(status().isForbidden());
        deposer("odj.txt", "text/plain", contenu, reference, service("INTERNAL_DOCUMENT_READ")).andExpect(status().isForbidden());
        assertThat(lignes(reference)).isZero();

        String id = json
            .readTree(
                deposer("odj.txt", "text/plain", contenu, reference, service("INTERNAL_DOCUMENT_DEPOSIT"))
                    .andExpect(status().isCreated())
                    .andReturn()
                    .getResponse()
                    .getContentAsString()
            )
            .get("id")
            .asText();
        String route = "/api/v1/interne/documents/" + id + "/contenu";
        mvc.perform(get(route)).andExpect(status().isUnauthorized());
        mvc.perform(get(route).with(utilisateur())).andExpect(status().isForbidden());
        mvc.perform(get(route).with(service("INTERNAL_DOCUMENT_DEPOSIT"))).andExpect(status().isForbidden());
        mvc.perform(get(route).with(service("INTERNAL_DOCUMENT_READ"))).andExpect(status().isOk()).andExpect(header().string("Content-Type", "text/plain"));
    }
}
