package cm.gimac.des.pilotage.web.rest.activite;

import static cm.gimac.des.pilotage.infrastructure.client.OrganisationStubAdapter.AGENT;
import static cm.gimac.des.pilotage.infrastructure.client.OrganisationStubAdapter.AGENT_BIS;
import static cm.gimac.des.pilotage.infrastructure.client.OrganisationStubAdapter.CHEF_DES;
import static cm.gimac.des.pilotage.infrastructure.client.OrganisationStubAdapter.CHEF_SERVICE;
import static cm.gimac.des.pilotage.infrastructure.client.OrganisationStubAdapter.DG;
import static cm.gimac.des.pilotage.infrastructure.client.OrganisationStubAdapter.INACTIF;
import static cm.gimac.des.pilotage.infrastructure.client.OrganisationStubAdapter.SANS_RESPONSABLE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cm.gimac.des.pilotage.AbstractPilotageIT;
import cm.gimac.des.pilotage.infrastructure.client.OrganisationStubAdapter.Compte;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Destinataire d'un rapport (docs/lot1/mon-activite.md §2.4, « Destinataire du rapport ») : n+1 actif, sinon chef de
 * l'unité de l'auteur, sinon chef d'une unité parente ; au sommet de l'organisation, rapport déposé sans destinataire ;
 * choix de l'auteur dans sa chaîne hiérarchique ; route de lecture des destinataires possibles.
 */
class RapportDestinataireIT extends AbstractPilotageIT {

    private static final String SANS_N1_SNA = "00000000-0000-4000-8000-0000000d0031";
    private static final String SANS_N1_MONETIQUE = "00000000-0000-4000-8000-0000000d0032";
    private static final String N1_INACTIF = "00000000-0000-4000-8000-0000000d0033";

    private MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder r, Object corps) throws Exception {
        return r.contentType(MediaType.APPLICATION_JSON).content(corps(corps));
    }

    private String brouillon(String auteur) throws Exception {
        Map<String, Object> r = Map.of("type", "QUOTIDIEN", "periode", aujourdhui().toString(), "mode", "REDIGE", "contenu", "Réalisé : revue des dossiers");
        return lire(mvc.perform(json(post("/api/v1/rapports").with(agent(auteur)), r)).andExpect(status().isCreated()).andReturn()).get("id").asText();
    }

    private JsonNode soumettre(String auteur, String id) throws Exception {
        return lire(mvc.perform(post("/api/v1/rapports/" + id + "/soumission").with(agent(auteur))).andExpect(status().isOk()).andReturn());
    }

    @Test
    @DisplayName("Sommet de l'organisation (DG, compte sans unité) : le rapport est déposé, sans destinataire ni visa")
    void sommet() throws Exception {
        for (String auteur : new String[] { DG, SANS_RESPONSABLE }) {
            String id = brouillon(auteur);
            JsonNode r = soumettre(auteur, id);
            assertThat(r.get("etat").asText()).isEqualTo("DEPOSE");
            assertThat(r.get("destinataire").isNull()).isTrue();
            assertThat(r.get("destinataireMotif").isNull()).isTrue();
            mvc.perform(get("/api/v1/rapports/" + id).with(agent(auteur))).andExpect(status().isOk()).andExpect(jsonPath("$.etat").value("DEPOSE"));
            mvc.perform(json(post("/api/v1/rapports/" + id + "/visa").with(responsable(CHEF_DES)), Map.of())).andExpect(status().is4xxClientError());
            JsonNode charge = json.readTree(
                jdbc.queryForObject("select payload::text from outbox_event where event_type = 'pilotage.report-submitted.v1' and aggregate_id = ?", String.class, id)
            );
            assertThat(charge.get("etat").asText()).isEqualTo("DEPOSE");
            assertThat(charge.get("destinataireId").isNull()).isTrue();
        }
    }

    @Test
    @DisplayName("Sans n+1 actif : chef de l'unité, sinon chef d'une unité parente (jamais l'auteur lui-même)")
    void chefsDUnite() throws Exception {
        organisation.enregistrer(new Compte(SANS_N1_SNA, "Ali Moussa", "Chargé d'études", "SNA", null, true, true));
        organisation.enregistrer(new Compte(SANS_N1_MONETIQUE, "Berthe Ondo", "Opératrice", "PROD_MONETIQUE", null, true, true));
        organisation.enregistrer(new Compte(N1_INACTIF, "Carine Nlend", "Analyste", "SNA", INACTIF, true, true));
        organisation.enregistrer(new Compte(CHEF_SERVICE, "Aminata Ngono", "Cheffe du service Normes et Agréments", "SNA", null, true, true));

        JsonNode r = soumettre(SANS_N1_SNA, brouillon(SANS_N1_SNA));
        assertThat(r.get("etat").asText()).isEqualTo("SOUMIS");
        assertThat(r.at("/destinataire/id").asText()).isEqualTo(CHEF_SERVICE);
        assertThat(r.get("destinataireMotif").asText()).isEqualTo("CHEF_UNITE");
        r = soumettre(N1_INACTIF, brouillon(N1_INACTIF));
        assertThat(r.at("/destinataire/id").asText()).isEqualTo(CHEF_SERVICE);
        assertThat(r.get("destinataireMotif").asText()).isEqualTo("CHEF_UNITE");
        r = soumettre(SANS_N1_MONETIQUE, brouillon(SANS_N1_MONETIQUE));
        assertThat(r.at("/destinataire/id").asText()).isEqualTo(CHEF_DES);
        assertThat(r.get("destinataireMotif").asText()).isEqualTo("CHEF_UNITE_PARENTE");
        // La cheffe du SNA, sans n+1 : pas à elle-même, au chef de l'unité parente.
        r = soumettre(CHEF_SERVICE, brouillon(CHEF_SERVICE));
        assertThat(r.at("/destinataire/id").asText()).isEqualTo(CHEF_DES);
        assertThat(r.get("destinataireMotif").asText()).isEqualTo("CHEF_UNITE_PARENTE");
        // Le destinataire trouvé reçoit et vise le rapport.
        String id = r.get("id").asText();
        mvc.perform(get("/api/v1/rapports").param("vue", "recus").with(responsable(CHEF_DES))).andExpect(jsonPath("$.contenu[?(@.id == '" + id + "')]").exists());
        mvc.perform(json(post("/api/v1/rapports/" + id + "/visa").with(responsable(CHEF_DES)), Map.of("commentaire", "Bien reçu"))).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Destinataires possibles avant la soumission ; choix dans la chaîne hiérarchique (CHOISI), refus hors de la chaîne")
    void choix() throws Exception {
        String id = brouillon(AGENT);
        JsonNode d = lire(mvc.perform(get("/api/v1/rapports/" + id + "/destinataires").with(agent(AGENT))).andExpect(status().isOk()).andReturn());
        assertThat(d.at("/destinataire/id").asText()).isEqualTo(CHEF_SERVICE);
        assertThat(d.get("motif").asText()).isEqualTo("N_PLUS_UN");
        assertThat(d.get("choix").findValuesAsText("id")).containsExactly(CHEF_SERVICE, CHEF_DES, DG);
        assertThat(d.get("choix").findValuesAsText("lien")).containsExactly("N_PLUS_UN", "CHEF_UNITE_PARENTE", "CHEF_UNITE_PARENTE");
        assertThat(d.at("/choix/0/unite").isNull()).isTrue();
        assertThat(d.at("/choix/1/unite").asText()).isEqualTo("DES");
        assertThat(d.at("/choix/2/unite").asText()).isEqualTo("DG");
        assertThat(d.at("/choix/1/nomAffiche").asText()).isEqualTo("Paul Mbarga Atangana");
        mvc.perform(get("/api/v1/rapports/" + id + "/destinataires").with(agent(AGENT_BIS))).andExpect(status().isForbidden());

        mvc
            .perform(json(post("/api/v1/rapports/" + id + "/soumission").with(agent(AGENT)), Map.of("destinataireId", AGENT_BIS)))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("PIL_DESTINATAIRE_INVALIDE"));
        JsonNode r = lire(
            mvc.perform(json(post("/api/v1/rapports/" + id + "/soumission").with(agent(AGENT)), Map.of("destinataireId", CHEF_DES))).andExpect(status().isOk()).andReturn()
        );
        assertThat(r.at("/destinataire/id").asText()).isEqualTo(CHEF_DES);
        assertThat(r.get("destinataireMotif").asText()).isEqualTo("CHOISI");

        // Sommet : aucun choix, dépôt annoncé.
        String sommet = brouillon(DG);
        d = lire(mvc.perform(get("/api/v1/rapports/" + sommet + "/destinataires").with(agent(DG))).andExpect(status().isOk()).andReturn());
        assertThat(d.get("destinataire").isNull()).isTrue();
        assertThat(d.get("depot").asBoolean()).isTrue();
        assertThat(d.get("choix")).isEmpty();
    }
}
