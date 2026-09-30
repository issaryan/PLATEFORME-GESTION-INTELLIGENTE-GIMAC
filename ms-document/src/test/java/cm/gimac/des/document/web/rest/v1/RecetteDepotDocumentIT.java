package cm.gimac.des.document.web.rest.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cm.gimac.des.document.IntegrationTest;
import cm.gimac.des.document.application.port.out.StockageObjet;
import cm.gimac.des.document.config.S3TestContainer;
import cm.gimac.des.document.domain.service.EmpreinteSha256;
import cm.gimac.des.document.infrastructure.stockage.StockageObjetS3;
import cm.gimac.des.document.infrastructure.texte.PdfDeTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * Recette du dépôt d'une pièce d'adhésion (tranche DDRM, étape 2 du parcours), en scénario :
 * MS-DOCUMENT n'a pas de Cucumber, ce test d'intégration en tient lieu (D-61). Service complet
 * avec l'adaptateur S3 réel (gimac.adapters.stockage=real) face à une passerelle S3 Testcontainers.
 *
 * Scénario : dépôt d'une pièce d'adhésion par la DDRM
 *   Étant donné un agent de la DDRM authentifié à deux facteurs, porteur de PAR_ADHESION_INSTRUCT
 *   Quand il dépose les statuts (PDF) de l'institution candidate pour la finalité ADHESION
 *   Alors MS-DOCUMENT répond 201 avec l'identifiant et l'empreinte SHA-256 du fichier
 *   Et le contenu est conservé à l'identique dans le compartiment WORM, empreinte en métadonnée
 *   Et MS-PARTICIPANT, par son jeton de service, relit la même empreinte sur l'interface interne
 *   Mais l'agent ne peut pas lire lui-même l'interface interne (403)
 */
@IntegrationTest
@AutoConfigureMockMvc
class RecetteDepotDocumentIT {

    static final String COMPARTIMENT = "gimac-documents-recette";

    @DynamicPropertySource
    static void stockageReel(DynamicPropertyRegistry registre) {
        try (S3Client client = S3TestContainer.client(COMPARTIMENT)) {
            client.createBucket(b -> b.bucket(COMPARTIMENT));
        }
        registre.add("gimac.adapters.stockage", () -> "real");
        registre.add("gimac.stockage.s3.point-acces", () -> S3TestContainer.pointAcces().toString());
        registre.add("gimac.stockage.s3.cle-acces", () -> S3TestContainer.CLE_ACCES);
        registre.add("gimac.stockage.s3.cle-secrete", () -> S3TestContainer.CLE_SECRETE);
        registre.add("gimac.stockage.s3.compartiment", () -> COMPARTIMENT);
    }

    @Autowired
    MockMvc mvc;

    @Autowired
    StockageObjet stockage;

    @Autowired
    S3Client s3;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    ObjectMapper json;

    @Test
    @DisplayName("Dépôt d'une pièce d'adhésion par la DDRM, vérification de l'empreinte par MS-PARTICIPANT")
    void depotPieceAdhesion() throws Exception {
        assertThat(stockage).isInstanceOf(StockageObjetS3.class);

        // Étant donné un agent de la DDRM authentifié à deux facteurs
        var agent = jwt()
            .jwt(j -> j.subject("7c1e0000-0000-0000-0000-00000000dd01").claim("acr", "2"))
            .authorities(new SimpleGrantedAuthority("PAR_ADHESION_INSTRUCT"));
        UUID institution = UUID.randomUUID();
        byte[] statuts = PdfDeTest.avecTexte("Statuts de la Banque B - article 1 : denomination");

        // Quand il dépose les statuts pour la finalité ADHESION
        String corps = mvc
            .perform(
                multipart("/api/v1/documents")
                    .file(new MockMultipartFile("fichier", "statuts.pdf", "application/pdf", statuts))
                    .param("finalite", "ADHESION")
                    .param("reference", institution + "/STATUTS")
                    .with(agent)
            )
            // Alors MS-DOCUMENT répond 201 avec l'identifiant et l'empreinte
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.sha256").value(EmpreinteSha256.de(statuts)))
            .andExpect(jsonPath("$.typeMime").value("application/pdf"))
            .andReturn()
            .getResponse()
            .getContentAsString();
        JsonNode reponse = json.readTree(corps);
        UUID id = UUID.fromString(reponse.get("id").asText());

        // Et le contenu est conservé à l'identique dans le compartiment WORM, empreinte en métadonnée
        String cle = jdbc.queryForObject("select cle_objet from depot_document where id = ?", String.class, id);
        assertThat(stockage.lire(cle)).hasValueSatisfying(b -> assertThat(b).isEqualTo(statuts));
        assertThat(s3.headObject(b -> b.bucket(COMPARTIMENT).key(cle)).metadata()).containsEntry("sha256", EmpreinteSha256.de(statuts));

        // Et MS-PARTICIPANT, par son jeton de service, relit la même empreinte
        mvc
            .perform(
                get("/api/v1/interne/documents/" + id + "/empreinte").with(
                    jwt().jwt(j -> j.subject("service-account-participant").claim("azp", "participant-service").claim("preferred_username", "service-account-participant-service")).authorities(new SimpleGrantedAuthority("INTERNAL_DOCUMENT_READ"))
                )
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sha256").value(reponse.get("sha256").asText()));

        // Mais l'agent ne peut pas lire lui-même l'interface interne
        mvc.perform(get("/api/v1/interne/documents/" + id + "/empreinte").with(agent)).andExpect(status().isForbidden());
    }
}
