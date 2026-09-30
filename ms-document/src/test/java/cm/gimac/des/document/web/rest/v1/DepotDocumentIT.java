package cm.gimac.des.document.web.rest.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cm.gimac.des.document.IntegrationTest;
import cm.gimac.des.document.domain.service.EmpreinteSha256;
import cm.gimac.des.document.infrastructure.stockage.StockageObjetMemoire;
import cm.gimac.des.document.infrastructure.texte.PdfDeTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Intégration du dépôt direct : chaîne complète (sécurité, contrôleur, cas d'utilisation,
 * PostgreSQL par Testcontainers, changelog Liquibase manuel) avec le substitut du dépôt objet.
 */
@IntegrationTest
@AutoConfigureMockMvc
class DepotDocumentIT {

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    StockageObjetMemoire stockage;

    @Autowired
    ObjectMapper json;

    static RequestPostProcessor agentDdrm() {
        return jwt()
            .jwt(j -> j.subject("7c1e0000-0000-0000-0000-00000000dd01").claim("acr", "2"))
            .authorities(new SimpleGrantedAuthority("PAR_ADHESION_INSTRUCT"));
    }

    static RequestPostProcessor serviceParticipant() {
        return jwt().jwt(j -> j.subject("5e0c0000-0000-0000-0000-0000000000a1").claim("azp", "participant-service").claim("preferred_username", "service-account-participant-service")).authorities(new SimpleGrantedAuthority("INTERNAL_DOCUMENT_READ"));
    }

    ResultActions deposer(String nom, String type, byte[] contenu, String reference) throws Exception {
        return mvc.perform(
            multipart("/api/v1/documents")
                .file(new MockMultipartFile("fichier", nom, type, contenu))
                .param("finalite", "ADHESION")
                .param("reference", reference)
                .with(agentDdrm())
        );
    }

    int lignes(String reference) {
        return jdbc.queryForObject("select count(*) from depot_document where reference = ?", Integer.class, reference);
    }

    @Test
    @DisplayName("Dépôt d'un texte : 201, empreinte serveur, objet conservé, métadonnées en base, empreinte relue par le service")
    void depotTexte() throws Exception {
        byte[] contenu = "Statuts de la Banque B, capital 10 000 000 XAF".getBytes(StandardCharsets.UTF_8);
        String reference = UUID.randomUUID() + "/STATUTS";

        String corps = deposer("statuts.txt", "text/plain", contenu, reference)
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.sha256").value(EmpreinteSha256.de(contenu)))
            .andExpect(jsonPath("$.nomFichier").value("statuts.txt"))
            .andExpect(jsonPath("$.taille").value(contenu.length))
            .andExpect(jsonPath("$.typeMime").value("text/plain"))
            .andReturn()
            .getResponse()
            .getContentAsString();
        JsonNode r = json.readTree(corps);
        UUID id = UUID.fromString(r.get("id").asText());

        Map<String, Object> ligne = jdbc.queryForMap("select * from depot_document where id = ?", id);
        assertThat(ligne)
            .containsEntry("finalite", "ADHESION")
            .containsEntry("reference", reference)
            .containsEntry("sha256", EmpreinteSha256.de(contenu))
            .containsEntry("deposant", "7c1e0000-0000-0000-0000-00000000dd01")
            .containsEntry("version_objet", "v1");
        assertThat((String) ligne.get("cle_objet")).startsWith("adhesion/").endsWith("/" + id);
        assertThat(stockage.lire((String) ligne.get("cle_objet"))).hasValueSatisfying(b -> assertThat(b).isEqualTo(contenu));

        mvc
            .perform(get("/api/v1/interne/documents/" + id + "/empreinte").with(serviceParticipant()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sha256").value(EmpreinteSha256.de(contenu)));
    }

    @Test
    @DisplayName("Dépôt d'un PDF sain déclaré en octet-stream : 201, type PDF reconnu")
    void depotPdf() throws Exception {
        deposer("convention.pdf", "application/octet-stream", PdfDeTest.avecTexte("Convention d'adhesion"), UUID.randomUUID() + "/CONVENTION")
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.typeMime").value("application/pdf"));
    }

    @Test
    @DisplayName("BNF-16 : texte ou PDF contenant un numéro de carte, 422 DOC_PAN_DETECTED, rien n'est conservé")
    void numeroDeCarte() throws Exception {
        String ref1 = UUID.randomUUID() + "/RIB";
        deposer("rib.txt", "text/plain", "Carte 4111 1111 1111 1111".getBytes(StandardCharsets.UTF_8), ref1)
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("DOC_PAN_DETECTED"))
            .andExpect(jsonPath("$.details.occurrences").value(1));
        String ref2 = UUID.randomUUID() + "/RIB";
        deposer("rib.pdf", "application/pdf", PdfDeTest.avecTexte("Carte 5500000000000004"), ref2)
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("DOC_PAN_DETECTED"));
        assertThat(lignes(ref1) + lignes(ref2)).isZero();
    }

    @Test
    @DisplayName("PDF chiffré : 422 DOC_PDF_UNREADABLE")
    void pdfChiffre() throws Exception {
        deposer("secret.pdf", "application/pdf", PdfDeTest.chiffre("x"), UUID.randomUUID() + "/X")
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("DOC_PDF_UNREADABLE"));
    }

    @Test
    @DisplayName("Fichier vide : 400 DOC_FILE_EMPTY ; au-delà de 20 Mo : 413 DOC_FILE_TOO_LARGE")
    void admission() throws Exception {
        String reference = UUID.randomUUID() + "/X";
        deposer("vide.txt", "text/plain", new byte[0], reference).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("DOC_FILE_EMPTY"));
        deposer("gros.bin", "application/octet-stream", new byte[20 * 1024 * 1024 + 1], reference)
            .andExpect(status().isPayloadTooLarge())
            .andExpect(jsonPath("$.code").value("DOC_FILE_TOO_LARGE"))
            .andExpect(jsonPath("$.details.tailleMax").value(20 * 1024 * 1024));
        mvc
            .perform(multipart("/api/v1/documents").param("finalite", "ADHESION").param("reference", reference).with(agentDdrm()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("DOC_FILE_MISSING"));
        mvc
            .perform(
                multipart("/api/v1/documents")
                    .file(new MockMultipartFile("fichier", "a.txt", "text/plain", new byte[] { 'a' }))
                    .param("reference", reference)
                    .with(agentDdrm())
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("DOC_PURPOSE_INVALID"));
        assertThat(lignes(reference)).isZero();
    }

    @Test
    @DisplayName("Empreinte d'un document inconnu : 404 DOC_NOT_FOUND")
    void empreinteInconnue() throws Exception {
        mvc
            .perform(get("/api/v1/interne/documents/" + UUID.randomUUID() + "/empreinte").with(serviceParticipant()))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("DOC_NOT_FOUND"));
    }

    @Test
    @DisplayName("RG-04, RG-16 : métadonnées en insertion seule, modification et suppression refusées par la base")
    void insertionSeule() throws Exception {
        byte[] contenu = "Registre du commerce".getBytes(StandardCharsets.UTF_8);
        String reference = UUID.randomUUID() + "/RCCM";
        deposer("rccm.txt", "text/plain", contenu, reference).andExpect(status().isCreated());

        assertThatThrownBy(() -> jdbc.update("update depot_document set sha256 = ? where reference = ?", "b".repeat(64), reference)).hasMessageContaining(
            "insertion seule"
        );
        assertThatThrownBy(() -> jdbc.update("delete from depot_document where reference = ?", reference)).hasMessageContaining("insertion seule");
        assertThat(jdbc.queryForObject("select sha256 from depot_document where reference = ?", String.class, reference)).isEqualTo(
            EmpreinteSha256.de(contenu)
        );
    }

    @Test
    @DisplayName("Empreinte au format canonique imposée par la base")
    void contrainteEmpreinte() {
        assertThatThrownBy(() ->
            jdbc.update(
                "insert into depot_document (id, finalite, reference, nom_fichier, taille, type_mime, sha256, cle_objet, deposant, depose_le) " +
                "values (?, 'ADHESION', 'r', 'n', 1, 'text/plain', ?, ?, 'd', now())",
                UUID.randomUUID(),
                "A".repeat(64),
                "k/" + UUID.randomUUID()
            )
        ).hasMessageContaining("ck_depot_document_sha256");
    }
}
