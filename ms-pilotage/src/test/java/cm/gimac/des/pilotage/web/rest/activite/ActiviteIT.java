package cm.gimac.des.pilotage.web.rest.activite;

import static cm.gimac.des.pilotage.infrastructure.client.OrganisationStubAdapter.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.pilotage.AbstractPilotageIT;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class ActiviteIT extends AbstractPilotageIT {

    private String creer(String auteur, String jour, String titre, Integer ordre) throws Exception {
        Map<String, Object> a = new HashMap<>();
        a.put("jour", jour);
        a.put("titre", titre);
        a.put("ordre", ordre);
        return lire(
            mvc.perform(post("/api/v1/activites").with(agent(auteur)).contentType(MediaType.APPLICATION_JSON).content(corps(a))).andExpect(status().isCreated()).andReturn()
        )
            .get("id")
            .asText();
    }

    @Test
    @DisplayName("Création, tri par jour puis ordre, ordre attribué en fin de journée, modification par l'auteur seul")
    void creationEtTri() throws Exception {
        creer(AGENT, "2026-10-07", "Mercredi", null);
        String b = creer(AGENT, "2026-10-06", "Deuxième", 2);
        creer(AGENT, "2026-10-06", "Première", 1);
        String c = creer(AGENT, "2026-10-06", "Troisième", null);

        mvc
            .perform(get("/api/v1/activites").param("du", "2026-10-06").param("au", "2026-10-07").with(agent(AGENT)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(4))
            .andExpect(jsonPath("$[0].titre").value("Première"))
            .andExpect(jsonPath("$[1].titre").value("Deuxième"))
            .andExpect(jsonPath("$[2].titre").value("Troisième"))
            .andExpect(jsonPath("$[2].ordre").value(3))
            .andExpect(jsonPath("$[2].statut").value("A_FAIRE"))
            .andExpect(jsonPath("$[2].auteur.id").value(AGENT))
            .andExpect(jsonPath("$[3].jour").value("2026-10-07"));

        Map<String, Object> m = new HashMap<>();
        m.put("jour", "2026-10-06");
        m.put("titre", "Deuxième, précisée");
        m.put("dureeMinutes", 90);
        mvc
            .perform(put("/api/v1/activites/" + b).with(agent(AGENT)).contentType(MediaType.APPLICATION_JSON).content(corps(m)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.titre").value("Deuxième, précisée"))
            .andExpect(jsonPath("$.ordre").value(2))
            .andExpect(jsonPath("$.dureeMinutes").value(90));
        m.put("dureeMinutes", 1441);
        mvc
            .perform(put("/api/v1/activites/" + b).with(agent(AGENT)).contentType(MediaType.APPLICATION_JSON).content(corps(m)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.details.champ").value("dureeMinutes"));
        // Le responsable lit, mais ne modifie pas : 403 PIL_ACTION_RESERVEE ; hors équipe : PIL_HORS_EQUIPE.
        mvc.perform(delete("/api/v1/activites/" + c).with(responsable(CHEF_SERVICE))).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("PIL_ACTION_RESERVEE"));
        mvc.perform(delete("/api/v1/activites/" + c).with(agent(AGENT_BIS))).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("PIL_HORS_EQUIPE"));
        mvc
            .perform(get("/api/v1/activites").param("du", "2026-10-06").param("au", "2026-10-06").param("collaborateur", AGENT).with(responsable(CHEF_SERVICE)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(3));
        mvc
            .perform(get("/api/v1/activites").param("du", "2026-10-06").param("au", "2026-10-06").param("collaborateur", AGENT).with(responsable(CHEF_DDRM)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("PIL_HORS_EQUIPE"));
        mvc.perform(delete("/api/v1/activites/" + c).with(agent(AGENT))).andExpect(status().isNoContent());
        mvc
            .perform(get("/api/v1/activites").param("du", "2026-10-01").param("au", "2026-11-01").with(agent(AGENT)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("PIL_INTERVALLE_INVALIDE"));
    }

    @Test
    @DisplayName("Reporter une activité en crée une copie A_FAIRE au jour indiqué ; une activité reportée ne change plus (409)")
    void report() throws Exception {
        String a = creer(AGENT, "2026-10-06", "Visite du site", null);
        creer(AGENT, "2026-10-08", "Déjà prévue", null);

        mvc
            .perform(post("/api/v1/activites/" + a + "/statut").with(agent(AGENT)).contentType(MediaType.APPLICATION_JSON).content("{\"statut\":\"REPORTEE\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.details.champ").value("reporteeAu"));
        mvc
            .perform(post("/api/v1/activites/" + a + "/statut").with(agent(AGENT)).contentType(MediaType.APPLICATION_JSON).content("{\"statut\":\"REPORTEE\",\"reporteeAu\":\"2026-10-06\"}"))
            .andExpect(status().isBadRequest());
        mvc
            .perform(post("/api/v1/activites/" + a + "/statut").with(agent(AGENT)).contentType(MediaType.APPLICATION_JSON).content("{\"statut\":\"REPORTEE\",\"reporteeAu\":\"2026-10-08\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.statut").value("REPORTEE"))
            .andExpect(jsonPath("$.reporteeAu").value("2026-10-08"));

        mvc
            .perform(get("/api/v1/activites").param("du", "2026-10-08").param("au", "2026-10-08").with(agent(AGENT)))
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[1].titre").value("Visite du site"))
            .andExpect(jsonPath("$[1].statut").value("A_FAIRE"))
            .andExpect(jsonPath("$[1].ordre").value(2))
            .andExpect(jsonPath("$[1].reporteeAu").doesNotExist());
        assertThat(compter("select count(*) from activite where origine_id = ?::uuid", a)).isEqualTo(1);

        mvc
            .perform(post("/api/v1/activites/" + a + "/statut").with(agent(AGENT)).contentType(MediaType.APPLICATION_JSON).content("{\"statut\":\"FAITE\"}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("PIL_TRANSITION_INTERDITE"));
        mvc
            .perform(post("/api/v1/activites/" + a + "/statut").with(agent(AGENT)).contentType(MediaType.APPLICATION_JSON).content("{\"statut\":\"INCONNU\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Activité liée à une tâche : tâche de l'appelant seulement (422 PIL_TACHE_INVALIDE)")
    void tacheLiee() throws Exception {
        String tache = lire(
            mvc
                .perform(post("/api/v1/taches").with(agent(AGENT_BIS)).contentType(MediaType.APPLICATION_JSON).content("{\"titre\":\"Tâche de Clarisse\"}"))
                .andExpect(status().isCreated())
                .andReturn()
        )
            .get("id")
            .asText();
        Map<String, Object> a = new HashMap<>();
        a.put("jour", "2026-10-06");
        a.put("titre", "Travail");
        a.put("tacheId", tache);
        mvc
            .perform(post("/api/v1/activites").with(agent(AGENT)).contentType(MediaType.APPLICATION_JSON).content(corps(a)))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("PIL_TACHE_INVALIDE"));
        mvc
            .perform(post("/api/v1/activites").with(agent(AGENT_BIS)).contentType(MediaType.APPLICATION_JSON).content(corps(a)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.tacheId").value(tache));
    }
}
