package cm.gimac.des.homologation.web.rest.lot1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cm.gimac.des.homologation.AbstractHomologationIT;
import cm.gimac.des.homologation.domain.enumeration.TypeProjet;
import com.jayway.jsonpath.JsonPath;
import java.time.Duration;
import java.util.UUID;
import org.apache.kafka.clients.consumer.Consumer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.kafka.test.utils.KafkaTestUtils;

class ReferentialPublicationIT extends AbstractHomologationIT {

    private static final UUID INGENIEUR = UUID.fromString("00000000-0000-0000-0000-00000000e001");

    @Test
    @DisplayName("HOM-01 : la publication écrit l'outbox puis l'événement parvient au sujet lu par MS-AUDIT")
    void publicationOutboxPuisSujet() throws Exception {
        UUID versionId = fixtures.brouillonComplet(TypeProjet.PAIEMENT_MOBILE);

        mvc
            .perform(post("/api/v1/referentiels/{id}/publication", versionId).with(utilisateur(INGENIEUR, "HOM_REFERENTIEL_PUBLISH")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.etat").value("PUBLIEE"))
            .andExpect(jsonPath("$.numero").isNumber())
            .andExpect(jsonPath("$.ordreTopologique").value(contains("PM-AMONT-01", "PM-BR-01", "PM-AVAL-01")))
            .andExpect(jsonPath("$.actionsPermises").value(contains("DERIVER", "MIGRER")));

        assertThat(
            jdbc.queryForObject(
                "select count(*) from outbox_event where aggregate_id = ? and event_type = ?",
                Integer.class,
                versionId.toString(),
                "homologation.catalog-published.v1"
            )
        ).isEqualTo(1);

        relayerOutboxJusquaEnvoi(versionId.toString());
        try (Consumer<String, String> c = consommateur("homologation.catalog")) {
            var recus = KafkaTestUtils.getRecords(c, Duration.ofSeconds(20)).records("homologation.catalog");
            assertThat(recus).anySatisfy(r -> {
                assertThat(r.key()).isEqualTo(versionId.toString());
                assertThat(JsonPath.<String>read(r.value(), "$.type")).isEqualTo("homologation.catalog-published.v1");
                assertThat(JsonPath.<String>read(r.value(), "$.specversion")).isEqualTo("1.0");
            });
        }

        mvc
            .perform(
                put("/api/v1/referentiels/{id}/operations/{code}", versionId, "PM-AMONT-01")
                    .with(utilisateur(INGENIEUR, "HOM_REFERENTIEL_EDIT"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(fixtures.operationJson("PM-AMONT-01"))
            )
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("HOM_REFERENTIAL_FROZEN"))
            .andExpect(jsonPath("$.details.brouillonDerivePropose.versionSourceId").value(versionId.toString()));
    }

    @Test
    @DisplayName("HOM-01 : une publication incomplète restitue l'opération et ses attributs manquants")
    void publicationIncomplete() throws Exception {
        UUID versionId = fixtures.brouillonIncomplet(TypeProjet.PAIEMENT_MOBILE, "PM-BR-04");

        mvc
            .perform(post("/api/v1/referentiels/{id}/publication", versionId).with(utilisateur(INGENIEUR, "HOM_REFERENTIEL_PUBLISH")))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("HOM_REFERENTIAL_INCOMPLETE"))
            .andExpect(jsonPath("$.details.operations[0].code").value("PM-BR-04"))
            .andExpect(jsonPath("$.details.operations[0].attributs").value(contains("partie", "preuveAttendue")));
    }

    @Test
    @DisplayName("HOM-01 : publier exige la permission HOM_REFERENTIEL_PUBLISH")
    void publicationSansPermission() throws Exception {
        UUID versionId = fixtures.brouillonComplet(TypeProjet.CARTE_INTERBANCAIRE);
        mvc
            .perform(post("/api/v1/referentiels/{id}/publication", versionId).with(utilisateur(INGENIEUR, "HOM_PORTAL_READ")))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("HOM-01 : la liste sans type rend les versions de tous les types, avec type seulement celles du type")
    void listeAvecEtSansType() throws Exception {
        UUID carte = fixtures.brouillonComplet(TypeProjet.CARTE_INTERBANCAIRE);
        UUID mobile = fixtures.brouillonComplet(TypeProjet.PAIEMENT_MOBILE);

        mvc
            .perform(get("/api/v1/referentiels").with(utilisateur(INGENIEUR, "HOM_REFERENTIEL_READ")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[*].id").value(hasItems(carte.toString(), mobile.toString())));
        mvc
            .perform(get("/api/v1/referentiels").param("type", "PAIEMENT_MOBILE").with(utilisateur(INGENIEUR, "HOM_REFERENTIEL_READ")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[*].typeProjet").value(everyItem(is("PAIEMENT_MOBILE"))))
            .andExpect(jsonPath("$[*].id").value(hasItems(mobile.toString())));
    }
}
