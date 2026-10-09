package cm.gimac.des.pilotage.web.rest.processus;

import static cm.gimac.des.pilotage.infrastructure.client.OrganisationStubAdapter.AGENT;
import static cm.gimac.des.pilotage.infrastructure.client.OrganisationStubAdapter.CHEF_DDRM;
import static cm.gimac.des.pilotage.infrastructure.client.OrganisationStubAdapter.CHEF_RESEAU;
import static cm.gimac.des.pilotage.infrastructure.client.OrganisationStubAdapter.INGENIEUR_RESEAU;
import static cm.gimac.des.pilotage.infrastructure.client.OrganisationStubAdapter.TECHNICIEN_RESEAU;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cm.gimac.des.pilotage.infrastructure.demo.DemonstrationProcessus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Lancer un processus, agir à chaque étape, suivre son avancement (D-98, docs/lot1/lancement-processus.md §1 à §4) :
 * initiateurs en données (défaut MEMBRES_UNITE, TOUT_LE_GIMAC, UNITES, PROFILS, PERMISSIONS), formulaire de lancement
 * contrôlé (422 et fieldErrors), lancement par un membre d'un autre service, « processus que je peux lancer », « mes
 * demandes » jusqu'à la clôture, événements portant l'initiateur.
 */
class ProcessusLancementIT extends AbstractProcessusIT {

    /** « Demande de changement d'adresse IP » du pôle réseau, ouverte à tout le GIMAC, avec une capture facultative. */
    private ObjectNode demandeIp(String code) {
        ObjectNode d = json.createObjectNode();
        d.put("code", code);
        d.put("unite", DemonstrationProcessus.UNITE);
        d.put("libelle", "Demande de changement d'adresse IP");
        d.put("categorie", "Réseau");
        d.set("etapes", json.valueToTree(DemonstrationProcessus.etapesIp()));
        d.set("initiateurs", json.valueToTree(Map.of("regles", List.of("TOUT_LE_GIMAC"))));
        ObjectNode formulaire = (ObjectNode) json.valueToTree(DemonstrationProcessus.formulaireIp());
        ((ArrayNode) formulaire.get("champs")).add(
            json.valueToTree(Map.of("code", "CAPTURE", "libelle", "Capture de l'erreur", "type", "PIECE_JOINTE", "obligatoire", false, "typesPieces", List.of("image/*")))
        );
        d.set("formulaire", formulaire);
        return d;
    }

    private static Map<String, Object> lancement(Map<String, Object> formulaire) {
        Map<String, Object> l = new HashMap<>();
        l.put("intitule", "Poste d'accueil du rez-de-chaussée");
        l.put("formulaire", formulaire);
        return l;
    }

    private static Map<String, Object> valeurs() {
        Map<String, Object> f = new HashMap<>();
        f.put("EQUIPEMENT", "PC-SNA-014");
        f.put("ADRESSE_ACTUELLE", "10.20.4.15");
        f.put("ADRESSE_DEMANDEE", "10.20.6.40");
        f.put("JUSTIFICATION", "Changement de bureau vers le plateau du service.");
        return f;
    }

    private String url(String instance, String etape, String acte) {
        return INSTANCES + "/" + instance + "/etapes/" + etape + "/" + acte;
    }

    private JsonNode instance(String id, String qui) throws Exception {
        return lire(mvc.perform(get(INSTANCES + "/" + id).with(agent(qui))).andExpect(status().isOk()).andReturn());
    }

    /** L'intervenant prend la tâche de l'étape, renseigne ses champs et la termine. */
    private JsonNode prendreEtTerminer(String id, String etape, String qui, Map<String, Object> champs) throws Exception {
        JsonNode e = etape(instance(id, qui), etape);
        if (e.at("/moi/prendre").asBoolean()) {
            mvc.perform(post("/api/v1/taches/" + e.get("tacheId").asText() + "/prise").with(agent(qui))).andExpect(status().isOk());
        }
        if (!champs.isEmpty()) {
            mvc.perform(json(put(url(id, etape, "champs")).with(agent(qui)), Map.of("valeurs", champs))).andExpect(status().isOk());
        }
        return lire(mvc.perform(post(url(id, etape, "fin")).with(agent(qui))).andExpect(status().isOk()).andReturn());
    }

    @Test
    @DisplayName("Initiateurs et formulaire en données : enregistrés, gardés à la modification, contrôlés à la publication")
    void definition() throws Exception {
        String id = creer(chef(CHEF_RESEAU), demandeIp("CHANGEMENT_IP"));
        JsonNode d = lire(mvc.perform(get(BASE + "/" + id).with(chef(CHEF_RESEAU))).andExpect(status().isOk()).andReturn());
        assertThat(textes(d.at("/initiateurs/regles"))).containsExactly("TOUT_LE_GIMAC");
        assertThat(d.at("/formulaire/champs").findValuesAsText("code")).containsExactly("EQUIPEMENT", "ADRESSE_ACTUELLE", "ADRESSE_DEMANDEE", "JUSTIFICATION", "CAPTURE");
        assertThat(d.at("/formulaire/champs/0/aide").asText()).startsWith("Nom ou numéro");
        assertThat(d.at("/formulaire/champs/4/typesPieces/0").asText()).isEqualTo("image/*");

        // Modification sans initiateurs ni formulaire (ancien client) : ceux de la définition sont gardés.
        ObjectNode sans = demandeIp("CHANGEMENT_IP");
        sans.remove("initiateurs");
        sans.remove("formulaire");
        sans.put("libelle", "Changement d'adresse IP");
        mvc.perform(json(put(BASE + "/" + id).with(chef(CHEF_RESEAU)), sans)).andExpect(status().isOk()).andExpect(jsonPath("$.initiateurs.regles[0]").value("TOUT_LE_GIMAC")).andExpect(jsonPath("$.formulaire.champs.length()").value(5));

        // Formes refusées : règle inconnue, profil mal formé, type de champ inconnu.
        ObjectNode mauvais = demandeIp("CHANGEMENT_IP");
        mauvais.set("initiateurs", json.valueToTree(Map.of("regles", List.of("TOUT_LE_MONDE"))));
        mvc.perform(json(put(BASE + "/" + id).with(chef(CHEF_RESEAU)), mauvais)).andExpect(status().isBadRequest()).andExpect(jsonPath("$.details.champ").value("initiateurs.regles[0]"));
        mauvais.set("initiateurs", json.valueToTree(Map.of("regles", List.of("PROFILS"), "profils", List.of("chef"))));
        mvc.perform(json(put(BASE + "/" + id).with(chef(CHEF_RESEAU)), mauvais)).andExpect(status().isBadRequest()).andExpect(jsonPath("$.details.champ").value("initiateurs.profils[0]"));
        mauvais.set("initiateurs", json.valueToTree(Map.of("regles", List.of("TOUT_LE_GIMAC"))));
        ((ObjectNode) mauvais.at("/formulaire/champs/0")).put("type", "COULEUR");
        mvc.perform(json(put(BASE + "/" + id).with(chef(CHEF_RESEAU)), mauvais)).andExpect(status().isBadRequest()).andExpect(jsonPath("$.details.champ").value("formulaire.champs[0].type"));

        // Contrôle : règle sans paramètre, unité inconnue, code de champ en double, liste vide (bloquants).
        ObjectNode incomplet = demandeIp("CHANGEMENT_IP");
        incomplet.set("initiateurs", json.valueToTree(Map.of("regles", List.of("UNITES", "PERMISSIONS"), "unites", List.of("UNITE_FANTOME"))));
        ((ArrayNode) incomplet.at("/formulaire/champs")).add(json.valueToTree(Map.of("code", "EQUIPEMENT", "libelle", "Doublon", "type", "LISTE")));
        mvc.perform(json(put(BASE + "/" + id).with(chef(CHEF_RESEAU)), incomplet)).andExpect(status().isOk());
        JsonNode c = lire(mvc.perform(post(BASE + "/" + id + "/controle").with(chef(CHEF_RESEAU))).andExpect(status().isOk()).andReturn());
        assertThat(c.get("publiable").asBoolean()).isFalse();
        assertThat(c.get("anomalies").findValuesAsText("code")).contains("INITIATEURS_INCOMPLETS", "UNITE_INCONNUE", "CHAMP_CODE_DUPLIQUE", "CHAMP_CHOIX_ABSENT");
        assertThat(c.get("anomalies").findValuesAsText("element")).contains("initiateurs.permissions", "initiateurs.unites", "formulaire.EQUIPEMENT");

        // Reprise Liquibase : les définitions existantes et toute ligne sans initiateurs gardent MEMBRES_UNITE.
        String defaut = jdbc.queryForObject("select column_default from information_schema.columns where table_name = 'processus' and column_name = 'initiateurs'", String.class);
        assertThat(defaut).contains("MEMBRES_UNITE");
        String flux = creer(chef(CHEF_RESEAU), definition("FLUX_SANS_INITIATEURS"));
        mvc.perform(get(BASE + "/" + flux).with(chef(CHEF_RESEAU))).andExpect(jsonPath("$.initiateurs.regles", Matchers.contains("MEMBRES_UNITE"))).andExpect(jsonPath("$.formulaire.champs.length()").value(0));
    }

    @Test
    @DisplayName("Un collaborateur d'un autre service lance une demande au pôle réseau, l'intervenant agit, l'initiateur suit jusqu'à la clôture")
    void lancementInterServices() throws Exception {
        String demande = creer(chef(CHEF_RESEAU), demandeIp("DEMANDE_IP"));
        publier(chef(CHEF_RESEAU), demande);
        String reserve = publie("FLUX_RESEAU", CHEF_RESEAU);

        // « Processus que je peux lancer » : la demande ouverte à tout le GIMAC, pas le processus réservé au pôle.
        JsonNode l = lire(mvc.perform(get(BASE + "/lancables").with(agent(AGENT))).andExpect(status().isOk()).andReturn());
        assertThat(l.at("/groupes/0/unite").asText()).isEqualTo("POLE_RESEAU");
        assertThat(l.at("/groupes/0/processus").findValuesAsText("code")).containsExactly("DEMANDE_IP");
        assertThat(l.at("/groupes/0/processus/0/droits/lancer").asBoolean()).isTrue();
        assertThat(l.at("/groupes/0/processus/0/nombreChampsFormulaire").asInt()).isEqualTo(5);
        assertThat(textes(l.at("/groupes/0/processus/0/initiateurs/regles"))).containsExactly("TOUT_LE_GIMAC");
        mvc.perform(get(BASE + "/lancables").param("q", "flux").with(agent(AGENT))).andExpect(jsonPath("$.groupes.length()").value(0));
        JsonNode membre = lire(mvc.perform(get(BASE + "/lancables").with(agent(TECHNICIEN_RESEAU))).andExpect(status().isOk()).andReturn());
        assertThat(membre.at("/groupes/0/processus").findValuesAsText("code")).contains("DEMANDE_IP", "FLUX_RESEAU");

        // La définition publiée se lit de qui peut la lancer (formulaire), même hors de l'unité.
        mvc.perform(get(BASE + "/" + demande).with(agent(AGENT))).andExpect(status().isOk()).andExpect(jsonPath("$.droits.lancer").value(true)).andExpect(jsonPath("$.formulaire.champs.length()").value(5));
        mvc.perform(get(BASE + "/" + reserve).with(agent(AGENT))).andExpect(status().isForbidden());

        // Refus explicite hors des initiateurs.
        mvc
            .perform(json(post(BASE + "/" + reserve + "/instances").with(agent(AGENT)), Map.of("intitule", "Flux", "designations", Map.of("MISE_EN_OEUVRE", INGENIEUR_RESEAU))))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("PIL_PROCESSUS_NON_INITIATEUR"))
            .andExpect(jsonPath("$.details.regles[0]").value("MEMBRES_UNITE"));

        // Formulaire contrôlé : toutes les erreurs ensemble, au format fieldErrors.
        Map<String, Object> faux = valeurs();
        faux.remove("ADRESSE_DEMANDEE");
        faux.put("JUSTIFICATION", "x".repeat(4001));
        faux.put("CAPTURE", "pas-un-document");
        faux.put("COULEUR", "bleu");
        mvc
            .perform(json(post(BASE + "/" + demande + "/instances").with(agent(AGENT)), lancement(faux)))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("PIL_PROCESSUS_FORMULAIRE_INVALIDE"))
            .andExpect(jsonPath("$.fieldErrors[*].field", Matchers.containsInAnyOrder("formulaire.ADRESSE_DEMANDEE", "formulaire.JUSTIFICATION", "formulaire.CAPTURE", "formulaire.COULEUR")))
            .andExpect(jsonPath("$.details.fieldErrors[*].code", Matchers.hasItems("OBLIGATOIRE", "TROP_LONG", "FORMAT", "INCONNU")));
        assertThat(jdbc.queryForObject("select count(*) from processus_instance", Integer.class)).isZero();

        // Pièce jointe par le dépôt fragmenté (référence : la définition lancée), déposée par l'initiateur.
        String capture = documents.deposer("capture.png", "image/png", "png".getBytes(StandardCharsets.UTF_8), "PROCESSUS_UNITE", demande, AGENT).toString();
        String autrui = documents.deposer("capture.png", "image/png", "png".getBytes(StandardCharsets.UTF_8), "PROCESSUS_UNITE", demande, TECHNICIEN_RESEAU).toString();
        Map<String, Object> v = valeurs();
        v.put("CAPTURE", Map.of("documentId", autrui));
        mvc.perform(json(post(BASE + "/" + demande + "/instances").with(agent(AGENT)), lancement(v))).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.details.fieldErrors[0].code").value("PIECE_INVALIDE"));
        v.put("CAPTURE", Map.of("documentId", capture));
        JsonNode i = lire(mvc.perform(json(post(BASE + "/" + demande + "/instances").with(agent(AGENT)), lancement(v))).andExpect(status().isCreated()).andReturn());
        String id = i.get("id").asText();
        assertThat(i.at("/lanceur/id").asText()).isEqualTo(AGENT);
        assertThat(i.get("formulaire").findValuesAsText("code")).containsExactly("EQUIPEMENT", "ADRESSE_ACTUELLE", "ADRESSE_DEMANDEE", "JUSTIFICATION", "CAPTURE");
        assertThat(i.at("/formulaire/2/valeur").asText()).isEqualTo("10.20.6.40");
        assertThat(i.at("/formulaire/4/valeur/nomFichier").asText()).isEqualTo("capture.png");
        assertThat(i.at("/formulaire/4/valeur/sha256").asText()).hasSize(64);
        JsonNode analyse = etape(i, "ANALYSE");
        assertThat(analyse.get("etat").asText()).isEqualTo("ACTIVE");
        assertThat(analyse.at("/aPrendre/profil").asText()).isEqualTo("ROLE_POLE_RESEAU");
        assertThat(analyse.at("/moi/prendre").asBoolean()).as("l'initiateur n'agit pas sur l'étape du pôle").isFalse();
        // L'événement d'activation porte l'initiateur (avancement notifié par MS-NOTIFICATION).
        JsonNode activation = charge("pilotage.process-step-activated.v1", id);
        assertThat(activation.get("lanceurId").asText()).isEqualTo(AGENT);
        assertThat(textes(activation.get("destinataires"))).containsExactlyInAnyOrder(INGENIEUR_RESEAU, TECHNICIEN_RESEAU);

        // L'intervenant du pôle voit les valeurs du formulaire et les seules actions permises.
        JsonNode vueIngenieur = instance(id, INGENIEUR_RESEAU);
        assertThat(vueIngenieur.at("/formulaire/0/valeur").asText()).isEqualTo("PC-SNA-014");
        assertThat(etape(vueIngenieur, "ANALYSE").at("/moi/prendre").asBoolean()).isTrue();
        assertThat(etape(vueIngenieur, "ANALYSE").at("/moi/executer").asBoolean()).isFalse();

        // « Mes demandes » de l'initiateur : étape en cours à prendre.
        JsonNode m = lire(mvc.perform(get(BASE + "/demandes").with(agent(AGENT))).andExpect(status().isOk()).andReturn());
        assertThat(m.get("total").asInt()).isEqualTo(1);
        assertThat(m.at("/contenu/0/id").asText()).isEqualTo(id);
        assertThat(m.at("/contenu/0/etapes/0/etat").asText()).isEqualTo("ACTIVE");
        assertThat(m.at("/contenu/0/etapes/0/aPrendre/profil").asText()).isEqualTo("ROLE_POLE_RESEAU");
        assertThat(m.at("/contenu/0/etapes/1/etat").asText()).isEqualTo("EN_ATTENTE");
        // Entrées d'un même acte au même instant : la dernière évolution est l'une d'elles.
        assertThat(m.at("/contenu/0/derniereEvolution/action").asText()).isIn("LANCEE", "ETAPE_ACTIVEE");
        mvc.perform(get(BASE + "/demandes").with(agent(INGENIEUR_RESEAU))).andExpect(jsonPath("$.total").value(0));

        // Analyse par l'ingénieure, validation par le chef de pôle : l'initiateur voit qui tient l'étape.
        prendreEtTerminer(id, "ANALYSE", INGENIEUR_RESEAU, Map.of("ADRESSE_DISPONIBLE", "Oui"));
        m = lire(mvc.perform(get(BASE + "/demandes").with(agent(AGENT))).andExpect(status().isOk()).andReturn());
        assertThat(m.at("/contenu/0/etapes/0/etat").asText()).isEqualTo("TERMINEE");
        assertThat(m.at("/contenu/0/etapes/0/executant/id").asText()).isEqualTo(INGENIEUR_RESEAU);
        assertThat(m.at("/contenu/0/etapes/1/etat").asText()).isEqualTo("ACTIVE");
        assertThat(m.at("/contenu/0/etapes/1/executant/id").asText()).isEqualTo(CHEF_RESEAU);
        assertThat(m.at("/contenu/0/avancement/terminees").asInt()).isEqualTo(1);
        assertThat(charges("pilotage.process-step-activated.v1", id)).allSatisfy(x -> assertThat(x.get("lanceurId").asText()).isEqualTo(AGENT));
        // « Mes étapes » (D-98) : le résumé porte les actions de l'appelant ; l'étape attend le chef, plus l'ingénieure.
        JsonNode miennesChef = lire(mvc.perform(get(INSTANCES).param("vue", "miennes").with(chef(CHEF_RESEAU))).andExpect(status().isOk()).andReturn());
        assertThat(miennesChef.at("/contenu/0/etapesEnCours/0/code").asText()).isEqualTo("VALIDATION");
        assertThat(miennesChef.at("/contenu/0/etapesEnCours/0/attendMoi").asBoolean()).isTrue();
        assertThat(miennesChef.at("/contenu/0/etapesEnCours/0/moi/executer").asBoolean()).isTrue();
        JsonNode miennesIngenieur = lire(mvc.perform(get(INSTANCES).param("vue", "miennes").with(agent(INGENIEUR_RESEAU))).andExpect(status().isOk()).andReturn());
        assertThat(miennesIngenieur.at("/contenu/0/id").asText()).isEqualTo(id);
        assertThat(miennesIngenieur.at("/contenu/0/etapesEnCours/0/attendMoi").asBoolean()).isFalse();
        assertThat(miennesIngenieur.at("/contenu/0/etapesEnCours/0/moi/executer").asBoolean()).isFalse();
        prendreEtTerminer(id, "VALIDATION", CHEF_RESEAU, Map.of("DECISION", "Accordée"));
        JsonNode unite = lire(mvc.perform(get(INSTANCES).param("vue", "unite").with(agent(TECHNICIEN_RESEAU))).andExpect(status().isOk()).andReturn());
        assertThat(unite.at("/contenu/0/etapesEnCours/0/code").asText()).isEqualTo("EXECUTION");
        assertThat(unite.at("/contenu/0/etapesEnCours/0/moi/prendre").asBoolean()).isTrue();
        assertThat(unite.at("/contenu/0/etapesEnCours/0/attendMoi").asBoolean()).isTrue();
        prendreEtTerminer(id, "EXECUTION", TECHNICIEN_RESEAU, Map.of("ADRESSE_ATTRIBUEE", "10.20.6.40"));
        JsonNode fin = prendreEtTerminer(id, "CLOTURE", TECHNICIEN_RESEAU, Map.of());
        assertThat(fin.get("etat").asText()).isEqualTo("CLOSE");

        // Clôture : l'initiateur la voit dans ses demandes et l'événement lui est destiné.
        mvc
            .perform(get(BASE + "/demandes").param("etat", "CLOSE").param("q", "accueil").with(agent(AGENT)))
            .andExpect(jsonPath("$.total").value(1))
            .andExpect(jsonPath("$.contenu[0].etat").value("CLOSE"))
            .andExpect(jsonPath("$.contenu[0].avancement.terminees").value(4))
            .andExpect(jsonPath("$.contenu[0].derniereEvolution.action").value(Matchers.in(List.of("ETAPE_SOUMISE", "ETAPE_TERMINEE", "CLOSE"))));
        mvc.perform(get(BASE + "/demandes").param("etat", "EN_COURS").with(agent(AGENT))).andExpect(jsonPath("$.total").value(0));
        JsonNode cloture = charge("pilotage.process-instance-closed.v1", id);
        assertThat(textes(cloture.get("destinataires"))).containsExactly(AGENT);
        JsonNode lu = instance(id, AGENT);
        assertThat(lu.get("historique").findValuesAsText("action")).contains("LANCEE", "ETAPE_PRISE", "CHAMPS_RENSEIGNES", "ETAPE_TERMINEE", "CLOSE");
        assertThat(lu.at("/formulaire/3/valeur").asText()).startsWith("Changement de bureau");
    }

    @Test
    @DisplayName("Unités, profils et permissions désignés : lancement ouvert à leurs seuls porteurs")
    void reglesCumulees() throws Exception {
        ObjectNode d = demandeIp("DEMANDE_CIBLEE");
        d.set("initiateurs", json.valueToTree(Map.of("regles", List.of("UNITES", "PERMISSIONS"), "unites", List.of("SNA"), "permissions", List.of("CER_DOSSIER_VERIFY"))));
        d.set("formulaire", json.valueToTree(Map.of("champs", List.of())));
        String id = creer(chef(CHEF_RESEAU), d);
        publier(chef(CHEF_RESEAU), id);
        Map<String, Object> l = lancement(Map.of());
        mvc.perform(json(post(BASE + "/" + id + "/instances").with(agent(AGENT)), l)).andExpect(status().isCreated());
        mvc.perform(json(post(BASE + "/" + id + "/instances").with(agent(CHEF_DDRM)), l)).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("PIL_PROCESSUS_NON_INITIATEUR"));
        mvc.perform(json(post(BASE + "/" + id + "/instances").with(utilisateur(CHEF_DDRM, "PIL_ACTIVITY_OWN", "CER_DOSSIER_VERIFY")), l)).andExpect(status().isCreated());
        // MEMBRES_UNITE n'est pas retenue : ni le technicien du pôle, ni son chef (D-100 : PROCESSUS_LANCER couvrant l'unité
        // n'ouvre plus le lancement, seul le public défini par le créateur compte).
        mvc.perform(json(post(BASE + "/" + id + "/instances").with(agent(TECHNICIEN_RESEAU)), l)).andExpect(status().isForbidden());
        mvc
            .perform(json(post(BASE + "/" + id + "/instances").with(chef(CHEF_RESEAU)), l))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("PIL_PROCESSUS_NON_INITIATEUR"))
            .andExpect(jsonPath("$.message", Matchers.not(Matchers.containsString("PROCESSUS_LANCER"))));
    }
}
