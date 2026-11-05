package cm.gimac.des.homologation.web.rest.lot1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cm.gimac.des.homologation.AbstractHomologationIT;
import cm.gimac.des.homologation.domain.cadrage.InstitutionView;
import cm.gimac.des.homologation.domain.enumeration.TypeProjet;
import cm.gimac.des.homologation.infrastructure.client.ParticipantStubAdapter;
import com.jayway.jsonpath.JsonPath;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Clôture sans suite des liaisons reprises (D-137) sur PostgreSQL réel : simulation sans écriture, clôture tracée
 * (statut, état, motif, événements), rejeu inchangé, refus d'une liaison en production. Données fictives.
 */
class RepriseCloturesIT extends AbstractHomologationIT {

    static final ObjectMapper JSON = JsonMapper.builder().build();
    static final String MOTIF = "Liaison par défaut de la reprise du 13/10/2026 sans projet réel";

    @Autowired
    ParticipantStubAdapter registre;

    @BeforeEach
    void preparer() throws Exception {
        fixtures.referentielPublie(TypeProjet.PAIEMENT_MOBILE);
        registre.enregistrer(new InstitutionView(UUID.randomUUID(), "Banque fictive 99001", "ACTIF", "BANQUE", List.of()), "99001");
        Map<String, Object> corps = Map.of(
            "simulation", false,
            "liaisons", List.of(
                Map.of("referenceOrigine", "REPRISE-99001-MOBILE", "codeMembre", "99001", "typeProjet", "PAIEMENT_MOBILE", "origineEtat", "PayWay test"),
                Map.of("referenceOrigine", "REPRISE-99001-PROD", "codeMembre", "99001", "typeProjet", "PAIEMENT_MOBILE", "avancement", "EN_PRODUCTION", "dateMiseEnProduction", "2025-11-30")
            )
        );
        mvc.perform(post("/api/v1/interne/reprise/liaisons").with(RepriseLiaisonsIT.compteReprise()).contentType(MediaType.APPLICATION_JSON).content(JSON.writeValueAsString(corps)))
            .andExpect(status().isOk());
    }

    ResultActions clore(Boolean simulation, String... references) throws Exception {
        Map<String, Object> corps = new LinkedHashMap<>();
        if (simulation != null) {
            corps.put("simulation", simulation);
        }
        corps.put("motif", MOTIF);
        corps.put("liaisons", List.of(references).stream().map(r -> Map.of("referenceOrigine", r)).toList());
        return mvc.perform(
            post("/api/v1/interne/reprise/liaisons/clotures").with(RepriseLiaisonsIT.compteReprise()).contentType(MediaType.APPLICATION_JSON).content(JSON.writeValueAsString(corps))
        );
    }

    String statut(String ref) {
        return jdbc.queryForObject("select p.statut from parcours_liaison p join liaison l on l.id = p.liaison_id where l.reference_origine = ?", String.class, ref);
    }

    @Test
    @DisplayName("Simulation par défaut : rapport CLOSE, rien n'est écrit")
    void simulation() throws Exception {
        clore(null, "REPRISE-99001-MOBILE").andExpect(status().isOk()).andExpect(jsonPath("$.simulation").value(true)).andExpect(jsonPath("$.lignes[0].resultat").value("CLOSE"));
        assertThat(statut("REPRISE-99001-MOBILE")).isNotEqualTo("CLOSE_SANS_SUITE");
    }

    @Test
    @DisplayName("Clôture réelle tracée, rejeu inchangé, liaison en production refusée, aucune suppression")
    void clotureReelle() throws Exception {
        long liaisonsAvant = jdbc.queryForObject("select count(*) from liaison", Long.class);
        String id = JsonPath.read(
            clore(false, "REPRISE-99001-MOBILE", "REPRISE-99001-PROD")
                .andExpect(jsonPath("$.lignes[0].resultat").value("CLOSE"))
                .andExpect(jsonPath("$.lignes[1].code").value("HOM_REPRISE_CLOTURE_EN_PRODUCTION"))
                .andReturn()
                .getResponse()
                .getContentAsString(),
            "$.lignes[0].liaisonId"
        );
        assertThat(statut("REPRISE-99001-MOBILE")).isEqualTo("CLOSE_SANS_SUITE");
        Map<String, Object> l = jdbc.queryForMap("select etat, motif_etat from liaison where id = ?::uuid", id);
        assertThat(l.get("etat")).isEqualTo("CLOSE");
        assertThat((String) l.get("motif_etat")).isEqualTo("Close sans suite par la reprise de l'existant : " + MOTIF);
        assertThat(jdbc.queryForList("select event_type from outbox_event where aggregate_id = ?", String.class, id)).contains("homologation.liaison-closed-without-follow-up.v1");
        clore(false, "REPRISE-99001-MOBILE").andExpect(jsonPath("$.lignes[0].resultat").value("INCHANGEE"));
        assertThat(jdbc.queryForObject("select count(*) from liaison", Long.class)).isEqualTo(liaisonsAvant);
        assertThat(statut("REPRISE-99001-PROD")).isEqualTo("EN_PRODUCTION");
    }
}
