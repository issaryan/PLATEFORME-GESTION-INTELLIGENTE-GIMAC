package cm.gimac.des.pilotage.web.rest.processus;

import static cm.gimac.des.pilotage.infrastructure.client.OrganisationStubAdapter.CHEF_DES;
import static cm.gimac.des.pilotage.infrastructure.client.OrganisationStubAdapter.CHEF_RESEAU;
import static cm.gimac.des.pilotage.infrastructure.client.OrganisationStubAdapter.INGENIEUR_RESEAU;
import static cm.gimac.des.pilotage.infrastructure.client.OrganisationStubAdapter.PROFIL_RESEAU;
import static cm.gimac.des.pilotage.infrastructure.client.OrganisationStubAdapter.TECHNICIEN_RESEAU;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cm.gimac.des.pilotage.application.port.in.InstancesProcessusUseCase;
import cm.gimac.des.pilotage.infrastructure.client.OrganisationStubAdapter.Compte;
import cm.gimac.des.pilotage.infrastructure.client.OrganisationStubAdapter.Unite;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Processus d'unité et organisation (D-93 §6) : tâches au validateur et aux signataires attendus (à prendre par
 * profil, prise qui désigne le signataire), réaction au retrait de profil, au changement d'unité et au changement de
 * chef d'unité d'un intervenant en cours d'étape, annulation des tâches d'attente.
 */
class ProcessusOrganisationIT extends AbstractProcessusIT {

    private static final String CHEF_BIS = "00000000-0000-4000-8000-0000000d0099";

    @Autowired
    private InstancesProcessusUseCase processus;

    /** Une étape : exécutant par profil, livrable DOC, signature VISA par profil, validation par le chef d'unité. */
    private String publieEtapeUnique(String code) throws Exception {
        Map<String, Object> etape = new HashMap<>();
        etape.put("code", "UNIQUE");
        etape.put("libelle", "Étape unique");
        etape.put("action", "Rédiger et faire viser");
        etape.put("intervenant", Map.of("mode", "PROFIL", "profil", PROFIL_RESEAU));
        etape.put("validateur", Map.of("mode", "CHEF_UNITE", "remplacement", "ROLE_RESPONSABLE_SERVICE"));
        etape.put("livrables", List.of(Map.of("code", "DOC", "libelle", "Document", "typesPieces", List.of("application/pdf"), "obligatoire", true)));
        etape.put("signatures", List.of(Map.of("code", "VISA", "libelle", "Visa technique", "signataire", Map.of("mode", "PROFIL", "profil", PROFIL_RESEAU), "porte", List.of("DOC"), "ordre", 1)));
        etape.put("delaiJours", 3);
        Map<String, Object> d = new HashMap<>(json.convertValue(definition(code), Map.class));
        d.put("etapes", List.of(etape));
        String id = creer(chef(CHEF_RESEAU), d);
        publier(chef(CHEF_RESEAU), id);
        return id;
    }

    private String lancer(String definition) throws Exception {
        return lire(mvc.perform(json(post(BASE + "/" + definition + "/instances").with(agent(TECHNICIEN_RESEAU)), Map.of("intitule", "Essai organisation"))).andExpect(status().isCreated()).andReturn())
            .get("id")
            .asText();
    }

    private JsonNode instance(String id, String qui) throws Exception {
        return lire(mvc.perform(get(INSTANCES + "/" + id).with(agent(qui))).andExpect(status().isOk()).andReturn());
    }

    private Map<String, Object> tache(String reference) {
        return jdbc.queryForMap("select id::text as id, etat, responsable_id, profil_requis, unite_cible from tache where origine_reference = ?", reference);
    }

    private String url(String instance, String acte) {
        return INSTANCES + "/" + instance + "/etapes/UNIQUE/" + acte;
    }

    @Test
    @DisplayName("Profil retiré puis changement d'unité de l'exécutant : l'étape repasse à prendre par le profil, une fois")
    void profilRetireEtChangementUnite() throws Exception {
        // Deux porteurs de plus au pôle : après chaque départ du rôle, plusieurs porteurs restent dans l'unité (étape à
        // prendre) ; avec un seul, l'étape lui serait confiée (désignation hors de l'unité).
        for (String n : List.of("0098", "0099")) {
            String autre = "00000000-0000-4000-8000-00000000" + n;
            organisation.enregistrer(new Compte(autre, "Renfort " + n, "Technicien réseau", "POLE_RESEAU", CHEF_RESEAU, true, true));
            organisation.attribuerProfils(autre, PROFIL_RESEAU);
        }
        String id = lancer(publieEtapeUnique("ORGA_PROFIL"));
        String tacheEtape = tache(id + "/UNIQUE").get("id").toString();
        mvc.perform(post("/api/v1/taches/" + tacheEtape + "/prise").with(agent(INGENIEUR_RESEAU))).andExpect(status().isOk());

        // Retrait du profil : plus exécutante, étape à prendre.
        organisation.attribuerProfils(INGENIEUR_RESEAU, "ROLE_COLLABORATEUR");
        assertThat(processus.traiterChangementCompte(INGENIEUR_RESEAU, "PROFIL_RETIRE", "admin-1")).isEqualTo(1);
        assertThat(tache(id + "/UNIQUE")).containsEntry("etat", "A_PRENDRE").containsEntry("profil_requis", PROFIL_RESEAU).containsEntry("unite_cible", "POLE_RESEAU");
        JsonNode i = instance(id, CHEF_RESEAU);
        assertThat(etape(i, "UNIQUE").get("executant").isNull()).isTrue();
        assertThat(i.get("historique").findValuesAsText("action")).contains("ORGANISATION");
        JsonNode e = charge("pilotage.process-step-reassigned.v1", id);
        assertThat(e.get("cause").asText()).isEqualTo("PROFIL_RETIRE");
        assertThat(textes(e.get("roles"))).containsExactly("EXECUTANT");
        assertThat(e.get("ancienId").asText()).isEqualTo(INGENIEUR_RESEAU);
        assertThat(textes(e.get("destinataires"))).contains(TECHNICIEN_RESEAU);
        // Rejeu : rien de plus.
        assertThat(processus.traiterChangementCompte(INGENIEUR_RESEAU, "PROFIL_RETIRE", "admin-1")).isZero();

        // Le technicien la prend, puis change d'unité : elle repasse à prendre.
        mvc.perform(post("/api/v1/taches/" + tacheEtape + "/prise").with(agent(TECHNICIEN_RESEAU))).andExpect(status().isOk());
        organisation.enregistrer(new Compte(TECHNICIEN_RESEAU, "Yves Ateba", "Technicien", "SNA", CHEF_DES, true, true));
        assertThat(processus.traiterChangementCompte(TECHNICIEN_RESEAU, "CHANGEMENT_UNITE", null)).isEqualTo(1);
        assertThat(tache(id + "/UNIQUE")).containsEntry("etat", "A_PRENDRE").containsEntry("responsable_id", null);
        assertThat(charge("pilotage.process-step-reassigned.v1", id).get("cause").asText()).isEqualTo("CHANGEMENT_UNITE");
    }

    @Test
    @DisplayName("Signature et validation : tâches aux signataires et au validateur, prise, nouveau chef d'unité, clôture")
    void tachesDAttenteEtNouveauChef() throws Exception {
        String id = lancer(publieEtapeUnique("ORGA_ATTENTE"));
        String tacheEtape = tache(id + "/UNIQUE").get("id").toString();
        mvc.perform(post("/api/v1/taches/" + tacheEtape + "/prise").with(agent(INGENIEUR_RESEAU))).andExpect(status().isOk());
        String doc = deposer(id, INGENIEUR_RESEAU, "doc.pdf", "application/pdf");
        mvc.perform(json(post(url(id, "livrables")).with(agent(INGENIEUR_RESEAU)), Map.of("livrable", "DOC", "documentId", doc))).andExpect(status().isOk());
        mvc.perform(post(url(id, "fin")).with(agent(INGENIEUR_RESEAU))).andExpect(status().isOk()).andExpect(jsonPath("$.etapes[0].etat").value("A_SIGNER"));

        // Signature attendue d'un profil : tâche à prendre, visible des porteurs dans « Mes tâches ».
        String refVisa = id + "/UNIQUE/SIGNATURE/VISA";
        Map<String, Object> visa = tache(refVisa);
        assertThat(visa).containsEntry("etat", "A_PRENDRE").containsEntry("profil_requis", PROFIL_RESEAU).containsEntry("unite_cible", "POLE_RESEAU");
        String tVisa = visa.get("id").toString();
        mvc.perform(get("/api/v1/taches").param("vue", "moi").with(agent(TECHNICIEN_RESEAU))).andExpect(jsonPath("$.contenu[*].id", Matchers.hasItem(tVisa)));
        mvc.perform(get("/api/v1/taches").param("vue", "moi").with(chef(CHEF_RESEAU))).andExpect(jsonPath("$.contenu[*].id", Matchers.not(Matchers.hasItem(tVisa))));
        // La prise désigne le signataire : lui seul signe.
        mvc.perform(post("/api/v1/taches/" + tVisa + "/prise").with(agent(TECHNICIEN_RESEAU))).andExpect(status().isOk());
        JsonNode i = instance(id, TECHNICIEN_RESEAU);
        assertThat(etape(i, "UNIQUE").at("/signatures/0/attendu/personne/id").asText()).isEqualTo(TECHNICIEN_RESEAU);
        assertThat(i.get("historique").findValuesAsText("detail")).anyMatch(d -> d != null && d.startsWith("Signature VISA prise par"));
        mvc
            .perform(json(post(url(id, "signature")).with(agent(INGENIEUR_RESEAU)).header("X-Reauth-Assertion", "assertion"), Map.of("signature", "VISA")))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("PIL_PROCESSUS_NON_SIGNATAIRE"));
        mvc
            .perform(json(post(url(id, "signature")).with(agent(TECHNICIEN_RESEAU)).header("X-Reauth-Assertion", "assertion"), Map.of("signature", "VISA")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.etapes[0].etat").value("A_VALIDER"));
        assertThat(tache(refVisa)).containsEntry("etat", "TERMINEE").containsEntry("responsable_id", TECHNICIEN_RESEAU);

        // Validation par le chef d'unité : tâche à lui ; nouveau chef : la tâche et la validation le suivent.
        String refValidation = id + "/UNIQUE/VALIDATION";
        assertThat(tache(refValidation)).containsEntry("etat", "OUVERTE").containsEntry("responsable_id", CHEF_RESEAU);
        organisation.enregistrer(new Compte(CHEF_BIS, "Nadia Fouda", "Cheffe du pôle réseau", "POLE_RESEAU", CHEF_DES, true, true));
        organisation.attribuerProfils(CHEF_BIS, "ROLE_RESPONSABLE_SERVICE");
        organisation.enregistrerUnite(new Unite("POLE_RESEAU", "PROD_INFO", CHEF_BIS));
        assertThat(processus.traiterChangementChef("POLE_RESEAU", "admin-1")).isEqualTo(1);
        assertThat(processus.traiterChangementChef("POLE_RESEAU", "admin-1")).isZero();
        assertThat(tache(refValidation)).containsEntry("etat", "OUVERTE").containsEntry("responsable_id", CHEF_BIS);
        JsonNode e = charge("pilotage.process-step-reassigned.v1", id);
        assertThat(e.get("cause").asText()).isEqualTo("CHANGEMENT_CHEF");
        assertThat(textes(e.get("roles"))).containsExactly("VALIDATEUR");
        assertThat(e.get("executantId").asText()).isEqualTo(CHEF_BIS);
        mvc
            .perform(json(post(url(id, "validation")).with(chef(CHEF_RESEAU)), Map.of("decision", "VALIDER")))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("PIL_PROCESSUS_NON_VALIDATEUR"));
        mvc.perform(json(post(url(id, "validation")).with(chef(CHEF_BIS)), Map.of("decision", "VALIDER"))).andExpect(status().isOk()).andExpect(jsonPath("$.etat").value("CLOSE"));
        assertThat(tache(refValidation)).containsEntry("etat", "TERMINEE").containsEntry("responsable_id", CHEF_BIS);
    }

    @Test
    @DisplayName("Annulation de l'instance : tâches d'attente annulées")
    void annulationDesAttentes() throws Exception {
        String id = lancer(publieEtapeUnique("ORGA_ANNULEE"));
        String tacheEtape = tache(id + "/UNIQUE").get("id").toString();
        mvc.perform(post("/api/v1/taches/" + tacheEtape + "/prise").with(agent(INGENIEUR_RESEAU))).andExpect(status().isOk());
        String doc = deposer(id, INGENIEUR_RESEAU, "doc.pdf", "application/pdf");
        mvc.perform(json(post(url(id, "livrables")).with(agent(INGENIEUR_RESEAU)), Map.of("livrable", "DOC", "documentId", doc))).andExpect(status().isOk());
        mvc.perform(post(url(id, "fin")).with(agent(INGENIEUR_RESEAU))).andExpect(status().isOk());
        mvc.perform(json(post(INSTANCES + "/" + id + "/annulation").with(chef(CHEF_RESEAU)), Map.of("motif", "Abandon"))).andExpect(status().isOk());
        assertThat(tache(id + "/UNIQUE/SIGNATURE/VISA")).containsEntry("etat", "ANNULEE");
        assertThat(tache(id + "/UNIQUE")).containsEntry("etat", "ANNULEE");
    }
}
