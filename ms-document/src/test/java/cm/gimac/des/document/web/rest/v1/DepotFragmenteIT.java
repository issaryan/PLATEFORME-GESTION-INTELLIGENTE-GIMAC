package cm.gimac.des.document.web.rest.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cm.gimac.des.document.IntegrationTest;
import cm.gimac.des.document.application.port.out.StockageObjet;
import cm.gimac.des.document.application.usecase.VerificationDepotService;
import cm.gimac.des.document.config.S3TestContainer;
import cm.gimac.des.document.domain.service.EmpreinteSha256;
import cm.gimac.des.document.infrastructure.stockage.StockageObjetS3;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

/**
 * Dépôts fragmentés de bout en bout (D-74) : service complet (sécurité, contrôleurs, cas
 * d'utilisation, PostgreSQL et Liquibase, passerelle S3 par Testcontainers), adaptateur S3 réel,
 * compartiment WORM à verrouillage d'objets et compartiment des dépôts en cours. Parties de 5 Mio
 * (minimum S3) ; la vérification est déclenchée directement (pas de planificateur en test).
 */
@IntegrationTest
@AutoConfigureMockMvc
class DepotFragmenteIT {

    static final String WORM = "gimac-documents-fragmente-it";
    static final String DEPOTS = "gimac-depots-fragmente-it";
    static final int PARTIE = 5 * 1024 * 1024;
    static final String AGENT = "7c1e0000-0000-0000-0000-00000000dd74";

    @DynamicPropertySource
    static void stockageReel(DynamicPropertyRegistry registre) {
        try (S3Client client = S3TestContainer.client(WORM)) {
            client.createBucket(b -> b.bucket(WORM).objectLockEnabledForBucket(true));
            client.createBucket(b -> b.bucket(DEPOTS));
        }
        registre.add("gimac.adapters.stockage", () -> "real");
        registre.add("gimac.stockage.s3.point-acces", () -> S3TestContainer.pointAcces().toString());
        registre.add("gimac.stockage.s3.cle-acces", () -> S3TestContainer.CLE_ACCES);
        registre.add("gimac.stockage.s3.cle-secrete", () -> S3TestContainer.CLE_SECRETE);
        registre.add("gimac.stockage.s3.compartiment", () -> WORM);
        registre.add("gimac.stockage.s3.compartiment-depots", () -> DEPOTS);
        registre.add("gimac.stockage.s3.exiger-verrouillage", () -> "true");
        registre.add("gimac.document.depot.taille-partie", () -> "5MB");
    }

    @Autowired
    MockMvc mvc;

    @Autowired
    StockageObjet stockage;

    @Autowired
    VerificationDepotService verification;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    ObjectMapper json;

    static RequestPostProcessor utilisateur(String sujet, String... permissions) {
        return jwt()
            .jwt(j -> j.subject(sujet).claim("acr", "2").claim("azp", "web_app").claim("preferred_username", "agent"))
            .authorities(Arrays.stream(permissions).map(SimpleGrantedAuthority::new).toArray(GrantedAuthority[]::new));
    }

    static RequestPostProcessor service() {
        return jwt()
            .jwt(j -> j.subject(UUID.randomUUID().toString()).claim("azp", "collaboration-service").claim("preferred_username", "service-account-collaboration-service"))
            .authorities(new SimpleGrantedAuthority("INTERNAL_DOCUMENT_READ"));
    }

    /** Fichier de 2 parties pleines et d'un reste, octets pseudo-aléatoires reproductibles. */
    static byte[] fichier(long graine) {
        byte[] octets = new byte[2 * PARTIE + 1234];
        new Random(graine).nextBytes(octets);
        return octets;
    }

    UUID ouvrir(String sujet, byte[] contenu) throws Exception {
        String corps = mvc
            .perform(
                post("/api/v1/depots")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        json.writeValueAsString(
                            Map.of("nomFichier", "atelier.bin", "typeMime", "application/octet-stream", "taille", contenu.length, "finalite", "MESSAGERIE", "reference", "conversation/c1")
                        )
                    )
                    .with(utilisateur(sujet, "COL_MESSAGE_SEND"))
            )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.etat").value("OUVERT"))
            .andExpect(jsonPath("$.taillePartie").value(PARTIE))
            .andExpect(jsonPath("$.nombreParties").value(3))
            .andReturn()
            .getResponse()
            .getContentAsString();
        return UUID.fromString(json.readTree(corps).get("id").asText());
    }

    ResultActions envoyer(String sujet, UUID id, int numero, byte[] contenu) throws Exception {
        byte[] partie = Arrays.copyOfRange(contenu, (numero - 1) * PARTIE, Math.min(numero * PARTIE, contenu.length));
        return mvc.perform(
            put("/api/v1/depots/" + id + "/parties/" + numero)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .content(partie)
                .with(utilisateur(sujet, "COL_MESSAGE_SEND"))
        );
    }

    JsonNode etat(String sujet, UUID id) throws Exception {
        return json.readTree(
            mvc.perform(get("/api/v1/depots/" + id).with(utilisateur(sujet, "COL_MESSAGE_SEND"))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString()
        );
    }

    UUID deposerEtVerifier(String sujet, byte[] contenu) throws Exception {
        UUID id = ouvrir(sujet, contenu);
        for (int n = 1; n <= 3; n++) {
            envoyer(sujet, id, n, contenu).andExpect(status().isNoContent());
        }
        mvc
            .perform(post("/api/v1/depots/" + id + "/fin").with(utilisateur(sujet, "COL_MESSAGE_SEND")))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.etat").value("EN_VERIFICATION"));
        verification.verifierEnAttente();
        return id;
    }

    @Test
    @DisplayName("Envoi par parties (désordre, renvoi), fin, vérification : DISPONIBLE, objet WORM identique, métadonnées et contenu en flux, dépôt supprimé")
    void depotComplet() throws Exception {
        assertThat(stockage).isInstanceOf(StockageObjetS3.class);
        byte[] contenu = fichier(74);
        UUID id = ouvrir(AGENT, contenu);
        envoyer(AGENT, id, 3, contenu).andExpect(status().isNoContent());
        envoyer(AGENT, id, 1, contenu).andExpect(status().isNoContent());
        envoyer(AGENT, id, 1, contenu).andExpect(status().isNoContent());
        assertThat(etat(AGENT, id).get("partiesRecues").toString()).isEqualTo("[1,3]");

        mvc
            .perform(post("/api/v1/depots/" + id + "/fin").with(utilisateur(AGENT, "COL_MESSAGE_SEND")))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("DOC_UPLOAD_INCOMPLETE"))
            .andExpect(jsonPath("$.details.manquantes[0]").value(2));
        envoyer(AGENT, id, 2, contenu).andExpect(status().isNoContent());
        mvc.perform(post("/api/v1/depots/" + id + "/fin").with(utilisateur(AGENT, "COL_MESSAGE_SEND"))).andExpect(status().isAccepted());
        envoyer(AGENT, id, 2, contenu).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("DOC_UPLOAD_CLOSED"));

        verification.verifierEnAttente();

        JsonNode e = etat(AGENT, id);
        String sha = EmpreinteSha256.de(contenu);
        assertThat(e.get("etat").asText()).isEqualTo("DISPONIBLE");
        assertThat(e.get("sha256").asText()).isEqualTo(sha);
        assertThat(e.get("dedoublonne").asBoolean()).isFalse();
        UUID document = UUID.fromString(e.get("documentId").asText());

        Map<String, Object> ligne = jdbc.queryForMap("select * from depot_document where id = ?", document);
        assertThat(ligne).containsEntry("finalite", "MESSAGERIE").containsEntry("deposant", AGENT).containsEntry("sha256", sha);
        assertThat((String) ligne.get("cle_objet")).startsWith("messagerie/").endsWith("/" + document);
        assertThat(jdbc.queryForObject("select count(*) from depot_fragmente_partie where depot_id = ?", Integer.class, id)).isEqualTo(3);

        mvc
            .perform(get("/api/v1/interne/documents/" + document).with(service()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.taille").value(contenu.length))
            .andExpect(jsonPath("$.sha256").value(sha))
            .andExpect(jsonPath("$.finalite").value("MESSAGERIE"))
            .andExpect(jsonPath("$.deposant").value(AGENT));
        byte[] lu = mvc
            .perform(get("/api/v1/interne/documents/" + document + "/contenu").with(service()))
            .andExpect(status().isOk())
            .andExpect(header().longValue("Content-Length", contenu.length))
            .andExpect(header().string("Repr-Digest", "sha-256=:" + Base64.getEncoder().encodeToString(java.util.HexFormat.of().parseHex(sha)) + ":"))
            .andReturn()
            .getResponse()
            .getContentAsByteArray();
        assertThat(lu).isEqualTo(contenu);

        try (S3Client client = S3TestContainer.client(DEPOTS)) {
            assertThatThrownBy(() -> client.headObject(b -> b.bucket(DEPOTS).key(id.toString()))).isInstanceOf(NoSuchKeyException.class);
        }
    }

    @Test
    @DisplayName("D-87 : livrable de processus (PROCESSUS_UNITE, PIL_ACTIVITY_OWN, référence = instance) relu par MS-PILOTAGE (jeton pilotage-service) : finalité, référence, déposant, type, empreinte")
    void livrableProcessus() throws Exception {
        byte[] contenu = fichier(77);
        String instance = UUID.randomUUID().toString();
        String ouverture = json.writeValueAsString(
            Map.of("nomFichier", "configuration.bin", "typeMime", "application/octet-stream", "taille", contenu.length, "finalite", "PROCESSUS_UNITE", "reference", instance)
        );
        mvc
            .perform(post("/api/v1/depots").contentType(MediaType.APPLICATION_JSON).content(ouverture).with(utilisateur(AGENT, "COL_MESSAGE_SEND", "DOC_DEPOSIT")))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("DOC_PURPOSE_FORBIDDEN"));
        String corps = mvc
            .perform(post("/api/v1/depots").contentType(MediaType.APPLICATION_JSON).content(ouverture).with(utilisateur(AGENT, "PIL_ACTIVITY_OWN")))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.etat").value("OUVERT"))
            .andReturn()
            .getResponse()
            .getContentAsString();
        UUID id = UUID.fromString(json.readTree(corps).get("id").asText());
        for (int n = 1; n <= 3; n++) {
            byte[] partie = Arrays.copyOfRange(contenu, (n - 1) * PARTIE, Math.min(n * PARTIE, contenu.length));
            mvc
                .perform(put("/api/v1/depots/" + id + "/parties/" + n).contentType(MediaType.APPLICATION_OCTET_STREAM).content(partie).with(utilisateur(AGENT, "PIL_ACTIVITY_OWN")))
                .andExpect(status().isNoContent());
        }
        mvc.perform(post("/api/v1/depots/" + id + "/fin").with(utilisateur(AGENT, "PIL_ACTIVITY_OWN"))).andExpect(status().isAccepted());
        verification.verifierEnAttente();
        JsonNode e = json.readTree(
            mvc.perform(get("/api/v1/depots/" + id).with(utilisateur(AGENT, "PIL_ACTIVITY_OWN"))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString()
        );
        assertThat(e.get("etat").asText()).isEqualTo("DISPONIBLE");
        String document = e.get("documentId").asText();
        RequestPostProcessor pilotage = jwt()
            .jwt(j -> j.subject(UUID.randomUUID().toString()).claim("azp", "pilotage-service").claim("preferred_username", "service-account-pilotage-service"))
            .authorities(new SimpleGrantedAuthority("INTERNAL_DOCUMENT_READ"));
        mvc
            .perform(get("/api/v1/interne/documents/" + document).with(pilotage))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(document))
            .andExpect(jsonPath("$.nomFichier").value("configuration.bin"))
            .andExpect(jsonPath("$.taille").value(contenu.length))
            .andExpect(jsonPath("$.typeMime").value("application/octet-stream"))
            .andExpect(jsonPath("$.sha256").value(EmpreinteSha256.de(contenu)))
            .andExpect(jsonPath("$.finalite").value("PROCESSUS_UNITE"))
            .andExpect(jsonPath("$.reference").value(instance))
            .andExpect(jsonPath("$.deposant").value(AGENT));
    }

    @Test
    @DisplayName("Déduplication : même contenu par un autre compte, nouveau document vers le même objet WORM (index simple sur cle_objet)")
    void deduplication() throws Exception {
        byte[] contenu = fichier(75);
        UUID premier = deposerEtVerifier(AGENT, contenu);
        UUID second = deposerEtVerifier("autre-agent", contenu);

        JsonNode a = etat(AGENT, premier);
        JsonNode b = etat("autre-agent", second);
        assertThat(b.get("etat").asText()).isEqualTo("DISPONIBLE");
        assertThat(b.get("dedoublonne").asBoolean()).isTrue();
        assertThat(b.get("documentId").asText()).isNotEqualTo(a.get("documentId").asText());
        String cleA = jdbc.queryForObject("select cle_objet from depot_document where id = ?", String.class, UUID.fromString(a.get("documentId").asText()));
        String cleB = jdbc.queryForObject("select cle_objet from depot_document where id = ?", String.class, UUID.fromString(b.get("documentId").asText()));
        assertThat(cleB).isEqualTo(cleA);
    }

    @Test
    @DisplayName("Numéro de carte dans un petit fichier texte : REJETE DOC_PAN_DETECTED, aucun document")
    void numeroDeCarte() throws Exception {
        byte[] contenu = "Carte du client : 4111 1111 1111 1111".getBytes(StandardCharsets.UTF_8);
        String corps = mvc
            .perform(
                post("/api/v1/depots")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        json.writeValueAsString(
                            Map.of("nomFichier", "note.txt", "typeMime", "text/plain", "taille", contenu.length, "finalite", "MESSAGERIE", "reference", "conversation/c2")
                        )
                    )
                    .with(utilisateur(AGENT, "COL_MESSAGE_SEND"))
            )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.nombreParties").value(1))
            .andReturn()
            .getResponse()
            .getContentAsString();
        UUID id = UUID.fromString(json.readTree(corps).get("id").asText());
        mvc
            .perform(
                put("/api/v1/depots/" + id + "/parties/1")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .content(contenu)
                    .with(utilisateur(AGENT, "COL_MESSAGE_SEND"))
            )
            .andExpect(status().isNoContent());
        mvc.perform(post("/api/v1/depots/" + id + "/fin").with(utilisateur(AGENT, "COL_MESSAGE_SEND"))).andExpect(status().isAccepted());
        verification.verifierEnAttente();

        JsonNode e = etat(AGENT, id);
        assertThat(e.get("etat").asText()).isEqualTo("REJETE");
        assertThat(e.get("motif").asText()).isEqualTo("DOC_PAN_DETECTED");
        assertThat(e.has("documentId")).isFalse();
    }

    @Test
    @DisplayName("Accès : dépôt d'un autre compte inconnu (404) ; abandon, puis partie refusée (409) ; finalité sans permission (403)")
    void acces() throws Exception {
        byte[] contenu = fichier(76);
        UUID id = ouvrir(AGENT, contenu);
        mvc.perform(get("/api/v1/depots/" + id).with(utilisateur("autre-agent", "COL_MESSAGE_SEND"))).andExpect(status().isNotFound());
        envoyer("autre-agent", id, 1, contenu).andExpect(status().isNotFound());
        mvc.perform(delete("/api/v1/depots/" + id).with(utilisateur(AGENT, "COL_MESSAGE_SEND"))).andExpect(status().isNoContent());
        envoyer(AGENT, id, 1, contenu).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("DOC_UPLOAD_CLOSED"));
        assertThat(etat(AGENT, id).get("etat").asText()).isEqualTo("ABANDONNE");

        mvc
            .perform(
                post("/api/v1/depots")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"nomFichier\":\"a\",\"typeMime\":\"text/plain\",\"taille\":1,\"finalite\":\"ADHESION\",\"reference\":\"r\"}")
                    .with(utilisateur(AGENT, "COL_MESSAGE_SEND"))
            )
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("DOC_PURPOSE_FORBIDDEN"));
    }
}
