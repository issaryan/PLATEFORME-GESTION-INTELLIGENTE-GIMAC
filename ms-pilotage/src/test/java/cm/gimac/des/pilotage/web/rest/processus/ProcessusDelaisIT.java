package cm.gimac.des.pilotage.web.rest.processus;

import static cm.gimac.des.pilotage.infrastructure.client.OrganisationStubAdapter.CHEF_RESEAU;
import static cm.gimac.des.pilotage.infrastructure.client.OrganisationStubAdapter.INGENIEUR_RESEAU;
import static cm.gimac.des.pilotage.infrastructure.client.OrganisationStubAdapter.TECHNICIEN_RESEAU;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Délai cible et délai maximal (D-106) : saisie du processus et de ses étapes (400 si le maximum est inférieur à la
 * cible), restitution, échéances de l'étape activée (la tâche et le retard portent le maximum, l'échéance proche la
 * cible) et délais de la demande comptés depuis son lancement.
 */
class ProcessusDelaisIT extends AbstractProcessusIT {

    private Map<String, Object> lancement() {
        Map<String, Object> l = new HashMap<>();
        l.put("intitule", "Flux à délais");
        l.put("champs", Map.of("SOURCE", "10.0.0.1", "PORT", 443));
        l.put("designations", Map.of("MISE_EN_OEUVRE", INGENIEUR_RESEAU));
        return l;
    }

    @Test
    @DisplayName("Saisie contrôlée, restitution, échéances cible et maximale de l'étape, délais de la demande")
    void delais() throws Exception {
        ObjectNode d = definition("FLUX_DELAIS");
        d.put("delaiMaxJours", 9);
        mvc
            .perform(json(post(BASE).with(chef(CHEF_RESEAU)), d))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("PIL_REQUETE_INVALIDE"))
            .andExpect(jsonPath("$.details.champ").value("delaiMaxJours"));
        d.put("delaiMaxJours", 15);
        ObjectNode premiere = (ObjectNode) d.get("etapes").get(0);
        int cible = premiere.get("delaiJours").asInt();
        premiere.put("delaiMaxJours", cible - 1 < 0 ? -1 : cible - 1);
        mvc
            .perform(json(post(BASE).with(chef(CHEF_RESEAU)), d))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.details.champ").value("etapes[0].delaiMaxJours"));
        premiere.put("delaiMaxJours", cible + 4);
        String id = creer(chef(CHEF_RESEAU), d);
        mvc
            .perform(get(BASE + "/" + id).with(chef(CHEF_RESEAU)))
            .andExpect(jsonPath("$.delaiCibleJours").value(10))
            .andExpect(jsonPath("$.delaiMaxJours").value(15))
            .andExpect(jsonPath("$.etapes[0].delaiMaxJours").value(cible + 4));
        publier(chef(CHEF_RESEAU), id);

        JsonNode i = lire(mvc.perform(json(post(BASE + "/" + id + "/instances").with(agent(TECHNICIEN_RESEAU)), lancement())).andExpect(status().isCreated()).andReturn());
        JsonNode e = i.get("etapes").get(0);
        LocalDate echeanceCible = LocalDate.parse(e.get("echeanceCible").asText());
        LocalDate echeance = LocalDate.parse(e.get("echeance").asText());
        assertThat(echeance).isEqualTo(echeanceCible.plusDays(4));
        assertThat(e.get("delaiMaxJours").asInt()).isEqualTo(cible + 4);
        assertThat(e.get("enRetard").asBoolean()).isFalse();
        JsonNode delais = i.get("delais");
        assertThat(delais.get("cibleJours").asInt()).isEqualTo(10);
        assertThat(delais.get("maxJours").asInt()).isEqualTo(15);
        assertThat(LocalDate.parse(delais.get("echeanceMax").asText())).isEqualTo(LocalDate.parse(delais.get("echeanceCible").asText()).plusDays(5));
        assertThat(delais.get("enRetard").asBoolean()).isFalse();

        // La tâche de l'étape porte l'échéance maximale.
        String tache = e.get("tacheId").asText();
        mvc.perform(get("/api/v1/taches/" + tache).with(agent(TECHNICIEN_RESEAU))).andExpect(status().isOk()).andExpect(jsonPath("$.echeance").value(echeance.toString()));

        // Une version publiée garde ses délais figés : une modification est refusée (409).
        mvc.perform(json(put(BASE + "/" + id).with(chef(CHEF_RESEAU)), d)).andExpect(status().isConflict());
    }
}
