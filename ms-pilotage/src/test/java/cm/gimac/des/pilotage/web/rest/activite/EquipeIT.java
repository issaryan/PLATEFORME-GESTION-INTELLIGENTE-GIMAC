package cm.gimac.des.pilotage.web.rest.activite;

import static cm.gimac.des.pilotage.infrastructure.client.OrganisationStubAdapter.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.pilotage.AbstractPilotageIT;
import cm.gimac.des.pilotage.RelaisOutboxDeTest;
import cm.gimac.des.pilotage.domain.service.ReglesActivite;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

class EquipeIT extends AbstractPilotageIT {

    @Autowired
    private RelaisOutboxDeTest relais;

    private MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder r, Object corps) throws Exception {
        return r.contentType(MediaType.APPLICATION_JSON).content(corps(corps));
    }

    private void activite(String auteur, LocalDate jour, String statut) throws Exception {
        String id = lire(mvc.perform(json(post("/api/v1/activites").with(agent(auteur)), Map.of("jour", jour.toString(), "titre", "A"))).andExpect(status().isCreated()).andReturn())
            .get("id")
            .asText();
        if (statut != null) {
            mvc.perform(json(post("/api/v1/activites/" + id + "/statut").with(agent(auteur)), Map.of("statut", statut))).andExpect(status().isOk());
        }
    }

    @Test
    @DisplayName("Vue d'équipe du jour (PIL_TEAM_READ) : activités, rapport du jour, tâches ouvertes et en retard, absence")
    void equipe() throws Exception {
        LocalDate jour = aujourdhui();
        activite(AGENT, jour, null);
        activite(AGENT, jour, "FAITE");
        activite(AGENT, jour, "EN_COURS");
        activite(AGENT, jour, "ANNULEE");
        Map<String, Object> t = new HashMap<>();
        t.put("titre", "En retard");
        t.put("echeance", jour.minusDays(2).toString());
        t.put("responsableId", AGENT);
        mvc.perform(json(post("/api/v1/taches").with(responsable(CHEF_SERVICE)), t)).andExpect(status().isCreated());
        mvc
            .perform(json(post("/api/v1/rapports").with(agent(AGENT)), Map.of("type", "QUOTIDIEN", "periode", jour.toString(), "mode", "REDIGE", "contenu", "x")))
            .andExpect(status().isCreated());
        Map<String, Object> absence = new HashMap<>();
        absence.put("titre", "Congé");
        absence.put("categorie", "ABSENCE");
        absence.put("journeeEntiere", true);
        absence.put("debut", ReglesActivite.horodatage(ReglesActivite.debutJour(jour)));
        absence.put("fin", ReglesActivite.horodatage(ReglesActivite.debutJour(jour.plusDays(1))));
        mvc.perform(json(post("/api/v1/agenda").with(agent(AGENT_BIS)), absence)).andExpect(status().isCreated());

        var membres = lire(mvc.perform(get("/api/v1/equipe").with(responsable(CHEF_SERVICE))).andExpect(status().isOk()).andReturn());
        assertThat(membres.size()).isEqualTo(3);
        var agent = membre(membres, AGENT);
        assertThat(agent.at("/activites/prevues").asInt()).isEqualTo(3);
        assertThat(agent.at("/activites/faites").asInt()).isEqualTo(1);
        assertThat(agent.at("/activites/enCours").asInt()).isEqualTo(1);
        assertThat(agent.at("/rapportDuJour").asText()).isEqualTo("BROUILLON");
        assertThat(agent.at("/tachesOuvertes").asInt()).isEqualTo(1);
        assertThat(agent.at("/tachesEnRetard").asInt()).isEqualTo(1);
        assertThat(agent.at("/absent").asBoolean()).isFalse();
        assertThat(agent.at("/collaborateur/niveau").asInt()).isEqualTo(1);
        assertThat(agent.at("/collaborateur/unite").asText()).isEqualTo("SNA");
        assertThat(agent.at("/collaborateur/nomAffiche").asText()).isEqualTo("Serge Ndzana");
        var bis = membre(membres, AGENT_BIS);
        assertThat(bis.at("/absent").asBoolean()).isTrue();
        assertThat(bis.at("/rapportDuJour").asText()).isEqualTo("ABSENT");
        mvc.perform(get("/api/v1/equipe").param("jour", jour.toString()).with(responsable(CHEF_DES))).andExpect(jsonPath("$.length()").value(4));
        mvc.perform(get("/api/v1/equipe").with(responsable(AGENT))).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/v1/equipe").with(agent(CHEF_SERVICE))).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/equipe").param("jour", "06/10/2026").with(responsable(CHEF_SERVICE))).andExpect(status().isBadRequest());
    }

    private static com.fasterxml.jackson.databind.JsonNode membre(com.fasterxml.jackson.databind.JsonNode membres, String id) {
        for (var m : membres) {
            if (id.equals(m.at("/collaborateur/id").asText())) {
                return m;
            }
        }
        throw new AssertionError("Membre absent : " + id);
    }

    @Test
    @DisplayName("Synthèse « Ma journée » : activités, 5 prochains événements, tâches, rapport du jour, rapports à recevoir")
    void synthese() throws Exception {
        LocalDate jour = aujourdhui();
        activite(CHEF_SERVICE, jour, "FAITE");
        activite(CHEF_SERVICE, jour, null);
        for (int i = 1; i <= 6; i++) {
            Map<String, Object> e = new HashMap<>();
            e.put("titre", "Réunion " + i);
            e.put("debut", ReglesActivite.horodatage(ReglesActivite.debutJour(jour.plusDays(i)).plusSeconds(9 * 3600)));
            e.put("fin", ReglesActivite.horodatage(ReglesActivite.debutJour(jour.plusDays(i)).plusSeconds(10 * 3600)));
            mvc.perform(json(post("/api/v1/agenda").with(agent(CHEF_SERVICE)), e)).andExpect(status().isCreated());
        }
        Map<String, Object> t = new HashMap<>();
        t.put("titre", "Échéance proche");
        t.put("echeance", jour.plusDays(2).toString());
        mvc.perform(json(post("/api/v1/taches").with(agent(CHEF_SERVICE)), t)).andExpect(status().isCreated());
        String rapport = lire(
            mvc
                .perform(json(post("/api/v1/rapports").with(agent(AGENT)), Map.of("type", "QUOTIDIEN", "periode", jour.toString(), "mode", "REDIGE", "contenu", "x")))
                .andReturn()
        )
            .get("id")
            .asText();
        mvc.perform(post("/api/v1/rapports/" + rapport + "/soumission").with(agent(AGENT))).andExpect(status().isOk());

        mvc
            .perform(get("/api/v1/moi/synthese").with(agent(CHEF_SERVICE)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.activites.prevues").value(2))
            .andExpect(jsonPath("$.activites.faites").value(1))
            .andExpect(jsonPath("$.activites.enCours").value(0))
            .andExpect(jsonPath("$.evenements.length()").value(5))
            .andExpect(jsonPath("$.evenements[0].titre").value("Réunion 1"))
            .andExpect(jsonPath("$.taches.ouvertes").value(1))
            .andExpect(jsonPath("$.taches.enRetard").value(0))
            .andExpect(jsonPath("$.taches.echeanceProche").value(1))
            .andExpect(jsonPath("$.rapportDuJour").value("ABSENT"))
            .andExpect(jsonPath("$.rapportsARecevoir").value(1));
        mvc.perform(get("/api/v1/moi/synthese").with(agent(AGENT))).andExpect(jsonPath("$.rapportDuJour").value("SOUMIS"));

        // Relais de l'outbox : enveloppe CloudEvents 1.0 sur le sujet pilotage.report, clé = rapport, sans contenu.
        relais.relayerJusquaEnvoi(rapport);
        List<ConsumerRecord<String, String>> recus = relais.lire("pilotage.report", rapport);
        assertThat(recus).hasSize(1);
        var enveloppe = json.readTree(recus.get(0).value());
        assertThat(enveloppe.get("specversion").asText()).isEqualTo("1.0");
        assertThat(enveloppe.get("type").asText()).isEqualTo("pilotage.report-submitted.v1");
        assertThat(enveloppe.get("source").asText()).isEqualTo("/gimac/des/pilotage");
        assertThat(enveloppe.get("data").get("destinataireId").asText()).isEqualTo(CHEF_SERVICE);
        assertThat(enveloppe.get("data").has("contenu")).isFalse();
    }
}
