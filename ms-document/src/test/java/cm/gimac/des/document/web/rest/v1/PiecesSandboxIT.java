package cm.gimac.des.document.web.rest.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cm.gimac.des.document.IntegrationTest;
import cm.gimac.des.document.domain.service.EmpreinteSha256;
import cm.gimac.des.document.infrastructure.sandbox.RelaisEvenementsSandbox;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Pièces de la sandbox de bout en bout dans MS-DOCUMENT (US-EXI-08) : sécurité, cas d'utilisation, PostgreSQL, Kafka
 * par Testcontainers ; dépôt d'un bilan et réception de document.document-sealed.v1, déduplication, empreinte
 * divergente, quarantaine, catalogue depuis la console.
 */
@IntegrationTest
@AutoConfigureMockMvc
class PiecesSandboxIT {

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    JdbcClient jdbcClient;

    @Autowired
    KafkaTemplate<String, String> kafka;

    @Autowired
    PlatformTransactionManager tm;

    @Autowired
    Environment env;

    @Autowired
    cm.gimac.des.document.application.port.out.PiecesSandbox pieces;

    static RequestPostProcessor service(String client, String... permissions) {
        return jwt()
            .jwt(j -> j.subject(UUID.randomUUID().toString()).claim("azp", client).claim("preferred_username", "service-account-" + client))
            .authorities(java.util.Arrays.stream(permissions).map(SimpleGrantedAuthority::new).toArray(GrantedAuthority[]::new));
    }

    static RequestPostProcessor agent(String... permissions) {
        return jwt()
            .jwt(j -> j.subject("rssi").claim("acr", "2").claim("azp", "web_app"))
            .authorities(java.util.Arrays.stream(permissions).map(SimpleGrantedAuthority::new).toArray(GrantedAuthority[]::new));
    }

    ResultActions deposer(String client, String type, byte[] contenu, String empreinte, String code) throws Exception {
        return mvc.perform(
            multipart("/api/v1/interne/pieces-sandbox")
                .file(new MockMultipartFile("fichier", "piece.txt", "text/plain", contenu))
                .param("type", type)
                .param("empreinte", empreinte)
                .param("codeParticipant", code)
                .param("reference", "campagne/" + UUID.randomUUID())
                .with(service(client, "INTERNAL_DOCUMENT_DEPOSIT"))
        );
    }

    @Test
    @DisplayName("Intégration, critère 1 : dépôt d'un bilan scellé et réception de document.document-sealed.v1 d'empreinte égale")
    void depotDuBilanFige() throws Exception {
        byte[] bilan = ("Bilan fige " + UUID.randomUUID()).getBytes(StandardCharsets.UTF_8);
        String sha = EmpreinteSha256.de(bilan);
        String id = com.jayway.jsonpath.JsonPath.read(
            deposer("sbx-campagne", "SBX_BILAN_CERTIFICATION", bilan, sha, "99001")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.etat").value("SCELLEE"))
                .andExpect(jsonPath("$.sha256").value(sha))
                .andReturn()
                .getResponse()
                .getContentAsString(),
            "$.id"
        );
        assertThat(jdbc.queryForObject("select finalite from depot_document where id = ?", String.class, UUID.fromString(id))).isEqualTo("SBX_BILAN_CERTIFICATION");
        RelaisEvenementsSandbox relais = new RelaisEvenementsSandbox(jdbcClient, kafka, new TransactionTemplate(tm), Clock.systemUTC());
        assertThat(relais.relayer()).isGreaterThanOrEqualTo(1);
        try (KafkaConsumer<String, String> c = consommateur()) {
            c.subscribe(List.of("document.document"));
            ConsumerRecord<String, String> trouve = null;
            long fin = System.nanoTime() + Duration.ofSeconds(30).toNanos();
            while (trouve == null && System.nanoTime() < fin) {
                for (ConsumerRecord<String, String> r : c.poll(Duration.ofMillis(500))) {
                    if (id.equals(r.key())) {
                        trouve = r;
                    }
                }
            }
            assertThat(trouve).isNotNull();
            assertThat(trouve.value()).contains("\"type\":\"document.document-sealed.v1\"", "\"sha256\":\"" + sha + "\"", "\"codeParticipant\":\"99001\"");
        }
        assertThat(jdbc.queryForObject("select statut from sbx_evenement_sortant where cle = ?", String.class, id)).isEqualTo("ENVOYE");
    }

    KafkaConsumer<String, String> consommateur() {
        return new KafkaConsumer<>(
            Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                env.getProperty("spring.cloud.stream.kafka.binder.brokers", "localhost:9092"),
                ConsumerConfig.GROUP_ID_CONFIG,
                "it-" + UUID.randomUUID(),
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                "earliest"
            ),
            new StringDeserializer(),
            new StringDeserializer()
        );
    }

    @Test
    @DisplayName("Critères 2, 3 et 4 : déduplication, empreinte divergente sans dépôt, quarantaine d'une trace portant un numéro de carte")
    void dedoublonnageDivergenceQuarantaine() throws Exception {
        byte[] trace = ("trace " + UUID.randomUUID()).getBytes(StandardCharsets.UTF_8);
        String sha = EmpreinteSha256.de(trace);
        String premier = deposer("sbx-trace", "SBX_TRACE_EXPORTEE", trace, sha, "99001").andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String second = deposer("sbx-trace", "SBX_TRACE_EXPORTEE", trace, sha, "99001")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.dejaConservee").value(true))
            .andReturn()
            .getResponse()
            .getContentAsString();
        assertThat((String) com.jayway.jsonpath.JsonPath.read(second, "$.id")).isEqualTo(com.jayway.jsonpath.JsonPath.read(premier, "$.id"));
        assertThat(jdbc.queryForObject("select count(*) from depot_document where sha256 = ?", Integer.class, sha)).isEqualTo(1);

        byte[] rapport = ("rapport " + UUID.randomUUID()).getBytes(StandardCharsets.UTF_8);
        deposer("sbx-iso20022", "SBX_RAPPORT_ECARTS_ISO20022", rapport, "0".repeat(64), "99001")
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("DOC_SBX_EMPREINTE_DIVERGENTE"));
        assertThat(jdbc.queryForObject("select count(*) from depot_document where sha256 = ?", Integer.class, EmpreinteSha256.de(rapport))).isZero();

        byte[] pan = ("carte 4970 1012 3456 7893 " + UUID.randomUUID()).getBytes(StandardCharsets.UTF_8);
        deposer("sbx-trace", "SBX_TRACE_EXPORTEE", pan, EmpreinteSha256.de(pan), "99001")
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.etat").value("QUARANTAINE"));
        assertThat(jdbc.queryForObject("select count(*) from depot_document where sha256 = ?", Integer.class, EmpreinteSha256.de(pan))).isZero();
        assertThat(jdbc.queryForObject("select count(*) from sbx_piece where sha256 = ? and etat = 'QUARANTAINE'", Integer.class, EmpreinteSha256.de(pan))).isEqualTo(1);
        // Supervision des quarantaines : la pièce est listée, sans fin de conservation.
        mvc
            .perform(get("/api/v1/sandbox/quarantaines?taille=100").with(agent("SBX_TRA_CONSULTATION")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.contenu[?(@.sha256 == '" + EmpreinteSha256.de(pan) + "')].occurrencesPan").value(1));
        // Modification concurrente vue par l'adaptateur : version lue périmée, aucune ligne modifiée.
        var type = pieces.type("SBX_TRACE_EXPORTEE").orElseThrow();
        Boolean modifie = new TransactionTemplate(tm).execute(s -> pieces.modifierRetention(type.avecRetention(java.time.Period.ofYears(11)), type.version() + 5, "rssi", "m", java.time.Instant.now()));
        assertThat(modifie).isFalse();

        // Déposant non admis et jeton utilisateur refusés.
        deposer("sbx-trace", "SBX_BILAN_CERTIFICATION", trace, sha, "99001").andExpect(status().isForbidden());
        mvc.perform(multipart("/api/v1/interne/pieces-sandbox").file(new MockMultipartFile("fichier", "a.txt", "text/plain", trace)).with(agent("INTERNAL_DOCUMENT_DEPOSIT"))).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Pièce déjà conservée par empreinte (contrat de MS-HOMOLOGATION) : 200, 404, 422, jeton de lecture exigé")
    void parEmpreinte() throws Exception {
        byte[] bilan = ("bilan " + UUID.randomUUID()).getBytes(StandardCharsets.UTF_8);
        String sha = EmpreinteSha256.de(bilan);
        String id = com.jayway.jsonpath.JsonPath.read(
            deposer("sbx-campagne", "SBX_BILAN_CERTIFICATION", bilan, sha, "99001").andReturn().getResponse().getContentAsString(),
            "$.id"
        );
        mvc
            .perform(get("/api/v1/interne/documents/empreintes/" + sha).with(service("homologation-service", "INTERNAL_DOCUMENT_READ")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(id))
            .andExpect(jsonPath("$.nomFichier").value("piece.txt"))
            .andExpect(jsonPath("$.typeMime").exists())
            .andExpect(jsonPath("$.sha256").value(sha));
        mvc.perform(get("/api/v1/interne/documents/empreintes/" + "d".repeat(64)).with(service("homologation-service", "INTERNAL_DOCUMENT_READ"))).andExpect(status().isNotFound());
        mvc
            .perform(get("/api/v1/interne/documents/empreintes/" + sha.toUpperCase()).with(service("homologation-service", "INTERNAL_DOCUMENT_READ")))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("DOC_SHA256_INVALIDE"));
        mvc.perform(get("/api/v1/interne/documents/empreintes/" + sha).with(service("homologation-service", "INTERNAL_DOCUMENT_DEPOSIT"))).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/interne/documents/empreintes/" + sha).with(agent("INTERNAL_DOCUMENT_READ"))).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Console : types de pièces et rétention modifiée, quarantaines ; permissions SBX_TRA_*")
    void console() throws Exception {
        mvc
            .perform(get("/api/v1/sandbox/types-pieces").with(agent("SBX_TRA_CONSULTATION")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(4))
            .andExpect(jsonPath("$[0].code").value("SBX_BILAN_CERTIFICATION"))
            .andExpect(jsonPath("$[0].libelles.es").value("Balance de certificación"));
        int version = jdbc.queryForObject("select version from sbx_type_piece where code = 'SBX_FICHIER_REMIS_PARTICIPANT'", Integer.class);
        mvc
            .perform(
                put("/api/v1/sandbox/types-pieces/SBX_FICHIER_REMIS_PARTICIPANT/retention")
                    .with(agent("SBX_TRA_ADMINISTRATION"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"retention\":\"P6Y\",\"motif\":\"Durée contractuelle\",\"version\":" + version + "}")
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.retention").value("P6Y"))
            .andExpect(jsonPath("$.version").value(version + 1));
        mvc
            .perform(
                put("/api/v1/sandbox/types-pieces/SBX_BILAN_CERTIFICATION/retention")
                    .with(agent("SBX_TRA_ADMINISTRATION"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"retention\":\"P5Y\",\"motif\":\"m\",\"version\":1}")
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("DOC_SBX_RETENTION_INSUFFISANTE"));
        mvc.perform(get("/api/v1/sandbox/quarantaines").with(agent("SBX_TRA_CONSULTATION"))).andExpect(status().isOk()).andExpect(jsonPath("$.taille").value(20));
        mvc.perform(get("/api/v1/sandbox/types-pieces").with(agent("DOC_DEPOSIT"))).andExpect(status().isForbidden());
    }
}
