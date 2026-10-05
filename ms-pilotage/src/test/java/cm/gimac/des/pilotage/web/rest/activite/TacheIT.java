package cm.gimac.des.pilotage.web.rest.activite;

import static cm.gimac.des.pilotage.infrastructure.client.OrganisationStubAdapter.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.pilotage.AbstractPilotageIT;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

class TacheIT extends AbstractPilotageIT {

    private MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder r, Object corps) throws Exception {
        return r.contentType(MediaType.APPLICATION_JSON).content(corps(corps));
    }

    private String confier(String createur, String responsable) throws Exception {
        Map<String, Object> t = new HashMap<>();
        t.put("titre", "Préparer le dossier de la liaison L-12");
        t.put("echeance", aujourdhui().minusDays(1).toString());
        t.put("priorite", "HAUTE");
        t.put("responsableId", responsable);
        return lire(mvc.perform(json(post("/api/v1/taches").with(responsable(createur)), t)).andExpect(status().isCreated()).andReturn()).get("id").asText();
    }

    private void transition(String id, String acteur, String etat, String motif, int statut) throws Exception {
        Map<String, Object> c = new HashMap<>();
        c.put("etat", etat);
        c.put("motif", motif);
        mvc.perform(json(post("/api/v1/taches/" + id + "/transition").with(agent(acteur)), c)).andExpect(status().is(statut));
    }

    @Test
    @DisplayName("Confier une tâche à un membre de son équipe (PIL_TASK_ASSIGN) ; événements créée et confiée ; historique")
    void confierAUnMembre() throws Exception {
        String id = confier(CHEF_SERVICE, AGENT);

        mvc
            .perform(get("/api/v1/taches/" + id).with(agent(AGENT)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.etat").value("OUVERTE"))
            .andExpect(jsonPath("$.priorite").value("HAUTE"))
            .andExpect(jsonPath("$.responsable.id").value(AGENT))
            .andExpect(jsonPath("$.responsable.nomAffiche").value("Serge Ndzana"))
            .andExpect(jsonPath("$.createur.id").value(CHEF_SERVICE))
            .andExpect(jsonPath("$.origine.type").value("INTERNE"))
            .andExpect(jsonPath("$.origine.reference").doesNotExist())
            .andExpect(jsonPath("$.enRetard").value(true))
            .andExpect(jsonPath("$.historique[0].action").value("CREEE"))
            .andExpect(jsonPath("$.historique[0].acteur.id").value(CHEF_SERVICE))
            .andExpect(jsonPath("$.historique[0].detail").value("Confiée à Serge Ndzana"));
        assertThat(jdbc.queryForList("select event_type from outbox_event where aggregate_id = ? order by event_type", String.class, id)).containsExactly(
            "pilotage.task-assigned.v1",
            "pilotage.task-created.v1"
        );

        mvc.perform(get("/api/v1/taches").param("vue", "moi").with(agent(AGENT))).andExpect(jsonPath("$.total").value(1)).andExpect(jsonPath("$.contenu[0].id").value(id));
        mvc.perform(get("/api/v1/taches").param("vue", "creees").with(responsable(CHEF_SERVICE))).andExpect(jsonPath("$.total").value(1));
        mvc.perform(get("/api/v1/taches").param("vue", "equipe").with(responsable(CHEF_DES))).andExpect(jsonPath("$.total").value(1));
        mvc.perform(get("/api/v1/taches").param("vue", "equipe").param("etat", "TERMINEE").with(responsable(CHEF_DES))).andExpect(jsonPath("$.total").value(0));
        mvc.perform(get("/api/v1/taches").param("vue", "equipe").with(agent(AGENT))).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("PIL_HORS_EQUIPE"));
        mvc.perform(get("/api/v1/taches").param("vue", "toutes").with(agent(AGENT))).andExpect(status().isBadRequest());
        // Lecture : chaîne hiérarchique avec PIL_TEAM_READ ; hors équipe : 403.
        mvc.perform(get("/api/v1/taches/" + id).with(responsable(CHEF_DES))).andExpect(status().isOk());
        mvc.perform(get("/api/v1/taches/" + id).with(responsable(CHEF_DDRM))).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("PIL_HORS_EQUIPE"));
        mvc.perform(get("/api/v1/taches/" + id).with(agent(AGENT_BIS))).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/taches/00000000-0000-4000-8000-000000000000").with(agent(AGENT))).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("PIL_TACHE_INTROUVABLE"));
    }

    @Test
    @DisplayName("Confier : sans PIL_TASK_ASSIGN (403 PIL_ACTION_RESERVEE), hors équipe (403 PIL_HORS_EQUIPE), compte inactif (422)")
    void confierRefuse() throws Exception {
        Map<String, Object> t = new HashMap<>();
        t.put("titre", "T");
        t.put("responsableId", AGENT);
        mvc
            .perform(json(post("/api/v1/taches").with(utilisateur(CHEF_SERVICE, "PIL_ACTIVITY_OWN", "PIL_TEAM_READ")), t))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("PIL_ACTION_RESERVEE"));
        t.put("responsableId", CHEF_DDRM);
        mvc.perform(json(post("/api/v1/taches").with(responsable(CHEF_SERVICE)), t)).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("PIL_HORS_EQUIPE"));
        t.put("responsableId", INACTIF);
        mvc
            .perform(json(post("/api/v1/taches").with(responsable(CHEF_SERVICE)), t))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("PIL_RESPONSABLE_INVALIDE"));
        t.remove("responsableId");
        t.put("titre", " ");
        mvc.perform(json(post("/api/v1/taches").with(agent(AGENT)), t)).andExpect(status().isBadRequest()).andExpect(jsonPath("$.details.champ").value("titre"));
        assertThat(compter("select count(*) from tache")).isZero();
    }

    @Test
    @DisplayName("Transitions : responsable seul, motif pour BLOQUEE et ANNULEE, 409 hors transition, créateur qui annule, task-completed")
    void transitions() throws Exception {
        String id = confier(CHEF_SERVICE, AGENT);
        transition(id, AGENT, "TERMINEE", null, 409);
        transition(id, CHEF_SERVICE, "EN_COURS", null, 403);
        transition(id, AGENT, "BLOQUEE", null, 422);
        transition(id, AGENT, "BLOQUEE", "En attente du participant", 200);
        transition(id, AGENT, "EN_COURS", null, 200);
        transition(id, AGENT, "TERMINEE", null, 200);
        mvc
            .perform(get("/api/v1/taches/" + id).with(agent(AGENT)))
            .andExpect(jsonPath("$.etat").value("TERMINEE"))
            .andExpect(jsonPath("$.termineeLe").isNotEmpty())
            .andExpect(jsonPath("$.enRetard").value(false))
            .andExpect(jsonPath("$.historique.length()").value(4))
            .andExpect(jsonPath("$.historique[1].action").value("TRANSITION"))
            .andExpect(jsonPath("$.historique[1].detail").value("OUVERTE vers BLOQUEE : En attente du participant"));
        assertThat(compter("select count(*) from outbox_event where aggregate_id = ? and event_type = 'pilotage.task-completed.v1'", id)).isEqualTo(1);
        transition(id, AGENT, "EN_COURS", null, 422);
        transition(id, AGENT, "EN_COURS", "Reprise demandée", 200);
        transition(id, CHEF_SERVICE, "ANNULEE", null, 422);
        transition(id, CHEF_SERVICE, "ANNULEE", "Plus nécessaire", 200);
        transition(id, AGENT, "EN_COURS", "x", 409);
        transition(id, CHEF_DDRM, "EN_COURS", "x", 403);

        // Historique en ajout seul : la base refuse modification et suppression.
        assertThatThrownBy(() -> jdbc.update("update tache_historique set detail = 'falsifié' where tache_id = ?::uuid", id)).hasMessageContaining("ajout seul");
        assertThatThrownBy(() -> jdbc.update("delete from tache_historique where tache_id = ?::uuid", id)).hasMessageContaining("ajout seul");
    }

    @Test
    @DisplayName("Modification par le créateur ou le responsable ; réaffectation par le créateur seul (task-assigned)")
    void modificationEtReaffectation() throws Exception {
        String id = confier(CHEF_SERVICE, AGENT);
        Map<String, Object> m = new HashMap<>();
        m.put("titre", "Dossier L-12 complété");
        m.put("priorite", "URGENTE");
        m.put("responsableId", AGENT);
        mvc.perform(json(put("/api/v1/taches/" + id).with(agent(AGENT)), m)).andExpect(status().isOk()).andExpect(jsonPath("$.priorite").value("URGENTE"));
        m.put("responsableId", AGENT_BIS);
        mvc.perform(json(put("/api/v1/taches/" + id).with(responsable(AGENT)), m)).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("PIL_ACTION_RESERVEE"));
        var t = lire(mvc.perform(json(put("/api/v1/taches/" + id).with(responsable(CHEF_SERVICE)), m)).andExpect(status().isOk()).andReturn());
        assertThat(t.at("/responsable/id").asText()).isEqualTo(AGENT_BIS);
        List<String> actions = new java.util.ArrayList<>();
        List<String> details = new java.util.ArrayList<>();
        t.get("historique").forEach(h -> {
            actions.add(h.get("action").asText());
            details.add(h.get("detail").asText());
        });
        assertThat(actions).containsExactlyInAnyOrder("CREEE", "MODIFIEE", "REAFFECTEE");
        assertThat(details).contains("De Serge Ndzana à Clarisse Bilong");
        assertThat(compter("select count(*) from outbox_event where aggregate_id = ? and event_type = 'pilotage.task-assigned.v1'", id)).isEqualTo(2);
        mvc.perform(json(put("/api/v1/taches/" + id).with(responsable(CHEF_DDRM)), m)).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("PIL_HORS_EQUIPE"));
    }

    @Test
    @DisplayName("Création par un autre service : jeton de service INTERNAL_PILOTAGE_TASK_CREATE, idempotente (201 puis 200)")
    void creationInterne() throws Exception {
        Map<String, Object> origine = new HashMap<>();
        origine.put("type", "OPERATION");
        origine.put("reference", "OP-2026-0042");
        origine.put("service", "homologation");
        origine.put("lien", "/des/parcours/operations/OP-2026-0042");
        Map<String, Object> t = new HashMap<>();
        t.put("titre", "Valider le procès-verbal de l'opération");
        t.put("responsableId", AGENT);
        t.put("origine", origine);

        String id = lire(
            mvc
                .perform(json(post("/api/v1/interne/taches").with(service("homologation-service", "INTERNAL_PILOTAGE_TASK_CREATE")), t))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.origine.type").value("OPERATION"))
                .andExpect(jsonPath("$.origine.reference").value("OP-2026-0042"))
                .andExpect(jsonPath("$.origine.lien").value("/des/parcours/operations/OP-2026-0042"))
                .andExpect(jsonPath("$.createur.id").value("homologation-service"))
                .andReturn()
        )
            .get("id")
            .asText();
        mvc
            .perform(json(post("/api/v1/interne/taches").with(service("homologation-service", "INTERNAL_PILOTAGE_TASK_CREATE")), t))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(id));
        assertThat(compter("select count(*) from tache")).isEqualTo(1);
        assertThat(compter("select count(*) from outbox_event where event_type = 'pilotage.task-created.v1'")).isEqualTo(1);

        // Jeton utilisateur, ou jeton de service sans la permission : refusés.
        mvc.perform(json(post("/api/v1/interne/taches").with(utilisateur(AGENT, "INTERNAL_PILOTAGE_TASK_CREATE")), t)).andExpect(status().isForbidden());
        mvc.perform(json(post("/api/v1/interne/taches").with(service("homologation-service", "INTERNAL_IAM_READ")), t)).andExpect(status().isForbidden());
        // Responsable externe, lien vers un autre site, origine INTERNE : refusés.
        t.put("responsableId", CORRESPONDANT);
        origine.put("reference", "OP-2026-0043");
        mvc
            .perform(json(post("/api/v1/interne/taches").with(service("homologation-service", "INTERNAL_PILOTAGE_TASK_CREATE")), t))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("PIL_RESPONSABLE_INVALIDE"));
        t.put("responsableId", AGENT);
        origine.put("lien", "//ailleurs.example/x");
        mvc.perform(json(post("/api/v1/interne/taches").with(service("homologation-service", "INTERNAL_PILOTAGE_TASK_CREATE")), t)).andExpect(status().isBadRequest());
        origine.put("lien", null);
        origine.put("type", "INTERNE");
        mvc.perform(json(post("/api/v1/interne/taches").with(service("homologation-service", "INTERNAL_PILOTAGE_TASK_CREATE")), t)).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("D-77 : clôture puis réouverture par le service qui a créé la tâche, idempotentes ; jeton de service exigé")
    void clotureEtReouvertureInterne() throws Exception {
        Map<String, Object> origine = new HashMap<>();
        origine.put("type", "OPERATION");
        origine.put("reference", "LIA-2026-001/17/R");
        origine.put("service", "homologation");
        origine.put("lien", "/des/parcours/x?onglet=operations");
        Map<String, Object> t = new HashMap<>();
        t.put("titre", "Constat de la couverture du périmètre (LIA-2026-001)");
        t.put("responsableId", AGENT);
        t.put("origine", origine);
        String id = lire(
            mvc.perform(json(post("/api/v1/interne/taches").with(service("homologation-service", "INTERNAL_PILOTAGE_TASK_CREATE")), t)).andExpect(status().isCreated()).andReturn()
        )
            .get("id")
            .asText();
        // Une tâche interne de même référence n'est jamais visée.
        Map<String, Object> interne = new HashMap<>();
        interne.put("titre", "Tâche personnelle");
        mvc.perform(json(post("/api/v1/taches").with(agent(AGENT)), interne)).andExpect(status().isCreated());

        Map<String, Object> cible = new HashMap<>();
        cible.put("origine", Map.of("type", "OPERATION", "reference", "LIA-2026-001/17/R"));
        cible.put("responsableId", AGENT);
        mvc
            .perform(json(post("/api/v1/interne/taches/cloture").with(service("homologation-service", "INTERNAL_PILOTAGE_TASK_CREATE")), cible))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(id))
            .andExpect(jsonPath("$[0].etat").value("TERMINEE"));
        mvc
            .perform(json(post("/api/v1/interne/taches/cloture").with(service("homologation-service", "INTERNAL_PILOTAGE_TASK_CREATE")), cible))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].etat").value("TERMINEE"));
        assertThat(compter("select count(*) from outbox_event where aggregate_id = ? and event_type = 'pilotage.task-completed.v1'", id)).isEqualTo(1);

        mvc
            .perform(json(post("/api/v1/interne/taches/reouverture").with(service("homologation-service", "INTERNAL_PILOTAGE_TASK_CREATE")), cible))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].etat").value("EN_COURS"));
        mvc
            .perform(get("/api/v1/taches/" + id).with(agent(AGENT)))
            .andExpect(jsonPath("$.etat").value("EN_COURS"))
            .andExpect(jsonPath("$.historique[*].detail").value(org.hamcrest.Matchers.hasItem("OUVERTE vers TERMINEE : terminée par le service homologation")))
            .andExpect(jsonPath("$.historique[*].detail").value(org.hamcrest.Matchers.hasItem("TERMINEE vers EN_COURS : rouverte par le service homologation")));

        // Sans responsable : toutes les tâches de l'origine ; autre responsable ou autre type : aucune.
        cible.put("responsableId", AGENT_BIS);
        mvc.perform(json(post("/api/v1/interne/taches/cloture").with(service("homologation-service", "INTERNAL_PILOTAGE_TASK_CREATE")), cible)).andExpect(jsonPath("$.length()").value(0));
        cible.put("responsableId", null);
        cible.put("origine", Map.of("type", "REFERENTIEL", "reference", "LIA-2026-001/17/R"));
        mvc.perform(json(post("/api/v1/interne/taches/cloture").with(service("homologation-service", "INTERNAL_PILOTAGE_TASK_CREATE")), cible)).andExpect(jsonPath("$.length()").value(0));
        cible.put("origine", Map.of("reference", "LIA-2026-001/17/R"));
        mvc
            .perform(json(post("/api/v1/interne/taches/cloture").with(service("homologation-service", "INTERNAL_PILOTAGE_TASK_CREATE")), cible))
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].etat").value("TERMINEE"));

        // Origine absente : 400 ; jeton utilisateur ou sans la permission : 403.
        mvc.perform(json(post("/api/v1/interne/taches/cloture").with(service("homologation-service", "INTERNAL_PILOTAGE_TASK_CREATE")), Map.of())).andExpect(status().isBadRequest());
        mvc.perform(json(post("/api/v1/interne/taches/reouverture").with(utilisateur(AGENT, "INTERNAL_PILOTAGE_TASK_CREATE")), cible)).andExpect(status().isForbidden());
        mvc.perform(json(post("/api/v1/interne/taches/cloture").with(service("homologation-service", "INTERNAL_IAM_READ")), cible)).andExpect(status().isForbidden());
    }
}
