package cm.gimac.des.pilotage.web.rest.processus;

import static cm.gimac.des.pilotage.infrastructure.client.OrganisationStubAdapter.AGENT;
import static cm.gimac.des.pilotage.infrastructure.client.OrganisationStubAdapter.AGENT_BIS;
import static cm.gimac.des.pilotage.infrastructure.client.OrganisationStubAdapter.CHEF_DDRM;
import static cm.gimac.des.pilotage.infrastructure.client.OrganisationStubAdapter.CHEF_RESEAU;
import static cm.gimac.des.pilotage.infrastructure.client.OrganisationStubAdapter.INGENIEUR_RESEAU;
import static cm.gimac.des.pilotage.infrastructure.client.OrganisationStubAdapter.SUPER_ADMIN;
import static cm.gimac.des.pilotage.infrastructure.client.OrganisationStubAdapter.TECHNICIEN_RESEAU;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cm.gimac.des.pilotage.application.port.out.OrganisationPort.Portee;
import cm.gimac.des.pilotage.application.port.out.OrganisationPort.Pouvoir;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Administration des référentiels métier, tous domaines (D-92), sur les processus d'unité : REF_METIER_ADMIN du
 * jeton (super administrateur, ingénieur d'homologation, tout profil qui la reçoit) et portée TOUTES de la matrice ;
 * un chef reste limité à ses unités ; HOM_REFERENTIEL_EDIT seul n'ouvre rien ; le journal nomme le droit utilisé.
 */
class ProcessusReferentielsMetierIT extends AbstractProcessusIT {

    private static final String REF = "REF_METIER_ADMIN";
    private static final String MENTION = "(au titre de REF_METIER_ADMIN)";

    /** Porteur de REF_METIER_ADMIN sans aucun autre droit sur les processus (ni matrice, ni IAM_PROFILE_EDIT). */
    static RequestPostProcessor referentiels(String id) {
        return utilisateur(id, "PIL_ACTIVITY_OWN", REF);
    }

    private Map<String, Object> lancement() {
        Map<String, Object> l = new HashMap<>();
        l.put("intitule", "Flux supervision vers SIEM");
        l.put("champs", Map.of("SOURCE", "10.0.0.1", "PORT", 443));
        l.put("designations", Map.of("MISE_EN_OEUVRE", INGENIEUR_RESEAU));
        return l;
    }

    private static List<String> details(JsonNode vue) {
        List<String> l = new ArrayList<>();
        vue.get("historique").forEach(h -> l.add(h.path("detail").asText()));
        return l;
    }

    /** Cycle complet sur le pôle réseau, que l'appelant ne dirige pas et dont il n'est pas membre. */
    private void cycleComplet(RequestPostProcessor qui, String code) throws Exception {
        // Création, modification, contrôle, publication avec levée motivée.
        ObjectNode d = definition(code);
        JsonNode cree = lire(mvc.perform(json(post(BASE).with(qui), d)).andExpect(status().isCreated()).andReturn());
        relever(d, cree);
        String v1 = cree.get("id").asText();
        assertThat(cree.at("/droits/modifier").asBoolean()).isTrue();
        assertThat(cree.at("/droits/supprimer").asBoolean()).isTrue();
        d.put("libelle", "Ouverture d'un flux réseau (revue)");
        ((ObjectNode) d.get("etapes").get(1).get("intervenant")).put("profil", "ROLE_POLE_SYSTEME");
        mvc.perform(json(put(BASE + "/" + v1).with(qui), d)).andExpect(status().isOk()).andExpect(jsonPath("$.libelle").value("Ouverture d'un flux réseau (revue)"));
        mvc.perform(post(BASE + "/" + v1 + "/controle").with(qui)).andExpect(status().isOk()).andExpect(jsonPath("$.anomalies[0].code").value("COUVERTURE_PROFIL"));
        JsonNode publie = lire(
            mvc
                .perform(json(post(BASE + "/" + v1 + "/publication").with(qui), Map.of("leverAvertissements", true, "motif", "Mise en service par l'administration")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.etat").value("PUBLIEE"))
                .andExpect(jsonPath("$.droits.lancer").value(false))
                .andExpect(jsonPath("$.droits.retirer").value(true))
                .andExpect(jsonPath("$.droits.nouvelleVersion").value(true))
                .andReturn()
        );
        assertThat(details(publie)).allMatch(x -> x.endsWith(MENTION));
        assertThat(publie.get("historique").findValuesAsText("action")).containsExactly("CREE", "MODIFIE", "PUBLIE");
        assertThat(publie.at("/historique/0/acteur/id").asText()).isNotBlank();

        // Nouvelle version, suppression du brouillon, nouvelle version encore, publication (la v1 est retirée), retrait.
        String v2 = lire(mvc.perform(post(BASE + "/" + v1 + "/nouvelle-version").with(qui)).andExpect(status().isCreated()).andReturn()).get("id").asText();
        mvc.perform(delete(BASE + "/" + v2).with(qui)).andExpect(status().isNoContent());
        assertThat(jdbc.queryForObject("select detail from processus_historique where action = 'SUPPRIME' and processus_id = ?::uuid", String.class, v2)).endsWith(MENTION);
        String v3 = lire(mvc.perform(post(BASE + "/" + v1 + "/nouvelle-version").with(qui)).andExpect(status().isCreated()).andReturn()).get("id").asText();
        mvc.perform(json(put(BASE + "/" + v3).with(qui), d)).andExpect(status().isOk());
        mvc.perform(json(post(BASE + "/" + v3 + "/publication").with(qui), Map.of("leverAvertissements", true, "motif", "Version 3"))).andExpect(status().isOk());
        mvc.perform(get(BASE + "/" + v1).with(qui)).andExpect(jsonPath("$.etat").value("RETIREE"));

        // D-100 : l'administration ne lance pas (public défini par le créateur : membres et responsables du pôle) ; un membre
        // lance, l'administration pilote l'instance.
        mvc
            .perform(json(post(BASE + "/" + v3 + "/instances").with(qui), lancement()))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("PIL_PROCESSUS_NON_INITIATEUR"));
        mvc.perform(get(BASE + "/lancables").with(qui)).andExpect(jsonPath("$.groupes.length()").value(0));
        String id = lire(mvc.perform(json(post(BASE + "/" + v3 + "/instances").with(agent(TECHNICIEN_RESEAU)), lancement())).andExpect(status().isCreated()).andReturn())
            .get("id")
            .asText();
        JsonNode i = lire(mvc.perform(get(INSTANCES + "/" + id).with(qui)).andExpect(status().isOk()).andReturn());
        assertThat(i.at("/droits/suspendre").asBoolean()).isTrue();
        assertThat(i.at("/droits/annuler").asBoolean()).isTrue();
        assertThat(etape(i, "DEMANDE").at("/moi/reaffecter").asBoolean()).isTrue();
        mvc.perform(get(INSTANCES).param("vue", "unite").with(qui)).andExpect(jsonPath("$.contenu[*].id", Matchers.hasItem(id)));
        mvc
            .perform(json(post(urlEtape(id, "DEMANDE", "reaffectation")).with(qui), Map.of("personneId", INGENIEUR_RESEAU, "motif", "Absence")))
            .andExpect(status().isOk());
        mvc.perform(json(post(INSTANCES + "/" + id + "/suspension").with(qui), Map.of("motif", "Gel des changements"))).andExpect(status().isOk()).andExpect(jsonPath("$.etat").value("SUSPENDUE"));
        mvc.perform(json(post(INSTANCES + "/" + id + "/reprise").with(qui), Map.of())).andExpect(status().isOk()).andExpect(jsonPath("$.etat").value("EN_COURS"));
        JsonNode annulee = lire(
            mvc.perform(json(post(INSTANCES + "/" + id + "/annulation").with(qui), Map.of("motif", "Demande abandonnée"))).andExpect(status().isOk()).andReturn()
        );
        assertThat(annulee.get("etat").asText()).isEqualTo("ANNULEE");
        Map<String, String> parAction = new HashMap<>();
        annulee.get("historique").forEach(h -> parAction.put(h.get("action").asText(), h.path("detail").asText()));
        assertThat(parAction.get("LANCEE")).doesNotContain("au titre de");
        assertThat(parAction.get("REAFFECTEE")).endsWith(MENTION);
        assertThat(parAction.get("SUSPENDUE")).isEqualTo("Gel des changements " + MENTION);
        assertThat(parAction.get("REPRISE")).isEqualTo(MENTION);
        assertThat(parAction.get("ANNULEE")).isEqualTo("Demande abandonnée " + MENTION);

        // Retrait de la version publiée.
        mvc
            .perform(json(post(BASE + "/" + v3 + "/retrait").with(qui), Map.of("motif", "Procédure remplacée")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.etat").value("RETIREE"))
            .andExpect(jsonPath("$.historique[-1].detail").value("Version 2 retirée : Procédure remplacée " + MENTION));
    }

    @Test
    @DisplayName("Super administrateur et ingénieur d'homologation (REF_METIER_ADMIN, sans matrice) : cycle complet sur une unité qu'ils ne dirigent pas")
    void cycleParLesDeuxProfils() throws Exception {
        assertThat(organisation.pouvoirs(SUPER_ADMIN).actions()).isEmpty();
        assertThat(organisation.pouvoirs(AGENT).actions()).isEmpty();
        cycleComplet(referentiels(SUPER_ADMIN), "FLUX_SUPER_ADMIN");
        cycleComplet(referentiels(AGENT), "FLUX_INGENIEUR");
    }

    @Test
    @DisplayName("Catalogue : toutes les unités et portée TOUS_DOMAINES pour REF_METIER_ADMIN ; création dans toute unité existante ; unité inconnue 422")
    void catalogueEtCreation() throws Exception {
        String reseau = publie("FLUX_RESEAU", CHEF_RESEAU);
        ObjectNode ddrm = definition("FLUX_DDRM");
        ddrm.put("unite", "DDRM");
        String brouillonDdrm = creer(chef(CHEF_DDRM), ddrm);
        ObjectNode ssi = definition("FLUX_SSI");
        ssi.put("unite", "SSI");
        creer(referentiels(SUPER_ADMIN), ssi);

        mvc
            .perform(get(BASE).with(referentiels(AGENT)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[*].code", Matchers.containsInAnyOrder(code("FLUX_RESEAU"), code("FLUX_DDRM"), code("FLUX_SSI"))));
        mvc.perform(get(BASE).param("unite", "DDRM").with(referentiels(AGENT))).andExpect(jsonPath("$[*].code", Matchers.contains(code("FLUX_DDRM"))));
        mvc.perform(get(BASE + "/" + brouillonDdrm).with(referentiels(AGENT))).andExpect(status().isOk()).andExpect(jsonPath("$.droits.modifier").value(true));
        mvc
            .perform(get(BASE + "/portee").with(referentiels(SUPER_ADMIN)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.portee").value("TOUS_DOMAINES"))
            .andExpect(jsonPath("$.definirToutesUnites").value(true))
            .andExpect(jsonPath("$.lancerToutesUnites").value(true))
            .andExpect(jsonPath("$.auTitreDe[0]").value(REF))
            .andExpect(jsonPath("$.unites.length()").value(0));
        ObjectNode inconnue = definition("FLUX_FANTOME");
        inconnue.put("unite", "UNITE_FANTOME");
        mvc.perform(json(post(BASE).with(referentiels(AGENT)), inconnue)).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("PIL_PROCESSUS_UNITE_INCONNUE"));

        // Chef : ses unités seulement, portée MES_UNITES ; refus 403 hors de ses unités.
        mvc.perform(get(BASE).with(chef(CHEF_RESEAU))).andExpect(jsonPath("$[*].code", Matchers.contains(code("FLUX_RESEAU"))));
        mvc
            .perform(get(BASE + "/portee").with(chef(CHEF_RESEAU)))
            .andExpect(jsonPath("$.portee").value("MES_UNITES"))
            .andExpect(jsonPath("$.definirToutesUnites").value(false))
            .andExpect(jsonPath("$.unitesDefinissables[0]").value("POLE_RESEAU"));
        ObjectNode horsUnite = definition("FLUX_HORS");
        horsUnite.put("unite", "SSI");
        mvc.perform(json(post(BASE).with(chef(CHEF_RESEAU)), horsUnite)).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("PIL_PROCESSUS_INTERDIT"));
        mvc.perform(get(BASE + "/" + brouillonDdrm).with(chef(CHEF_RESEAU))).andExpect(status().isForbidden());
        mvc.perform(json(put(BASE + "/" + brouillonDdrm).with(chef(CHEF_RESEAU)), ddrm)).andExpect(status().isForbidden());
        mvc.perform(delete(BASE + "/" + brouillonDdrm).with(chef(CHEF_RESEAU))).andExpect(status().isForbidden());
        // Le journal du chef dans son unité ne porte aucune mention.
        mvc.perform(get(BASE + "/" + reseau).with(chef(CHEF_RESEAU))).andExpect(jsonPath("$.historique[*].detail", Matchers.everyItem(Matchers.not(Matchers.containsString("au titre de")))));
    }

    @Test
    @DisplayName("Chef de département avec HOM_REFERENTIEL_EDIT seul : les autres unités restent fermées")
    void referentielHomologationSeul() throws Exception {
        RequestPostProcessor chefDepartement = utilisateur(CHEF_DDRM, "PIL_ACTIVITY_OWN", "PIL_TEAM_READ", "PIL_TASK_ASSIGN", "HOM_REFERENTIEL_EDIT");
        String reseau = publie("FLUX_RESEAU", CHEF_RESEAU);
        String brouillon = creer(chef(CHEF_RESEAU), definition("FLUX_BROUILLON"));
        mvc.perform(json(post(BASE).with(chefDepartement), definition("FLUX_DEPARTEMENT"))).andExpect(status().isForbidden());
        mvc.perform(get(BASE + "/" + brouillon).with(chefDepartement)).andExpect(status().isForbidden());
        mvc.perform(delete(BASE + "/" + brouillon).with(chefDepartement)).andExpect(status().isForbidden());
        mvc.perform(post(BASE + "/" + reseau + "/nouvelle-version").with(chefDepartement)).andExpect(status().isForbidden());
        mvc.perform(json(post(BASE + "/" + reseau + "/retrait").with(chefDepartement), Map.of("motif", "x"))).andExpect(status().isForbidden());
        mvc.perform(json(post(BASE + "/" + reseau + "/instances").with(chefDepartement), lancement())).andExpect(status().isForbidden());
        String instance = lire(mvc.perform(json(post(BASE + "/" + reseau + "/instances").with(agent(TECHNICIEN_RESEAU)), lancement())).andExpect(status().isCreated()).andReturn())
            .get("id")
            .asText();
        mvc.perform(json(post(INSTANCES + "/" + instance + "/suspension").with(chefDepartement), Map.of("motif", "x"))).andExpect(status().isForbidden());
        mvc.perform(get(BASE).with(chefDepartement)).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get(BASE + "/portee").with(chefDepartement)).andExpect(jsonPath("$.portee").value("MES_UNITES"));
        // Son propre département reste ouvert par la matrice.
        ObjectNode ddrm = definition("FLUX_DDRM");
        ddrm.put("unite", "DDRM");
        creer(chefDepartement, ddrm);
    }

    @Test
    @DisplayName("Fonction attribuable : profil créé avec REF_METIER_ADMIN ; profil avec PROCESSUS_DEFINIR ou PROCESSUS_LANCER à TOUTES sans la permission (D-100 : sans lancement)")
    void fonctionAttribuable() throws Exception {
        // Profil créé quelconque recevant REF_METIER_ADMIN par composition.
        organisation.attribuerProfils(AGENT_BIS, "ROLE_PROFIL_CREE_QUALITE");
        ObjectNode ssi = definition("FLUX_QUALITE");
        ssi.put("unite", "SSI");
        String id = creer(referentiels(AGENT_BIS), ssi);
        mvc.perform(json(post(BASE + "/" + id + "/publication").with(referentiels(AGENT_BIS)), Map.of("motif", "Qualité"))).andExpect(status().isUnprocessableEntity());
        mvc.perform(delete(BASE + "/" + id).with(referentiels(AGENT_BIS))).andExpect(status().isNoContent());
        // Le même compte sans la permission : refusé.
        mvc.perform(json(post(BASE).with(agent(AGENT_BIS)), ssi)).andExpect(status().isForbidden());

        // Matrice : PROCESSUS_DEFINIR à TOUTES, sans la permission : définir partout, pas lancer hors de son unité.
        organisation.definirPouvoirs(AGENT_BIS, Map.of(Pouvoir.PROCESSUS_DEFINIR, Portee.TOUTES));
        String reseau = creer(agent(AGENT_BIS), definition("FLUX_MATRICE"));
        publier(agent(AGENT_BIS), reseau);
        mvc
            .perform(get(BASE + "/" + reseau).with(agent(AGENT_BIS)))
            .andExpect(jsonPath("$.droits.retirer").value(true))
            .andExpect(jsonPath("$.droits.lancer").value(false))
            .andExpect(jsonPath("$.historique[0].detail", Matchers.endsWith("(au titre de PROCESSUS_DEFINIR portée TOUTES)")));
        mvc.perform(json(post(BASE + "/" + reseau + "/instances").with(agent(AGENT_BIS)), lancement())).andExpect(status().isForbidden());
        mvc
            .perform(get(BASE + "/portee").with(agent(AGENT_BIS)))
            .andExpect(jsonPath("$.portee").value("TOUS_DOMAINES"))
            .andExpect(jsonPath("$.definirToutesUnites").value(true))
            .andExpect(jsonPath("$.lancerToutesUnites").value(false))
            .andExpect(jsonPath("$.auTitreDe[0]").value("PROCESSUS_DEFINIR:TOUTES"));

        // Matrice : PROCESSUS_LANCER à TOUTES : lire et piloter partout, ni définir, ni lancer (D-100 : le public du processus
        // fait seul foi).
        organisation.definirPouvoirs(AGENT_BIS, Map.of(Pouvoir.PROCESSUS_LANCER, Portee.TOUTES));
        mvc.perform(json(post(BASE).with(agent(AGENT_BIS)), definition("FLUX_LANCEUR"))).andExpect(status().isForbidden());
        mvc.perform(json(post(BASE + "/" + reseau + "/instances").with(agent(AGENT_BIS)), lancement())).andExpect(status().isForbidden());
        mvc.perform(get(BASE + "/" + reseau).with(agent(AGENT_BIS))).andExpect(status().isOk()).andExpect(jsonPath("$.droits.lancer").value(false));
        String instance = lire(mvc.perform(json(post(BASE + "/" + reseau + "/instances").with(agent(TECHNICIEN_RESEAU)), lancement())).andExpect(status().isCreated()).andReturn())
            .get("id")
            .asText();
        mvc
            .perform(json(post(INSTANCES + "/" + instance + "/suspension").with(agent(AGENT_BIS)), Map.of("motif", "Gel")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.historique[-1].detail").value("Gel (au titre de PROCESSUS_LANCER portée TOUTES)"));
    }
}
