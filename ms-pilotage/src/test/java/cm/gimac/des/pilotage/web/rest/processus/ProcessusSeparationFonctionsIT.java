package cm.gimac.des.pilotage.web.rest.processus;

import static cm.gimac.des.pilotage.infrastructure.client.OrganisationStubAdapter.AGENT;
import static cm.gimac.des.pilotage.infrastructure.client.OrganisationStubAdapter.CHEF_DDRM;
import static cm.gimac.des.pilotage.infrastructure.client.OrganisationStubAdapter.CHEF_RESEAU;
import static cm.gimac.des.pilotage.infrastructure.client.OrganisationStubAdapter.CHEF_SERVICE;
import static cm.gimac.des.pilotage.infrastructure.client.OrganisationStubAdapter.INGENIEUR_RESEAU;
import static cm.gimac.des.pilotage.infrastructure.client.OrganisationStubAdapter.PROFIL_RESEAU;
import static cm.gimac.des.pilotage.infrastructure.client.OrganisationStubAdapter.TECHNICIEN_RESEAU;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Recette du travail quotidien (docs/recette/quotidien.md) côté MS-PILOTAGE : séparation des fonctions (QUO-02, D-99),
 * lecture de l'instance par les porteurs d'une étape à prendre (QUO-03), pièces et livrables téléchargeables (QUO-04).
 * Organisation simulée : pôle réseau (chef, ingénieure et technicien portant ROLE_POLE_RESEAU), service SNA.
 */
class ProcessusSeparationFonctionsIT extends AbstractProcessusIT {

    private static final String SEPARATION = "PIL_PROCESSUS_SEPARATION_FONCTIONS";

    /** Processus d'une unité : TRAITEMENT par profil (livrable NOTE facultatif), puis CONTROLE désigné au lancement. */
    private ObjectNode definition(String code, String unite, String profil, boolean initiateurIntervenant) {
        return definition(code, unite, profil, initiateurIntervenant, true);
    }

    private ObjectNode definition(String code, String unite, String profil, boolean initiateurIntervenant, boolean controleAuLancement) {
        ObjectNode d = json.createObjectNode();
        d.put("code", code);
        d.put("unite", unite);
        d.put("libelle", "Demande de séparation");
        d.putObject("initiateurs").put("initiateurIntervenant", initiateurIntervenant).putArray("regles").add("TOUT_LE_GIMAC");
        ArrayNode etapes = d.putArray("etapes");
        ObjectNode traitement = etapes.addObject();
        traitement.put("code", "TRAITEMENT");
        traitement.put("libelle", "Traitement");
        traitement.put("delaiJours", 2);
        traitement.putObject("intervenant").put("mode", "PROFIL").put("profil", profil);
        ObjectNode note = traitement.putArray("livrables").addObject();
        note.put("code", "NOTE");
        note.put("libelle", "Note");
        note.put("obligatoire", false);
        note.putArray("typesPieces").add("application/pdf");
        if (!controleAuLancement) {
            return d;
        }
        ObjectNode controle = etapes.addObject();
        controle.put("code", "CONTROLE");
        controle.put("libelle", "Contrôle");
        controle.put("delaiJours", 1);
        controle.putArray("predecesseurs").add("TRAITEMENT");
        controle.putObject("intervenant").put("mode", "AU_LANCEMENT").put("remplacement", profil);
        return d;
    }

    private String publie(RequestPostProcessor chef, ObjectNode d) throws Exception {
        String id = creer(chef, d);
        mvc.perform(json(post(BASE + "/" + id + "/publication").with(chef), Map.of("motif", "Mise en service", "leverAvertissements", true))).andExpect(status().isOk());
        return id;
    }

    private static Map<String, Object> lancement(String designe) {
        Map<String, Object> l = new HashMap<>();
        l.put("intitule", "Demande de séparation");
        l.put("designations", designe == null ? Map.of() : Map.of("CONTROLE", designe));
        return l;
    }

    private String url(String instance, String etape, String acte) {
        return INSTANCES + "/" + instance + "/etapes/" + etape + "/" + acte;
    }

    @Test
    @DisplayName("QUO-02 : l'initiateur n'est ni résolu, ni désigné, ni proposé, ni réaffecté, ni ne prend l'étape de sa demande")
    void initiateurEcarte() throws Exception {
        // Trois porteurs du profil réseau dans le pôle (le chef aussi) : sans l'initiateur, deux, l'étape est à prendre.
        organisation.attribuerProfils(CHEF_RESEAU, "ROLE_RESPONSABLE_SERVICE", PROFIL_RESEAU);
        String processus = publie(chef(CHEF_RESEAU), definition("SEPARATION_A", "POLE_RESEAU", PROFIL_RESEAU, false));

        // Se désigner soi-même au lancement : refus explicite.
        mvc
            .perform(json(post(BASE + "/" + processus + "/instances").with(agent(TECHNICIEN_RESEAU)), lancement(TECHNICIEN_RESEAU)))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value(SEPARATION))
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("séparation des fonctions")));

        JsonNode i = lire(
            mvc.perform(json(post(BASE + "/" + processus + "/instances").with(agent(TECHNICIEN_RESEAU)), lancement(INGENIEUR_RESEAU))).andExpect(status().isCreated()).andReturn()
        );
        String id = i.get("id").asText();
        JsonNode traitement = etape(i, "TRAITEMENT");
        assertThat(traitement.at("/aPrendre/profil").asText()).isEqualTo(PROFIL_RESEAU);
        assertThat(traitement.at("/moi/prendre").asBoolean()).isFalse();
        // L'offre ne parvient pas à l'initiateur, qui ne la voit pas dans « Mes tâches » et ne peut pas la prendre.
        assertThat(textes(charge("pilotage.process-step-activated.v1", id).get("destinataires"))).containsExactlyInAnyOrder(CHEF_RESEAU, INGENIEUR_RESEAU);
        String tache = traitement.get("tacheId").asText();
        JsonNode miennes = lire(mvc.perform(get("/api/v1/taches").param("vue", "moi").with(agent(TECHNICIEN_RESEAU))).andExpect(status().isOk()).andReturn());
        assertThat(miennes.get("contenu").findValuesAsText("id")).doesNotContain(tache);
        mvc
            .perform(post("/api/v1/taches/" + tache + "/prise").with(agent(TECHNICIEN_RESEAU)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value(SEPARATION));

        // Candidats de la réaffectation : jamais l'initiateur, nommé dans « exclus ».
        JsonNode candidats = lire(mvc.perform(get(url(id, "TRAITEMENT", "candidats")).with(chef(CHEF_RESEAU))).andExpect(status().isOk()).andReturn());
        assertThat(candidats.get("dansUnite").findValuesAsText("id")).containsExactlyInAnyOrder(CHEF_RESEAU, INGENIEUR_RESEAU);
        assertThat(textes(candidats.get("exclus"))).containsExactly(TECHNICIEN_RESEAU);
        mvc
            .perform(json(post(url(id, "TRAITEMENT", "reaffectation")).with(chef(CHEF_RESEAU)), Map.of("personneId", TECHNICIEN_RESEAU, "motif", "Essai")))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value(SEPARATION));
        mvc.perform(post("/api/v1/taches/" + tache + "/prise").with(agent(INGENIEUR_RESEAU))).andExpect(status().isOk());
    }

    @Test
    @DisplayName("QUO-02 : seul autre porteur que l'initiateur, l'étape lui est confiée ; aucun autre : chef de l'unité")
    void seulAutrePorteurPuisChef() throws Exception {
        String processus = publie(chef(CHEF_RESEAU), definition("SEPARATION_B", "POLE_RESEAU", PROFIL_RESEAU, false));
        JsonNode i = lire(
            mvc.perform(json(post(BASE + "/" + processus + "/instances").with(agent(TECHNICIEN_RESEAU)), lancement(INGENIEUR_RESEAU))).andExpect(status().isCreated()).andReturn()
        );
        assertThat(etape(i, "TRAITEMENT").at("/executant/id").asText()).isEqualTo(INGENIEUR_RESEAU);

        // Le technicien perd le profil : l'ingénieure, seule porteuse, lance ; plus aucun autre porteur : le chef de l'unité.
        organisation.attribuerProfils(TECHNICIEN_RESEAU, "ROLE_COLLABORATEUR");
        String seul = publie(chef(CHEF_RESEAU), definition("SEPARATION_B2", "POLE_RESEAU", PROFIL_RESEAU, false, false));
        JsonNode j = lire(
            mvc.perform(json(post(BASE + "/" + seul + "/instances").with(agent(INGENIEUR_RESEAU)), lancement(null))).andExpect(status().isCreated()).andReturn()
        );
        assertThat(etape(j, "TRAITEMENT").at("/executant/id").asText()).isEqualTo(CHEF_RESEAU);
        assertThat(j.get("historique").findValuesAsText("detail")).anyMatch(d -> d.contains("hors de l'initiateur de la demande"));
    }

    @Test
    @DisplayName("QUO-02 : la définition peut autoriser l'initiateur à intervenir (réglage en données)")
    void autorisationExplicite() throws Exception {
        String processus = publie(chef(CHEF_RESEAU), definition("SEPARATION_C", "POLE_RESEAU", PROFIL_RESEAU, true));
        mvc.perform(get(BASE + "/" + processus).with(chef(CHEF_RESEAU))).andExpect(jsonPath("$.initiateurs.initiateurIntervenant").value(true));
        JsonNode i = lire(
            mvc.perform(json(post(BASE + "/" + processus + "/instances").with(agent(TECHNICIEN_RESEAU)), lancement(TECHNICIEN_RESEAU))).andExpect(status().isCreated()).andReturn()
        );
        assertThat(etape(i, "TRAITEMENT").get("executant").isNull()).isTrue();
        assertThat(etape(i, "TRAITEMENT").at("/moi/prendre").asBoolean()).isTrue();
    }

    @Test
    @DisplayName("QUO-03 et QUO-04 : un porteur hors de l'unité lit l'étape à prendre ; pièces et livrables téléchargés par qui lit")
    void lecturePorteursEtDocuments() throws Exception {
        // Processus du SNA confié au profil réseau, porté seulement au pôle réseau (deux porteurs) : à prendre hors de l'unité.
        String processus = publie(chef(CHEF_SERVICE), definition("SEPARATION_D", "SNA", PROFIL_RESEAU, false));
        JsonNode i = lire(
            mvc.perform(json(post(BASE + "/" + processus + "/instances").with(agent(AGENT)), lancement(INGENIEUR_RESEAU))).andExpect(status().isCreated()).andReturn()
        );
        String id = i.get("id").asText();
        assertThat(etape(i, "TRAITEMENT").at("/aPrendre/profil").asText()).isEqualTo(PROFIL_RESEAU);
        // Avant la correction : 403 « lisible de l'unité et de ses intervenants » pour le porteur qui doit prendre l'étape.
        mvc.perform(get(INSTANCES + "/" + id).with(agent(TECHNICIEN_RESEAU))).andExpect(status().isOk()).andExpect(jsonPath("$.etapes[0].moi.prendre").value(true));
        // Personne étrangère : refus explicite (403), jamais « introuvable ».
        mvc.perform(get(INSTANCES + "/" + id).with(agent(CHEF_DDRM))).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("PIL_PROCESSUS_INTERDIT"));

        String tache = etape(i, "TRAITEMENT").get("tacheId").asText();
        mvc.perform(post("/api/v1/taches/" + tache + "/prise").with(agent(TECHNICIEN_RESEAU))).andExpect(status().isOk());
        String doc = deposer(id, TECHNICIEN_RESEAU, "note.pdf", "application/pdf");
        mvc.perform(json(post(url(id, "TRAITEMENT", "livrables")).with(agent(TECHNICIEN_RESEAU)), Map.of("livrable", "NOTE", "documentId", doc))).andExpect(status().isOk());

        // L'initiatrice télécharge le livrable ; un étranger est refusé ; un document hors de l'instance est inconnu.
        byte[] corps = mvc
            .perform(get(INSTANCES + "/" + id + "/documents/" + doc).with(agent(AGENT)))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/pdf"))
            .andReturn()
            .getResponse()
            .getContentAsByteArray();
        assertThat(new String(corps, StandardCharsets.UTF_8)).startsWith("note.pdf");
        mvc.perform(get(INSTANCES + "/" + id + "/documents/" + doc).with(agent(CHEF_DDRM))).andExpect(status().isForbidden());
        String autre = deposer(id, AGENT, "autre.pdf", "application/pdf");
        mvc
            .perform(get(INSTANCES + "/" + id + "/documents/" + autre).with(agent(AGENT)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("PIL_PROCESSUS_DOCUMENT_INCONNU"));
    }
}
