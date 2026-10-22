package cm.gimac.des.homologation.web.rest.lot1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cm.gimac.des.homologation.AbstractHomologationIT;
import cm.gimac.des.homologation.application.usecase.ParcoursDepot;
import cm.gimac.des.homologation.contrat.SchemaContrat;
import cm.gimac.des.homologation.domain.cadrage.InstitutionView;
import cm.gimac.des.homologation.domain.enumeration.EtatLiaison;
import cm.gimac.des.homologation.domain.enumeration.TypeProjet;
import cm.gimac.des.homologation.infrastructure.client.CollaboratorStubAdapter;
import cm.gimac.des.homologation.infrastructure.client.ParticipantStubAdapter;
import com.jayway.jsonpath.JsonPath;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * US-EXI-16 sur PostgreSQL et Kafka réels : ouverture d'une liaison de l'institution 99001 publiée avec son code et
 * sa version d'état, puis gel et clôture publiés avec des versions strictement croissantes, enveloppes conformes aux
 * schémas publiés et extension institutionid.
 */
class EtatsLiaisonSandboxIT extends AbstractHomologationIT {

    static final JsonMapper JSON = JsonMapper.builder().build();

    @Autowired
    ParticipantStubAdapter registre;

    @Autowired
    ParcoursDepot depot;

    @Autowired
    TransactionTemplate transaction;

    void changerEtat(String liaison, EtatLiaison etat) {
        transaction.executeWithoutResult(s -> depot.trouver(UUID.fromString(liaison), true).orElseThrow().liaison().setEtat(etat));
    }

    List<String> charges(String liaison) {
        return jdbc.queryForList(
            "select payload::text from outbox_event where aggregate_id = ? and event_type = 'homologation.liaison-status-changed.v1' order by created_at",
            String.class,
            liaison
        );
    }

    @Test
    @DisplayName("CA1, CA2 : ouverture publiée pour la sandbox (code 99001, version 1), gel puis clôture en versions croissantes")
    void ouvertureGelCloture() throws Exception {
        fixtures.referentielPublie(TypeProjet.PAIEMENT_MOBILE);
        UUID institution = UUID.randomUUID();
        registre.enregistrer(new InstitutionView(institution, "Institution 99001", "ADHERENT", "ETABLISSEMENT_PAIEMENT", List.of(new InstitutionView.ServiceSouscrit("PAIEMENT_MOBILE", "2026-09-01"))), "99001");
        String corps = mvc
            .perform(
                post("/api/v1/liaisons")
                    .with(utilisateur(UUID.randomUUID(), "HOM_LIAISON_READ", "HOM_LIAISON_OPEN", "HOM_PROJECT_DESIGNATE"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"institutionId\":\"%s\",\"typeProjet\":\"PAIEMENT_MOBILE\",\"chefDeProjetId\":\"%s\"}".formatted(institution, CollaboratorStubAdapter.CONDUCTEUR_1))
            )
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
        String id = JsonPath.read(corps, "$.id");

        relayerOutboxJusquaEnvoi(id);
        List<JsonNode> recus = new ArrayList<>();
        try (var c = consommateur("homologation.liaison")) {
            for (ConsumerRecord<String, String> r : KafkaTestUtils.getRecords(c, Duration.ofSeconds(20)).records("homologation.liaison")) {
                if (id.equals(r.key())) {
                    recus.add(JSON.readTree(r.value()));
                }
            }
        }
        JsonNode ouverte = recus.stream().filter(e -> e.get("type").asString().equals("homologation.liaison-opened.v1")).findFirst().orElseThrow();
        JsonNode etat = recus.stream().filter(e -> e.get("type").asString().equals("homologation.liaison-status-changed.v1")).findFirst().orElseThrow();
        assertThat(ouverte.get("institutionid").asString()).isEqualTo(institution.toString());
        assertThat(etat.get("institutionid").asString()).isEqualTo(institution.toString());
        assertThat(SchemaContrat.charger("sandbox/homologation.liaison-opened.v1.schema.json").ecarts(ouverte.get("data"))).isEmpty();
        assertThat(SchemaContrat.charger("sandbox/homologation.liaison-status-changed.v1.schema.json").ecarts(etat.get("data"))).isEmpty();
        assertThat(ouverte.at("/data/codeParticipant").asString()).isEqualTo("99001");
        assertThat(ouverte.at("/data/versionEtat").asLong()).isEqualTo(1L);
        assertThat(etat.at("/data/statut").asString()).isEqualTo("OUVERTE");
        assertThat(etat.at("/data/versionEtat").asLong()).isEqualTo(1L);
        assertThat(etat.at("/data/typeProjet").asString()).isEqualTo("PAIEMENT_MOBILE");

        // Écriture sans changement de statut : aucune publication.
        transaction.executeWithoutResult(s -> depot.trouver(UUID.fromString(id), true).orElseThrow());
        assertThat(charges(id)).hasSize(1);

        // Gel : version 2 ; clôture : version 3.
        changerEtat(id, EtatLiaison.GELEE);
        changerEtat(id, EtatLiaison.CLOSE);
        List<String> charges = charges(id);
        assertThat(charges).hasSize(3);
        assertThat(JsonPath.<String>read(charges.get(1), "$.statut")).isEqualTo("GELEE");
        assertThat(JsonPath.<Integer>read(charges.get(1), "$.versionEtat")).isEqualTo(2);
        assertThat(JsonPath.<String>read(charges.get(2), "$.statut")).isEqualTo("CLOSE");
        assertThat(JsonPath.<String>read(charges.get(2), "$.statutPrecedent")).isEqualTo("GELEE");
        assertThat(JsonPath.<Integer>read(charges.get(2), "$.versionEtat")).isEqualTo(3);
        assertThat(jdbc.queryForObject("select version_etat from liaison_sandbox where liaison_id = ?::uuid", Long.class, id)).isEqualTo(3L);
    }
}
