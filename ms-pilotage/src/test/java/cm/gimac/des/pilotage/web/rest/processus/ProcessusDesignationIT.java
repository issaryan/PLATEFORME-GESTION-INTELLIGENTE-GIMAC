package cm.gimac.des.pilotage.web.rest.processus;

import static cm.gimac.des.pilotage.infrastructure.client.OrganisationStubAdapter.AGENT;
import static cm.gimac.des.pilotage.infrastructure.client.OrganisationStubAdapter.AGENT_BIS;
import static cm.gimac.des.pilotage.infrastructure.client.OrganisationStubAdapter.CHEF_DDRM;
import static cm.gimac.des.pilotage.infrastructure.client.OrganisationStubAdapter.CHEF_DES;
import static cm.gimac.des.pilotage.infrastructure.client.OrganisationStubAdapter.CHEF_RESEAU;
import static cm.gimac.des.pilotage.infrastructure.client.OrganisationStubAdapter.INACTIF;
import static cm.gimac.des.pilotage.infrastructure.client.OrganisationStubAdapter.INGENIEUR_RESEAU;
import static cm.gimac.des.pilotage.infrastructure.client.OrganisationStubAdapter.TECHNICIEN_RESEAU;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cm.gimac.des.pilotage.application.port.in.InstancesProcessusUseCase;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Désignation hors de l'unité (D-87, docs/lot1/processus-unite.md « Désignation hors de l'unité ») : un profil
 * exigé que personne ne porte dans l'unité désignée mais que d'autres portent ailleurs dans le GIMAC ne bloque ni
 * la publication, ni le lancement, ni la réaffectation. Un seul porteur : affectation automatique ; plusieurs :
 * tâche à prendre offerte à tous, le chef ou le lanceur peut choisir ; aucun : le chef de l'unité reçoit l'étape
 * pour désigner. Organisation simulée : seul Serge Ndzana (service SNA) porte ROLE_INGENIEUR_HOMOLOGATION.
 */
class ProcessusDesignationIT extends AbstractProcessusIT {

    private static final String HOMOLOGATION = "ROLE_INGENIEUR_HOMOLOGATION";

    @Autowired
    private InstancesProcessusUseCase instances;

    /** Processus du pôle réseau : TRAITEMENT par profil, puis MISE_EN_OEUVRE désignée au lancement (remplacement : le même profil). */
    private ObjectNode definitionHorsUnite(String code, String profil) {
        ObjectNode d = json.createObjectNode();
        d.put("code", code);
        d.put("unite", "POLE_RESEAU");
        d.put("libelle", "Avis d'homologation");
        ArrayNode etapes = d.putArray("etapes");
        ObjectNode traitement = etapes.addObject();
        traitement.put("code", "TRAITEMENT");
        traitement.put("libelle", "Traitement");
        traitement.put("delaiJours", 2);
        traitement.putObject("intervenant").put("mode", "PROFIL").put("profil", profil);
        ObjectNode mise = etapes.addObject();
        mise.put("code", "MISE_EN_OEUVRE");
        mise.put("libelle", "Mise en œuvre");
        mise.put("delaiJours", 3);
        mise.putArray("predecesseurs").add("TRAITEMENT");
        mise.putObject("intervenant").put("mode", "AU_LANCEMENT").put("remplacement", profil);
        return d;
    }

    private String publieHorsUnite(String code, String profil) throws Exception {
        String id = creer(chef(CHEF_RESEAU), definitionHorsUnite(code, profil));
        mvc
            .perform(json(post(BASE + "/" + id + "/publication").with(chef(CHEF_RESEAU)), Map.of("motif", "Mise en service", "leverAvertissements", true)))
            .andExpect(status().isOk());
        return id;
    }

    private static Map<String, Object> lancement(String designe) {
        Map<String, Object> l = new HashMap<>();
        l.put("intitule", "Avis sur le terminal TPE-42");
        l.put("designations", Map.of("MISE_EN_OEUVRE", designe));
        return l;
    }

    private String url(String instance, String etape, String acte) {
        return urlEtape(instance, etape, acte);
    }

    @Test
    @DisplayName("Publication : porteurs du profil hors de l'unité = simple information, publication sans levée ni motif")
    void publicationHorsUnite() throws Exception {
        String id = creer(chef(CHEF_RESEAU), definitionHorsUnite("AVIS_HORS_UNITE", HOMOLOGATION));
        JsonNode c = lire(mvc.perform(post(BASE + "/" + id + "/controle").with(chef(CHEF_RESEAU))).andExpect(status().isOk()).andReturn());
        assertThat(c.get("publiable").asBoolean()).isTrue();
        assertThat(c.get("anomalies").findValuesAsText("code")).containsOnly("COUVERTURE_HORS_UNITE");
        assertThat(c.get("anomalies").findValuesAsText("gravite")).containsOnly("AVERTISSEMENT");
        assertThat(c.at("/anomalies/0/message").asText()).contains("1 porteur").contains("SNA");
        mvc
            .perform(json(post(BASE + "/" + id + "/publication").with(chef(CHEF_RESEAU)), Map.of()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.etat").value("PUBLIEE"))
            .andExpect(jsonPath("$.avertissementsLeves").value(false))
            .andExpect(jsonPath("$.avertissements[0].code").value("COUVERTURE_HORS_UNITE"))
            .andExpect(jsonPath("$.avertissements[0].gravite").value("AVERTISSEMENT"));

        // Personne nulle part : l'anomalie reste levable.
        String vide = creer(chef(CHEF_RESEAU), definitionHorsUnite("AVIS_SANS_PORTEUR", "ROLE_POLE_SYSTEME"));
        c = lire(mvc.perform(post(BASE + "/" + vide + "/controle").with(chef(CHEF_RESEAU))).andExpect(status().isOk()).andReturn());
        assertThat(c.get("anomalies").findValuesAsText("code")).containsOnly("COUVERTURE_PROFIL");
        assertThat(c.get("anomalies").findValuesAsText("gravite")).containsOnly("LEVABLE");
        mvc
            .perform(json(post(BASE + "/" + vide + "/publication").with(chef(CHEF_RESEAU)), Map.of()))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("PIL_PROCESSUS_AVERTISSEMENTS"));
    }

    @Test
    @DisplayName("Un seul porteur ailleurs : désignation admise hors de l'unité et étape confiée automatiquement")
    void seulPorteurAilleurs() throws Exception {
        String processus = publieHorsUnite("AVIS_SEUL", HOMOLOGATION);
        // Désignation : personne active sans le profil, ou inactive : refus maintenu.
        mvc
            .perform(json(post(BASE + "/" + processus + "/instances").with(agent(TECHNICIEN_RESEAU)), lancement(CHEF_DDRM)))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("PIL_PROCESSUS_DESIGNATION_INVALIDE"));
        mvc
            .perform(json(post(BASE + "/" + processus + "/instances").with(agent(TECHNICIEN_RESEAU)), lancement(INACTIF)))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("PIL_PROCESSUS_DESIGNATION_INVALIDE"));
        JsonNode i = lire(
            mvc.perform(json(post(BASE + "/" + processus + "/instances").with(agent(TECHNICIEN_RESEAU)), lancement(AGENT))).andExpect(status().isCreated()).andReturn()
        );
        assertThat(i.at("/designations/" + etapeDe(processus, "MISE_EN_OEUVRE") + "/id").asText()).isEqualTo(AGENT);
        JsonNode traitement = etape(i, "TRAITEMENT");
        assertThat(traitement.get("etat").asText()).isEqualTo("ACTIVE");
        assertThat(traitement.at("/executant/id").asText()).isEqualTo(AGENT);
        assertThat(traitement.get("aPrendre").isNull()).isTrue();
        assertThat(i.get("historique").findValuesAsText("detail")).anyMatch(x -> x.contains("hors de l'unité"));
        // L'exécutant hors de l'unité lit l'instance et agit sur l'étape.
        mvc.perform(get(INSTANCES + "/" + i.get("id").asText()).with(agent(AGENT))).andExpect(status().isOk()).andExpect(jsonPath("$.etapes[0].moi.executer").value(true));
    }

    @Test
    @DisplayName("Plusieurs porteurs ailleurs : tâche offerte à tous, prise, choix par le chef ou le lanceur, candidats")
    void plusieursPorteursAilleurs() throws Exception {
        organisation.attribuerProfils(AGENT_BIS, "ROLE_SNA", HOMOLOGATION);
        String processus = publieHorsUnite("AVIS_PLUSIEURS", HOMOLOGATION);
        JsonNode i = lire(
            mvc.perform(json(post(BASE + "/" + processus + "/instances").with(agent(TECHNICIEN_RESEAU)), lancement(AGENT_BIS))).andExpect(status().isCreated()).andReturn()
        );
        String id = i.get("id").asText();
        JsonNode traitement = etape(i, "TRAITEMENT");
        assertThat(traitement.get("executant").isNull()).isTrue();
        assertThat(traitement.at("/aPrendre/profil").asText()).isEqualTo(HOMOLOGATION);
        assertThat(traitement.at("/aPrendre/unite").isNull()).isTrue();
        String tache = traitement.get("tacheId").asText();
        JsonNode active = charge("pilotage.process-step-activated.v1", id);
        assertThat(textes(active.get("destinataires"))).containsExactlyInAnyOrder(AGENT, AGENT_BIS);

        // Candidats de l'étape : personne dans l'unité, deux ailleurs avec leur unité ; réservés au chef et au lanceur.
        JsonNode candidats = lire(mvc.perform(get(url(id, "TRAITEMENT", "candidats")).with(chef(CHEF_RESEAU))).andExpect(status().isOk()).andReturn());
        assertThat(candidats.get("profil").asText()).isEqualTo(HOMOLOGATION);
        assertThat(candidats.get("unite").asText()).isEqualTo("POLE_RESEAU");
        assertThat(candidats.get("dansUnite")).isEmpty();
        assertThat(candidats.get("ailleurs").findValuesAsText("id")).containsExactlyInAnyOrder(AGENT, AGENT_BIS);
        assertThat(candidats.get("ailleurs").findValuesAsText("unite")).containsOnly("SNA");
        mvc.perform(get(url(id, "TRAITEMENT", "candidats")).with(agent(TECHNICIEN_RESEAU))).andExpect(status().isOk());
        mvc.perform(get(url(id, "TRAITEMENT", "candidats")).with(agent(INGENIEUR_RESEAU))).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("PIL_PROCESSUS_INTERDIT"));

        // Le porteur hors de l'unité voit la tâche dans « Mes tâches » et la prend.
        JsonNode miennes = lire(mvc.perform(get("/api/v1/taches").param("vue", "moi").with(agent(AGENT_BIS))).andExpect(status().isOk()).andReturn());
        assertThat(miennes.get("contenu").findValuesAsText("id")).contains(tache);
        mvc.perform(get("/api/v1/taches/" + tache).with(agent(AGENT_BIS))).andExpect(status().isOk());
        mvc.perform(post("/api/v1/taches/" + tache + "/prise").with(agent(INGENIEUR_RESEAU))).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/taches/" + tache + "/prise").with(agent(AGENT_BIS))).andExpect(status().isOk());
        i = lire(mvc.perform(get(INSTANCES + "/" + id).with(agent(AGENT_BIS))).andExpect(status().isOk()).andReturn());
        assertThat(etape(i, "TRAITEMENT").at("/executant/id").asText()).isEqualTo(AGENT_BIS);
        assertThat(i.get("historique").findValuesAsText("action")).contains("ETAPE_PRISE");

        // Le chef de l'unité réaffecte à un porteur hors de l'unité ; personne sans le rôle ou inactive : refus.
        mvc
            .perform(json(post(url(id, "TRAITEMENT", "reaffectation")).with(chef(CHEF_RESEAU)), Map.of("personneId", CHEF_DDRM, "motif", "Essai")))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("PIL_PROCESSUS_PERSONNE_INVALIDE"));
        mvc
            .perform(json(post(url(id, "TRAITEMENT", "reaffectation")).with(chef(CHEF_RESEAU)), Map.of("personneId", INACTIF, "motif", "Essai")))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("PIL_PROCESSUS_PERSONNE_INVALIDE"));
        i = lire(
            mvc
                .perform(json(post(url(id, "TRAITEMENT", "reaffectation")).with(chef(CHEF_RESEAU)), Map.of("personneId", AGENT, "motif", "Plan de charge")))
                .andExpect(status().isOk())
                .andReturn()
        );
        assertThat(etape(i, "TRAITEMENT").at("/executant/id").asText()).isEqualTo(AGENT);

        // Seconde instance : le lanceur choisit lui-même parmi les porteurs tant que l'étape est à prendre.
        JsonNode j = lire(
            mvc.perform(json(post(BASE + "/" + processus + "/instances").with(agent(TECHNICIEN_RESEAU)), lancement(AGENT))).andExpect(status().isCreated()).andReturn()
        );
        assertThat(etape(j, "TRAITEMENT").at("/moi/reaffecter").asBoolean()).isTrue();
        String jid = j.get("id").asText();
        j = lire(
            mvc
                .perform(json(post(url(jid, "TRAITEMENT", "reaffectation")).with(agent(TECHNICIEN_RESEAU)), Map.of("personneId", AGENT, "motif", "Dossier connu")))
                .andExpect(status().isOk())
                .andReturn()
        );
        assertThat(etape(j, "TRAITEMENT").at("/executant/id").asText()).isEqualTo(AGENT);
        assertThat(etape(j, "TRAITEMENT").at("/moi/reaffecter").asBoolean()).isFalse();
        mvc
            .perform(json(post(url(jid, "TRAITEMENT", "reaffectation")).with(agent(TECHNICIEN_RESEAU)), Map.of("personneId", AGENT_BIS, "motif", "Retour")))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Aucun porteur nulle part : lancement accepté, étape confiée au chef de l'unité pour désigner (chef initiateur : son responsable)")
    void aucunPorteur() throws Exception {
        String processus = publieHorsUnite("AVIS_AUCUN", "ROLE_POLE_SYSTEME");
        // D-99 : le lanceur ne se désigne plus lui-même ; le chef désigne un membre de sa portée.
        mvc
            .perform(json(post(BASE + "/" + processus + "/instances").with(agent(TECHNICIEN_RESEAU)), lancement(TECHNICIEN_RESEAU)))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("PIL_PROCESSUS_SEPARATION_FONCTIONS"));
        JsonNode i = lire(
            mvc.perform(json(post(BASE + "/" + processus + "/instances").with(chef(CHEF_RESEAU)), lancement(TECHNICIEN_RESEAU))).andExpect(status().isCreated()).andReturn()
        );
        // Le chef de l'unité est l'initiateur : son responsable (chef du DES) reçoit l'étape pour désigner.
        assertThat(etape(i, "TRAITEMENT").at("/executant/id").asText()).isEqualTo(CHEF_DES);
        assertThat(i.get("historique").findValuesAsText("detail")).anyMatch(x -> x.contains("hors de l'initiateur") && x.contains("désigner"));
    }

    @Test
    @DisplayName("Candidats d'un intervenant pour le modélisateur et le lancement : dans l'unité d'abord, ailleurs ensuite")
    void candidats() throws Exception {
        organisation.attribuerProfils(AGENT_BIS, "ROLE_SNA", "ROLE_POLE_RESEAU");
        JsonNode r = lire(
            mvc.perform(get(BASE + "/candidats").param("unite", "POLE_RESEAU").param("profil", "ROLE_POLE_RESEAU").with(agent(TECHNICIEN_RESEAU))).andExpect(status().isOk()).andReturn()
        );
        assertThat(r.get("dansUnite").findValuesAsText("id")).containsExactlyInAnyOrder(INGENIEUR_RESEAU, TECHNICIEN_RESEAU);
        assertThat(r.get("ailleurs").findValuesAsText("id")).containsExactly(AGENT_BIS);
        assertThat(r.at("/ailleurs/0/unite").asText()).isEqualTo("SNA");
        assertThat(r.at("/ailleurs/0/nomAffiche").asText()).isEqualTo("Clarisse Bilong");
        r = lire(mvc.perform(get(BASE + "/candidats").param("unite", "POLE_RESEAU").with(agent(TECHNICIEN_RESEAU))).andExpect(status().isOk()).andReturn());
        assertThat(r.get("dansUnite").findValuesAsText("id")).contains(CHEF_RESEAU, INGENIEUR_RESEAU, TECHNICIEN_RESEAU);
        assertThat(r.get("ailleurs")).isEmpty();
        mvc
            .perform(get(BASE + "/candidats").param("unite", "UNITE_FANTOME").param("profil", "ROLE_POLE_RESEAU").with(agent(TECHNICIEN_RESEAU)))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("PIL_PROCESSUS_UNITE_INCONNUE"));
        mvc.perform(get(BASE + "/candidats").param("unite", "POLE_RESEAU").param("profil", "pas un profil !").with(agent(TECHNICIEN_RESEAU))).andExpect(status().isBadRequest());
        mvc.perform(get(BASE + "/candidats").param("profil", "ROLE_POLE_RESEAU").with(agent(TECHNICIEN_RESEAU))).andExpect(status().isBadRequest());
        mvc.perform(get(BASE + "/candidats").param("unite", "POLE_RESEAU").with(utilisateur(TECHNICIEN_RESEAU))).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Revue d'organisation hors de l'unité : le porteur hors de l'unité garde l'étape, la perd avec son profil")
    void revueHorsUnite() throws Exception {
        organisation.attribuerProfils(AGENT_BIS, "ROLE_SNA", HOMOLOGATION);
        String processus = publieHorsUnite("AVIS_REVUE", HOMOLOGATION);
        JsonNode i = lire(
            mvc.perform(json(post(BASE + "/" + processus + "/instances").with(agent(TECHNICIEN_RESEAU)), lancement(AGENT))).andExpect(status().isCreated()).andReturn()
        );
        String id = i.get("id").asText();
        String tache = etape(i, "TRAITEMENT").get("tacheId").asText();
        mvc.perform(post("/api/v1/taches/" + tache + "/prise").with(agent(AGENT))).andExpect(status().isOk());
        // Changement d'unité sans perte du profil : personne ne le porte au pôle, il reste exécutant.
        assertThat(instances.traiterChangementCompte(AGENT, "CHANGEMENT_UNITE", null)).isZero();
        // Profil retiré : seule Clarisse Bilong le porte encore, l'étape lui est confiée.
        organisation.attribuerProfils(AGENT, "ROLE_COLLABORATEUR");
        assertThat(instances.traiterChangementCompte(AGENT, "PROFIL_RETIRE", "admin-1")).isEqualTo(1);
        i = lire(mvc.perform(get(INSTANCES + "/" + id).with(chef(CHEF_RESEAU))).andExpect(status().isOk()).andReturn());
        assertThat(etape(i, "TRAITEMENT").at("/executant/id").asText()).isEqualTo(AGENT_BIS);
        assertThat(i.get("historique").findValuesAsText("action")).contains("ORGANISATION");
    }
}
