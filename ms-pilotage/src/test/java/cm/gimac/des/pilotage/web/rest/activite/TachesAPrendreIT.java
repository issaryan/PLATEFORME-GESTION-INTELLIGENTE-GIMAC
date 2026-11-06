package cm.gimac.des.pilotage.web.rest.activite;

import static cm.gimac.des.pilotage.infrastructure.client.OrganisationStubAdapter.AGENT;
import static cm.gimac.des.pilotage.infrastructure.client.OrganisationStubAdapter.AGENT_BIS;
import static cm.gimac.des.pilotage.infrastructure.client.OrganisationStubAdapter.CHEF_DDRM;
import static cm.gimac.des.pilotage.infrastructure.client.OrganisationStubAdapter.CHEF_SERVICE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cm.gimac.des.pilotage.AbstractPilotageIT;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Tâches à prendre offertes par un autre service (D-93, docs/lot1/effet-reel-referentiels.md §9) : création sans
 * responsable avec profilsRequis (et uniteCible facultative), idempotence par origine.reference seule, visibilité
 * dans « Mes tâches » des porteurs des profils, prise réservée (pilotage.task-taken.v1), clôture et réouverture sans
 * avoir été prise, refus ; offre par permission (permissionsRequises, porteurs lus par permission, D-94).
 */
class TachesAPrendreIT extends AbstractPilotageIT {

    private static final String INTERNE = "/api/v1/interne/taches";

    private static RequestPostProcessor homologation() {
        return service("homologation-service", "INTERNAL_PILOTAGE_TASK_CREATE");
    }

    private MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder r, Object corps) throws Exception {
        return r.contentType(MediaType.APPLICATION_JSON).content(corps(corps));
    }

    private static Map<String, Object> offre(String reference, List<String> profils, String unite) {
        Map<String, Object> origine = new HashMap<>();
        origine.put("type", "OPERATION");
        origine.put("reference", reference);
        origine.put("service", "homologation");
        origine.put("lien", "/des/parcours/x?onglet=operations");
        Map<String, Object> t = new HashMap<>();
        t.put("titre", "Déposer le rapport d'essai (LIA-2026-001)");
        t.put("priorite", "HAUTE");
        t.put("profilsRequis", profils);
        t.put("uniteCible", unite);
        t.put("origine", origine);
        return t;
    }

    private List<String> mesTaches(String qui) throws Exception {
        JsonNode p = lire(mvc.perform(get("/api/v1/taches").param("vue", "moi").with(agent(qui))).andExpect(status().isOk()).andReturn());
        List<String> ids = new ArrayList<>();
        p.get("contenu").forEach(n -> ids.add(n.get("id").asText()));
        return ids;
    }

    private JsonNode charge(String type, String agregat) throws Exception {
        return json.readTree(jdbc.queryForObject("select payload::text from outbox_event where event_type = ? and aggregate_id = ? order by created_at desc limit 1", String.class, type, agregat));
    }

    @Test
    @DisplayName("Offre sans unité : à prendre par les porteurs des profils, idempotente, prise réservée, pilotage.task-taken.v1")
    void offreSansUnite() throws Exception {
        Map<String, Object> t = offre("LIA-2026-001/3/R", List.of("ROLE_INGENIEUR_HOMOLOGATION", "ROLE_CHEF_DDRM"), null);
        String id = lire(
            mvc
                .perform(json(post(INTERNE).with(homologation()), t))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.etat").value("A_PRENDRE"))
                .andExpect(jsonPath("$.aPrendre").value(true))
                .andExpect(jsonPath("$.responsable").doesNotExist())
                .andExpect(jsonPath("$.uniteCible").doesNotExist())
                .andExpect(jsonPath("$.profilsRequis", Matchers.contains("ROLE_CHEF_DDRM", "ROLE_INGENIEUR_HOMOLOGATION")))
                .andExpect(jsonPath("$.createur.id").value("homologation-service"))
                .andReturn()
        )
            .get("id")
            .asText();
        // Idempotence par origine.reference seule.
        mvc.perform(json(post(INTERNE).with(homologation()), t)).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id));
        assertThat(compter("select count(*) from tache")).isEqualTo(1);
        JsonNode offerte = charge("pilotage.task-offered.v1", id);
        List<String> destinataires = new ArrayList<>();
        offerte.get("destinataires").forEach(n -> destinataires.add(n.asText()));
        assertThat(destinataires).containsExactlyInAnyOrder(AGENT, CHEF_DDRM);

        // « Mes tâches » : porteurs des profils seulement ; lecture refusée aux autres.
        assertThat(mesTaches(AGENT)).contains(id);
        assertThat(mesTaches(CHEF_DDRM)).contains(id);
        assertThat(mesTaches(AGENT_BIS)).doesNotContain(id);
        mvc.perform(get("/api/v1/taches/" + id).with(agent(AGENT))).andExpect(status().isOk()).andExpect(jsonPath("$.aPrendre").value(true));
        mvc.perform(get("/api/v1/taches/" + id).with(agent(AGENT_BIS))).andExpect(status().isForbidden());

        // Prise : sans l'un des profils, refus ; le porteur la prend.
        mvc.perform(post("/api/v1/taches/" + id + "/prise").with(agent(AGENT_BIS))).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("PIL_PROFIL_REQUIS"));
        mvc
            .perform(post("/api/v1/taches/" + id + "/prise").with(agent(CHEF_DDRM)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.etat").value("OUVERTE"))
            .andExpect(jsonPath("$.aPrendre").value(false))
            .andExpect(jsonPath("$.responsable.id").value(CHEF_DDRM));
        JsonNode prise = charge("pilotage.task-taken.v1", id);
        assertThat(prise.get("tacheId").asText()).isEqualTo(id);
        assertThat(prise.at("/origine/reference").asText()).isEqualTo("LIA-2026-001/3/R");
        assertThat(prise.at("/origine/service").asText()).isEqualTo("homologation");
        assertThat(prise.get("responsableId").asText()).isEqualTo(CHEF_DDRM);
        assertThat(prise.get("mode").asText()).isEqualTo("PRISE");
        mvc.perform(post("/api/v1/taches/" + id + "/prise").with(agent(AGENT))).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("PIL_TRANSITION_INTERDITE"));
        // D-138 : offre adressée à des profils, la prise (« je m'en occupe ») ne la retire pas des autres porteurs.
        assertThat(mesTaches(AGENT)).contains(id);
        assertThat(mesTaches(CHEF_DDRM)).contains(id);
        // Après la prise, la même demande rend la tâche prise.
        mvc.perform(json(post(INTERNE).with(homologation()), t)).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id)).andExpect(jsonPath("$.responsable.id").value(CHEF_DDRM));
    }

    @Test
    @DisplayName("Offre dans une unité : membres porteurs seulement ; clôture et réouverture sans avoir été prise")
    void offreDansUneUniteClotureEtReouverture() throws Exception {
        String id = lire(mvc.perform(json(post(INTERNE).with(homologation()), offre("LIA-2026-002/AFFECTER/SNA", List.of("ROLE_SNA", "ROLE_INGENIEUR_HOMOLOGATION"), "SNA"))).andExpect(status().isCreated()).andReturn())
            .get("id")
            .asText();
        assertThat(mesTaches(AGENT_BIS)).contains(id);
        assertThat(mesTaches(AGENT)).contains(id);
        assertThat(mesTaches(CHEF_DDRM)).doesNotContain(id);
        mvc.perform(post("/api/v1/taches/" + id + "/prise").with(responsable(CHEF_SERVICE))).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("PIL_PROFIL_REQUIS"));

        Map<String, Object> cible = Map.of("origine", Map.of("reference", "LIA-2026-002/AFFECTER/SNA"));
        mvc
            .perform(json(post(INTERNE + "/cloture").with(homologation()), cible))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(id))
            .andExpect(jsonPath("$[0].etat").value("TERMINEE"))
            .andExpect(jsonPath("$[0].responsable").doesNotExist());
        assertThat(mesTaches(AGENT_BIS)).doesNotContain(id);
        mvc
            .perform(json(post(INTERNE + "/reouverture").with(homologation()), cible))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].etat").value("A_PRENDRE"))
            .andExpect(jsonPath("$[0].aPrendre").value(true));
        assertThat(mesTaches(AGENT_BIS)).contains(id);
        mvc.perform(post("/api/v1/taches/" + id + "/prise").with(agent(AGENT_BIS))).andExpect(status().isOk()).andExpect(jsonPath("$.responsable.id").value(AGENT_BIS));
    }

    @Test
    @DisplayName("Offre refusée : responsable et profils ensemble, profils absents ou trop nombreux, code invalide, unité inconnue")
    void refus() throws Exception {
        Map<String, Object> deux = offre("LIA-2026-003/3/R", List.of("ROLE_SNA"), null);
        deux.put("responsableId", AGENT);
        mvc.perform(json(post(INTERNE).with(homologation()), deux)).andExpect(status().isBadRequest()).andExpect(jsonPath("$.details.champ").value("profilsRequis"));
        mvc.perform(json(post(INTERNE).with(homologation()), offre("LIA-2026-003/3/R", List.of(), null))).andExpect(status().isBadRequest()).andExpect(jsonPath("$.details.champ").value("profilsRequis"));
        List<String> trop = new ArrayList<>();
        for (int n = 0; n < 21; n++) {
            trop.add("ROLE_P" + n);
        }
        mvc.perform(json(post(INTERNE).with(homologation()), offre("LIA-2026-003/3/R", trop, null))).andExpect(status().isBadRequest());
        mvc.perform(json(post(INTERNE).with(homologation()), offre("LIA-2026-003/3/R", List.of("ROLE SNA"), null))).andExpect(status().isBadRequest());
        mvc
            .perform(json(post(INTERNE).with(homologation()), offre("LIA-2026-003/3/R", List.of("ROLE_SNA"), "INCONNUE")))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("PIL_UNITE_INCONNUE"));
        Map<String, Object> uniteSeule = offre("LIA-2026-003/3/R", null, "SNA");
        mvc.perform(json(post(INTERNE).with(homologation()), uniteSeule)).andExpect(status().isBadRequest()).andExpect(jsonPath("$.details.champ").value("profilsRequis"));
        assertThat(compter("select count(*) from tache")).isZero();
    }

    private static Map<String, Object> offreParPermission(String reference, List<String> permissions, String unite) {
        Map<String, Object> t = offre(reference, null, unite);
        t.remove("profilsRequis");
        t.put("permissionsRequises", permissions);
        return t;
    }

    @Test
    @DisplayName("Offre par permission : porteurs lus par permission, « Mes tâches », prise, attribution par le chef, task-taken")
    void offreParPermission() throws Exception {
        // Composition des profils (données de MS-IAM) : la permission caractérise la fonction, aucun profil n'est cité par l'offre.
        organisation.composerProfil("ROLE_SNA", "HOM_TEST_FONCTION");
        organisation.composerProfil("ROLE_INGENIEUR_HOMOLOGATION", "HOM_TEST_FONCTION");
        String id = lire(
            mvc
                .perform(json(post(INTERNE).with(homologation()), offreParPermission("LIA-2026-004/AFFECTER/SNA", List.of("HOM_TEST_FONCTION"), null)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.etat").value("A_PRENDRE"))
                .andExpect(jsonPath("$.profilsRequis", Matchers.empty()))
                .andExpect(jsonPath("$.permissionsRequises", Matchers.contains("HOM_TEST_FONCTION")))
                .andReturn()
        )
            .get("id")
            .asText();
        mvc.perform(json(post(INTERNE).with(homologation()), offreParPermission("LIA-2026-004/AFFECTER/SNA", List.of("HOM_TEST_FONCTION"), null))).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id));
        List<String> destinataires = new ArrayList<>();
        charge("pilotage.task-offered.v1", id).get("destinataires").forEach(n -> destinataires.add(n.asText()));
        assertThat(destinataires).containsExactlyInAnyOrder(AGENT, AGENT_BIS);
        assertThat(mesTaches(AGENT)).contains(id);
        assertThat(mesTaches(AGENT_BIS)).contains(id);
        assertThat(mesTaches(CHEF_DDRM)).doesNotContain(id);
        mvc.perform(get("/api/v1/taches/" + id).with(agent(CHEF_DDRM))).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/taches/" + id + "/prise").with(agent(CHEF_DDRM))).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("PIL_PROFIL_REQUIS"));
        mvc.perform(post("/api/v1/taches/" + id + "/prise").with(agent(AGENT_BIS))).andExpect(status().isOk()).andExpect(jsonPath("$.responsable.id").value(AGENT_BIS));
        JsonNode prise = charge("pilotage.task-taken.v1", id);
        assertThat(prise.get("responsableId").asText()).isEqualTo(AGENT_BIS);
        assertThat(prise.get("permissionsRequises").get(0).asText()).isEqualTo("HOM_TEST_FONCTION");
        assertThat(prise.get("profilsRequis")).isEmpty();
        assertThat(prise.at("/origine/reference").asText()).isEqualTo("LIA-2026-004/AFFECTER/SNA");

        // Dans une unité : attribution par le chef de l'unité, à un porteur de la permission seulement.
        String id2 = lire(mvc.perform(json(post(INTERNE).with(homologation()), offreParPermission("LIA-2026-005/AFFECTER/SNA", List.of("HOM_TEST_FONCTION"), "SNA"))).andExpect(status().isCreated()).andReturn())
            .get("id")
            .asText();
        mvc
            .perform(json(post("/api/v1/taches/" + id2 + "/attribution").with(responsable(CHEF_SERVICE)), Map.of("responsableId", CHEF_SERVICE)))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("PIL_RESPONSABLE_INVALIDE"));
        mvc
            .perform(json(post("/api/v1/taches/" + id2 + "/attribution").with(responsable(CHEF_SERVICE)), Map.of("responsableId", AGENT)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.responsable.id").value(AGENT));
        JsonNode attribution = charge("pilotage.task-taken.v1", id2);
        assertThat(attribution.get("mode").asText()).isEqualTo("ATTRIBUTION");
        assertThat(attribution.get("acteurId").asText()).isEqualTo(CHEF_SERVICE);

        // Clôture et réouverture sans responsable, comme pour une offre par profil.
        Map<String, Object> cible = Map.of("origine", Map.of("reference", "LIA-2026-005/AFFECTER/SNA"));
        mvc.perform(json(post(INTERNE + "/cloture").with(homologation()), cible)).andExpect(status().isOk()).andExpect(jsonPath("$[0].etat").value("TERMINEE"));
        mvc.perform(json(post(INTERNE + "/reouverture").with(homologation()), cible)).andExpect(status().isOk()).andExpect(jsonPath("$[0].etat").value("EN_COURS"));
        // Remise à prendre (aPrendre) : l'offre seule redevient à prendre, sans responsable, et est offerte de nouveau.
        mvc.perform(json(post(INTERNE + "/cloture").with(homologation()), cible)).andExpect(status().isOk());
        Map<String, Object> offreSeule = Map.of("origine", Map.of("reference", "LIA-2026-005/AFFECTER/SNA"), "aPrendre", true);
        mvc
            .perform(json(post(INTERNE + "/reouverture").with(homologation()), offreSeule))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].etat").value("A_PRENDRE"))
            .andExpect(jsonPath("$[0].responsable").doesNotExist());
        List<String> deNouveau = new ArrayList<>();
        charge("pilotage.task-offered.v1", id2).get("destinataires").forEach(n -> deNouveau.add(n.asText()));
        assertThat(deNouveau).containsExactlyInAnyOrder(AGENT, AGENT_BIS);
        assertThat(mesTaches(AGENT)).contains(id2);
        mvc.perform(json(post(INTERNE + "/cloture").with(homologation()), offreSeule)).andExpect(status().isOk()).andExpect(jsonPath("$[0].etat").value("TERMINEE"));
    }

    @Test
    @DisplayName("Offre par permission refusée : avec un responsable, liste vide, trop longue ou code invalide ; profils et permissions ensemble admis")
    void refusParPermission() throws Exception {
        Map<String, Object> deux = offreParPermission("LIA-2026-006/3/R", List.of("HOM_TEST_FONCTION"), null);
        deux.put("responsableId", AGENT);
        mvc.perform(json(post(INTERNE).with(homologation()), deux)).andExpect(status().isBadRequest()).andExpect(jsonPath("$.details.champ").value("permissionsRequises"));
        mvc
            .perform(json(post(INTERNE).with(homologation()), offreParPermission("LIA-2026-006/3/R", List.of(), null)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.details.champ").value("permissionsRequises"));
        List<String> trop = new ArrayList<>();
        for (int n = 0; n < 21; n++) {
            trop.add("HOM_P" + n);
        }
        mvc.perform(json(post(INTERNE).with(homologation()), offreParPermission("LIA-2026-006/3/R", trop, null))).andExpect(status().isBadRequest());
        mvc
            .perform(json(post(INTERNE).with(homologation()), offreParPermission("LIA-2026-006/3/R", List.of("hom_minuscule"), null)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.details.champ").value("permissionsRequises"));
        assertThat(compter("select count(*) from tache")).isZero();
        // Les deux listes : l'un des profils ou l'une des permissions suffit.
        organisation.composerProfil("ROLE_SNA", "HOM_TEST_FONCTION");
        Map<String, Object> mixte = offre("LIA-2026-006/3/R", List.of("ROLE_CHEF_DDRM"), null);
        mixte.put("permissionsRequises", List.of("HOM_TEST_FONCTION"));
        String id = lire(mvc.perform(json(post(INTERNE).with(homologation()), mixte)).andExpect(status().isCreated()).andReturn()).get("id").asText();
        assertThat(mesTaches(CHEF_DDRM)).contains(id);
        assertThat(mesTaches(AGENT_BIS)).contains(id);
        assertThat(mesTaches(AGENT)).doesNotContain(id);
    }
}
