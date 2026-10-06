package cm.gimac.des.homologation.web.rest.lot1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cm.gimac.des.homologation.AbstractHomologationIT;
import cm.gimac.des.homologation.domain.enumeration.TypeProjet;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ReferentialControlIT extends AbstractHomologationIT {

    private static final UUID INGENIEUR = UUID.fromString("00000000-0000-0000-0000-00000000e001");

    @Test
    @DisplayName("HOM-02 : un brouillon cyclique est signalé à blanc puis sa publication est refusée sans outbox")
    void cycleSignalePuisPublicationRefusee() throws Exception {
        UUID versionId = fixtures.brouillonAvecCycle(TypeProjet.DELEGATAIRE, "DL-BR-01", "DL-BR-02", "DL-BR-03");

        mvc
            .perform(post("/api/v1/referentiels/{id}/controle", versionId).with(utilisateur(INGENIEUR, "HOM_REFERENTIEL_EDIT")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.conforme").value(false))
            .andExpect(jsonPath("$.anomalies[0].type").value("CYCLE"))
            .andExpect(jsonPath("$.anomalies[0].codes").value(contains("DL-BR-01", "DL-BR-02", "DL-BR-03", "DL-BR-01")));

        mvc
            .perform(post("/api/v1/referentiels/{id}/publication", versionId).with(utilisateur(INGENIEUR, "HOM_REFERENTIEL_PUBLISH")))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("HOM_REFERENTIAL_GRAPH_INVALID"));

        assertThat(jdbc.queryForObject("select etat from referential_version where id = ?", String.class, versionId)).isEqualTo("BROUILLON");
        assertThat(jdbc.queryForObject("select count(*) from outbox_event where aggregate_id = ?", Integer.class, versionId.toString())).isZero();
    }

    @Test
    @DisplayName("HOM-02 : le graphe par couches alimente DagView")
    void grapheParCouches() throws Exception {
        UUID versionId = fixtures.brouillonComplet(TypeProjet.CARTE_INTERBANCAIRE);

        mvc
            .perform(get("/api/v1/referentiels/{id}/graphe", versionId).with(utilisateur(INGENIEUR, "HOM_REFERENTIEL_READ", "HOM_REFERENTIEL_READ_ALL")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.noeuds.length()").value(3))
            .andExpect(jsonPath("$.noeuds[?(@.code=='CI-AVAL-01')].couche").value(contains(2)))
            .andExpect(jsonPath("$.arcs.length()").value(2))
            .andExpect(jsonPath("$.anomalies").isEmpty());
    }

    @Test
    @DisplayName("HOM-02 : un prédécesseur de branche pour une opération amont est une incohérence de segment")
    void segmentIncoherent() throws Exception {
        UUID versionId = fixtures.brouillonSegmentIncoherent();

        mvc
            .perform(post("/api/v1/referentiels/{id}/controle", versionId).with(utilisateur(INGENIEUR, "HOM_REFERENTIEL_EDIT")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.anomalies[0].type").value("SEGMENT_INCOHERENT"))
            .andExpect(jsonPath("$.anomalies[0].codes").value(contains("DL-AMONT-03", "DL-BR-02")));
    }
}
