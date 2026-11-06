package cm.gimac.des.pilotage.web.rest.processus;

import static cm.gimac.des.pilotage.infrastructure.client.OrganisationStubAdapter.AGENT;
import static cm.gimac.des.pilotage.infrastructure.client.OrganisationStubAdapter.CHEF_DES;
import static cm.gimac.des.pilotage.infrastructure.client.OrganisationStubAdapter.CHEF_RESEAU;
import static cm.gimac.des.pilotage.infrastructure.client.OrganisationStubAdapter.INGENIEUR_RESEAU;
import static cm.gimac.des.pilotage.infrastructure.client.OrganisationStubAdapter.TECHNICIEN_RESEAU;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cm.gimac.des.pilotage.domain.processus.DocumentSigne;
import cm.gimac.des.pilotage.domain.service.EmpreinteSignature;
import cm.gimac.des.pilotage.infrastructure.client.ReauthentificationStubAdapter;
import cm.gimac.des.pilotage.infrastructure.client.ReauthentificationStubAdapter.Verification;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Exécution d'un processus d'unité (D-87 §2) de bout en bout, sur « Ouverture d'un flux réseau » : lancement
 * (désignations, champs), étapes et tâches liées, tâche à prendre réservée au profil, livrables (MS-DOCUMENT),
 * champs, validation et renvoi, réaffectation, suspension et reprise, signature FIDO2 (faux vérificateur),
 * clôture, événements ; annulation ; droits.
 */
class ProcessusExecutionIT extends AbstractProcessusIT {

    private static Map<String, Object> lancement(String designe) {
        Map<String, Object> l = new HashMap<>();
        l.put("intitule", "Flux supervision vers SIEM");
        l.put("champs", Map.of("SOURCE", "10.0.0.1", "PORT", 443));
        l.put("designations", designe == null ? Map.of() : Map.of("MISE_EN_OEUVRE", designe));
        return l;
    }

    private JsonNode lancer(String processus, String qui, String designe) throws Exception {
        return lire(mvc.perform(json(post(BASE + "/" + processus + "/instances").with(agent(qui)), lancement(designe))).andExpect(status().isCreated()).andReturn());
    }

    private String url(String instance, String etape, String acte) {
        return urlEtape(instance, etape, acte);
    }

    private JsonNode instance(String id, String qui) throws Exception {
        return lire(mvc.perform(get(INSTANCES + "/" + id).with(agent(qui))).andExpect(status().isOk()).andReturn());
    }

    /** État et responsable de la tâche d'attente (validation, signature) de cette référence (D-93). */
    private Map<String, Object> attente(String reference) {
        return jdbc.queryForMap("select etat, responsable_id, profil_requis, unite_cible from tache where origine_type = 'PROCESSUS' and origine_reference = ?", reference);
    }

    private String tache(JsonNode instance, String etape) {
        return etape(instance, etape).get("tacheId").asText();
    }

    @Test
    @DisplayName("Ouverture d'un flux réseau : lancement, étapes, tâches, livrables, validation, renvoi, réaffectation, suspension, signature, clôture")
    void deBoutEnBout() throws Exception {
        String processus = publie("OUVERTURE_FLUX", CHEF_RESEAU);

        // Lancement : membre de l'unité (sans pouvoir), désignation exigée ; hors unité : refus.
        mvc
            .perform(json(post(BASE + "/" + processus + "/instances").with(agent(TECHNICIEN_RESEAU)), lancement(null)))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("PIL_PROCESSUS_DESIGNATION_MANQUANTE"))
            .andExpect(jsonPath("$.details.cles[0]").value(etapeDe(processus, "MISE_EN_OEUVRE")));
        mvc
            .perform(json(post(BASE + "/" + processus + "/instances").with(agent(TECHNICIEN_RESEAU)), lancement(AGENT)))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("PIL_PROCESSUS_DESIGNATION_INVALIDE"));
        mvc.perform(json(post(BASE + "/" + processus + "/instances").with(agent(AGENT)), lancement(INGENIEUR_RESEAU))).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("PIL_PROCESSUS_NON_INITIATEUR"));
        Map<String, Object> inconnue = lancement(INGENIEUR_RESEAU);
        inconnue.put("designations", Map.of("MISE_EN_OEUVRE", INGENIEUR_RESEAU, "ANALYSE", INGENIEUR_RESEAU));
        mvc.perform(json(post(BASE + "/" + processus + "/instances").with(agent(TECHNICIEN_RESEAU)), inconnue)).andExpect(status().isBadRequest()).andExpect(jsonPath("$.details.champ").value("designations." + etapeDe(processus, "ANALYSE")));

        JsonNode i = lancer(processus, TECHNICIEN_RESEAU, INGENIEUR_RESEAU);
        String id = i.get("id").asText();
        assertThat(i.get("etat").asText()).isEqualTo("EN_COURS");
        assertThat(i.at("/processus/code").asText()).isEqualTo(code("OUVERTURE_FLUX"));
        assertThat(i.at("/designations/" + etapeDe(processus, "MISE_EN_OEUVRE") + "/id").asText()).isEqualTo(INGENIEUR_RESEAU);
        JsonNode demande = etape(i, "DEMANDE");
        assertThat(demande.get("etat").asText()).isEqualTo("ACTIVE");
        assertThat(demande.at("/executant/id").asText()).isEqualTo(TECHNICIEN_RESEAU);
        assertThat(demande.at("/moi/executer").asBoolean()).isTrue();
        assertThat(etape(i, "ANALYSE").get("etat").asText()).isEqualTo("EN_ATTENTE");
        assertThat(demande.get("champs").findValuesAsText("code")).contains("SOURCE", "PORT");
        assertThat(demande.at("/champs/2/valeur").asInt()).isEqualTo(443);

        // Tâche liée : origine PROCESSUS, ne se termine pas à la main.
        String tDemande = tache(i, "DEMANDE");
        mvc
            .perform(get("/api/v1/taches/" + tDemande).with(agent(TECHNICIEN_RESEAU)))
            .andExpect(jsonPath("$.origine.type").value("PROCESSUS"))
            .andExpect(jsonPath("$.origine.reference").value(id + "/" + etapeDe(id, "DEMANDE")))
            .andExpect(jsonPath("$.origine.lien").value("/des/processus/instances/" + id + "?etape=" + etapeDe(id, "DEMANDE")));
        mvc.perform(json(post("/api/v1/taches/" + tDemande + "/transition").with(agent(TECHNICIEN_RESEAU)), Map.of("etat", "EN_COURS"))).andExpect(status().isOk());
        mvc
            .perform(json(post("/api/v1/taches/" + tDemande + "/transition").with(agent(TECHNICIEN_RESEAU)), Map.of("etat", "TERMINEE")))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("PIL_TACHE_PROCESSUS"));

        // Fin : champs obligatoires ; valeurs contrôlées ; seul l'exécutant agit.
        mvc
            .perform(post(url(id, "DEMANDE", "fin")).with(agent(TECHNICIEN_RESEAU)))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("PIL_PROCESSUS_CHAMPS_MANQUANTS"))
            .andExpect(jsonPath("$.details.champs", Matchers.containsInAnyOrder("DESTINATION", "PROTOCOLE")));
        mvc
            .perform(json(put(url(id, "DEMANDE", "champs")).with(agent(TECHNICIEN_RESEAU)), Map.of("valeurs", Map.of("PROTOCOLE", "ICMP"))))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("PIL_PROCESSUS_CHAMP_INVALIDE"));
        mvc
            .perform(json(put(url(id, "DEMANDE", "champs")).with(agent(TECHNICIEN_RESEAU)), Map.of("valeurs", Map.of("INCONNU", "x"))))
            .andExpect(status().isBadRequest());
        mvc
            .perform(json(put(url(id, "DEMANDE", "champs")).with(agent(INGENIEUR_RESEAU)), Map.of("valeurs", Map.of("PROTOCOLE", "TCP"))))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("PIL_PROCESSUS_NON_INTERVENANT"));
        mvc
            .perform(json(put(url(id, "DEMANDE", "champs")).with(agent(TECHNICIEN_RESEAU)), Map.of("valeurs", Map.of("PROTOCOLE", "TCP", "DESTINATION", "10.0.9.9", "DATE_SOUHAITEE", "2026-10-20"))))
            .andExpect(status().isOk());
        i = lire(mvc.perform(post(url(id, "DEMANDE", "fin")).with(agent(TECHNICIEN_RESEAU))).andExpect(status().isOk()).andReturn());
        assertThat(etape(i, "DEMANDE").get("etat").asText()).isEqualTo("TERMINEE");
        mvc.perform(get("/api/v1/taches/" + tDemande).with(agent(TECHNICIEN_RESEAU))).andExpect(jsonPath("$.etat").value("TERMINEE"));

        // ANALYSE : à prendre par les porteurs de ROLE_POLE_RESEAU.
        JsonNode analyse = etape(i, "ANALYSE");
        assertThat(analyse.get("etat").asText()).isEqualTo("ACTIVE");
        assertThat(analyse.get("executant").isNull()).isTrue();
        assertThat(analyse.at("/aPrendre/profil").asText()).isEqualTo("ROLE_POLE_RESEAU");
        assertThat(analyse.at("/aPrendre/unite").asText()).isEqualTo("POLE_RESEAU");
        assertThat(analyse.at("/validateur/personne/id").asText()).isEqualTo(CHEF_RESEAU);
        String tAnalyse = tache(i, "ANALYSE");
        mvc.perform(get("/api/v1/taches").param("vue", "unite").with(agent(INGENIEUR_RESEAU))).andExpect(jsonPath("$.contenu[*].id", Matchers.hasItem(tAnalyse)));
        mvc.perform(get("/api/v1/taches").param("vue", "unite").with(chef(CHEF_RESEAU))).andExpect(jsonPath("$.contenu[*].id", Matchers.not(Matchers.hasItem(tAnalyse))));
        JsonNode offre = charge("pilotage.task-offered.v1", tAnalyse);
        assertThat(textes(offre.get("destinataires"))).containsExactlyInAnyOrder(INGENIEUR_RESEAU, TECHNICIEN_RESEAU);
        mvc
            .perform(post("/api/v1/taches/" + tAnalyse + "/prise").with(chef(CHEF_RESEAU)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("PIL_PROFIL_REQUIS"));
        mvc
            .perform(post(url(id, "ANALYSE", "fin")).with(agent(INGENIEUR_RESEAU)))
            // D-138 : porteur du rôle, l'ingénieure agit sans prise ; la fin est refusée faute de livrables, pas de prise.
            .andExpect(status().isUnprocessableEntity());
        assertThat(etape(instance(id, INGENIEUR_RESEAU), "ANALYSE").at("/moi/prendre").asBoolean()).isTrue();
        mvc.perform(post("/api/v1/taches/" + tAnalyse + "/prise").with(agent(INGENIEUR_RESEAU))).andExpect(status().isOk()).andExpect(jsonPath("$.profilRequis").value("ROLE_POLE_RESEAU"));
        i = instance(id, INGENIEUR_RESEAU);
        assertThat(etape(i, "ANALYSE").at("/executant/id").asText()).isEqualTo(INGENIEUR_RESEAU);
        assertThat(i.get("historique").findValuesAsText("action")).contains("LANCEE", "ETAPE_ACTIVEE", "ETAPE_TERMINEE", "ETAPE_PRISE");

        // Livrables : document déposé par l'exécutant pour cette instance, type admis.
        String autre = deposer(id, TECHNICIEN_RESEAU, "note.pdf", "application/pdf");
        mvc
            .perform(json(post(url(id, "ANALYSE", "livrables")).with(agent(INGENIEUR_RESEAU)), Map.of("livrable", "NOTE_ANALYSE", "documentId", autre)))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("PIL_PROCESSUS_DOCUMENT_INVALIDE"));
        String texte = deposer(id, INGENIEUR_RESEAU, "note.txt", "text/plain");
        mvc
            .perform(json(post(url(id, "ANALYSE", "livrables")).with(agent(INGENIEUR_RESEAU)), Map.of("livrable", "NOTE_ANALYSE", "documentId", texte)))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("PIL_PROCESSUS_TYPE_PIECE"));
        mvc
            .perform(json(post(url(id, "ANALYSE", "livrables")).with(agent(INGENIEUR_RESEAU)), Map.of("livrable", "INCONNU", "documentId", texte)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("PIL_PROCESSUS_LIVRABLE_INCONNU"));
        mvc
            .perform(post(url(id, "ANALYSE", "fin")).with(agent(INGENIEUR_RESEAU)))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("PIL_PROCESSUS_LIVRABLES_MANQUANTS"))
            .andExpect(jsonPath("$.details.livrables[0]").value("NOTE_ANALYSE"));
        String note = deposer(id, INGENIEUR_RESEAU, "note.pdf", "application/pdf");
        i = lire(
            mvc.perform(json(post(url(id, "ANALYSE", "livrables")).with(agent(INGENIEUR_RESEAU)), Map.of("livrable", "NOTE_ANALYSE", "documentId", note))).andExpect(status().isOk()).andReturn()
        );
        JsonNode depot = etape(i, "ANALYSE").at("/livrables/0/depot");
        assertThat(depot.get("documentId").asText()).isEqualTo(note);
        assertThat(depot.get("sha256").asText()).matches("^[0-9a-f]{64}$");
        assertThat(depot.at("/deposePar/id").asText()).isEqualTo(INGENIEUR_RESEAU);
        mvc.perform(json(put(url(id, "ANALYSE", "champs")).with(agent(INGENIEUR_RESEAU)), Map.of("valeurs", Map.of("AVIS", "FAVORABLE")))).andExpect(status().isOk());
        i = lire(mvc.perform(post(url(id, "ANALYSE", "fin")).with(agent(INGENIEUR_RESEAU))).andExpect(status().isOk()).andReturn());
        assertThat(etape(i, "ANALYSE").get("etat").asText()).isEqualTo("A_VALIDER");
        // Tâche au validateur (D-93), dans « Mes tâches » du chef d'unité.
        assertThat(attente(id + "/" + etapeDe(id, "ANALYSE") + "/VALIDATION")).containsEntry("etat", "OUVERTE").containsEntry("responsable_id", CHEF_RESEAU);
        mvc
            .perform(get("/api/v1/taches").param("vue", "moi").with(chef(CHEF_RESEAU)))
            .andExpect(jsonPath("$.contenu[*].origine.reference", Matchers.hasItem(id + "/" + etapeDe(id, "ANALYSE") + "/VALIDATION")));
        assertThat(charge("pilotage.process-step-submitted.v1", id).get("attente").asText()).isEqualTo("VALIDATION");
        assertThat(textes(charge("pilotage.process-step-submitted.v1", id).get("destinataires"))).containsExactly(CHEF_RESEAU);

        // Validation : le validateur seul ; renvoi avec motif, l'étape redevient active.
        mvc
            .perform(json(post(url(id, "ANALYSE", "validation")).with(agent(TECHNICIEN_RESEAU)), Map.of("decision", "VALIDER")))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("PIL_PROCESSUS_NON_VALIDATEUR"));
        mvc.perform(json(post(url(id, "ANALYSE", "validation")).with(chef(CHEF_RESEAU)), Map.of("decision", "RENVOYER"))).andExpect(status().isBadRequest());
        i = lire(
            mvc
                .perform(json(post(url(id, "ANALYSE", "validation")).with(chef(CHEF_RESEAU)), Map.of("decision", "RENVOYER", "motif", "Préciser le risque résiduel")))
                .andExpect(status().isOk())
                .andReturn()
        );
        assertThat(etape(i, "ANALYSE").get("etat").asText()).isEqualTo("ACTIVE");
        assertThat(etape(i, "ANALYSE").get("renvois").asInt()).isEqualTo(1);
        assertThat(attente(id + "/" + etapeDe(id, "ANALYSE") + "/VALIDATION")).containsEntry("etat", "TERMINEE");
        JsonNode renvoi = charge("pilotage.process-step-activated.v1", id);
        assertThat(renvoi.get("renvoi").asBoolean()).isTrue();
        assertThat(textes(renvoi.get("destinataires"))).containsExactly(INGENIEUR_RESEAU);
        mvc.perform(post(url(id, "ANALYSE", "fin")).with(agent(INGENIEUR_RESEAU))).andExpect(status().isOk());
        i = lire(mvc.perform(json(post(url(id, "ANALYSE", "validation")).with(chef(CHEF_RESEAU)), Map.of("decision", "VALIDER"))).andExpect(status().isOk()).andReturn());
        assertThat(etape(i, "ANALYSE").get("etat").asText()).isEqualTo("TERMINEE");
        mvc.perform(get("/api/v1/taches/" + tAnalyse).with(agent(INGENIEUR_RESEAU))).andExpect(jsonPath("$.etat").value("TERMINEE"));

        // MISE_EN_OEUVRE : la personne désignée ; réaffectation par le chef de l'unité seulement.
        JsonNode mise = etape(i, "MISE_EN_OEUVRE");
        assertThat(mise.at("/executant/id").asText()).isEqualTo(INGENIEUR_RESEAU);
        mvc
            .perform(json(post(url(id, "MISE_EN_OEUVRE", "reaffectation")).with(agent(TECHNICIEN_RESEAU)), Map.of("personneId", TECHNICIEN_RESEAU, "motif", "Congé")))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("PIL_PROCESSUS_INTERDIT"));
        mvc
            .perform(json(post(url(id, "MISE_EN_OEUVRE", "reaffectation")).with(chef(CHEF_RESEAU)), Map.of("personneId", AGENT, "motif", "Congé")))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("PIL_PROCESSUS_PERSONNE_INVALIDE"));
        i = lire(
            mvc
                .perform(json(post(url(id, "MISE_EN_OEUVRE", "reaffectation")).with(chef(CHEF_RESEAU)), Map.of("personneId", TECHNICIEN_RESEAU, "motif", "Congé de l'ingénieure")))
                .andExpect(status().isOk())
                .andReturn()
        );
        assertThat(etape(i, "MISE_EN_OEUVRE").at("/executant/id").asText()).isEqualTo(TECHNICIEN_RESEAU);
        JsonNode reaffectee = charge("pilotage.process-step-reassigned.v1", id);
        assertThat(reaffectee.get("cause").asText()).isEqualTo("REAFFECTATION");
        assertThat(reaffectee.get("etape").asText()).isEqualTo(etapeDe(id, "MISE_EN_OEUVRE"));
        assertThat(reaffectee.get("libelleEtape").asText()).isEqualTo("Mise en œuvre");
        assertThat(reaffectee.get("ancienId").asText()).isEqualTo(INGENIEUR_RESEAU);
        assertThat(textes(reaffectee.get("destinataires"))).containsExactly(TECHNICIEN_RESEAU);
        mvc.perform(get("/api/v1/taches/" + tache(i, "MISE_EN_OEUVRE")).with(agent(TECHNICIEN_RESEAU))).andExpect(jsonPath("$.responsable.id").value(TECHNICIEN_RESEAU));

        // Suspension et reprise par le chef ; rien ne bouge pendant la suspension.
        mvc.perform(json(post(INSTANCES + "/" + id + "/suspension").with(agent(TECHNICIEN_RESEAU)), Map.of("motif", "Gel"))).andExpect(status().isForbidden());
        mvc.perform(json(post(INSTANCES + "/" + id + "/suspension").with(chef(CHEF_RESEAU)), Map.of())).andExpect(status().isBadRequest());
        mvc
            .perform(json(post(INSTANCES + "/" + id + "/suspension").with(chef(CHEF_RESEAU)), Map.of("motif", "Gel des changements")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.etat").value("SUSPENDUE"))
            .andExpect(jsonPath("$.droits.reprendre").value(true));
        String config = deposer(id, TECHNICIEN_RESEAU, "configuration.txt", "text/plain");
        mvc
            .perform(json(post(url(id, "MISE_EN_OEUVRE", "livrables")).with(agent(TECHNICIEN_RESEAU)), Map.of("livrable", "CONFIGURATION", "documentId", config)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("PIL_PROCESSUS_INSTANCE_ETAT"));
        mvc.perform(json(post(INSTANCES + "/" + id + "/reprise").with(chef(CHEF_RESEAU)), Map.of())).andExpect(status().isOk()).andExpect(jsonPath("$.etat").value("EN_COURS"));
        mvc
            .perform(json(post(url(id, "MISE_EN_OEUVRE", "livrables")).with(agent(TECHNICIEN_RESEAU)), Map.of("livrable", "CONFIGURATION", "documentId", config)))
            .andExpect(status().isOk());
        i = lire(mvc.perform(post(url(id, "MISE_EN_OEUVRE", "fin")).with(agent(TECHNICIEN_RESEAU))).andExpect(status().isOk()).andReturn());

        // RECETTE : prise, procès-verbal, signature du chef d'unité (FIDO2), puis clôture.
        String tRecette = tache(i, "RECETTE");
        mvc.perform(post("/api/v1/taches/" + tRecette + "/prise").with(agent(TECHNICIEN_RESEAU))).andExpect(status().isOk());
        String pv = deposer(id, TECHNICIEN_RESEAU, "pv.pdf", "application/pdf");
        mvc.perform(json(post(url(id, "RECETTE", "livrables")).with(agent(TECHNICIEN_RESEAU)), Map.of("livrable", "PV_RECETTE", "documentId", pv))).andExpect(status().isOk());
        mvc
            .perform(json(post(url(id, "RECETTE", "signature")).with(chef(CHEF_RESEAU)).header("X-Reauth-Assertion", "assertion"), Map.of("signature", "VISA_CHEF")))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("PIL_PROCESSUS_ETAPE_ETAT"));
        i = lire(mvc.perform(post(url(id, "RECETTE", "fin")).with(agent(TECHNICIEN_RESEAU))).andExpect(status().isOk()).andReturn());
        JsonNode recette = etape(i, "RECETTE");
        assertThat(recette.get("etat").asText()).isEqualTo("A_SIGNER");
        assertThat(recette.at("/signatures/0/attendu/personne/id").asText()).isEqualTo(CHEF_RESEAU);
        assertThat(attente(id + "/" + etapeDe(id, "RECETTE") + "/SIGNATURE/VISA_CHEF")).containsEntry("etat", "OUVERTE").containsEntry("responsable_id", CHEF_RESEAU);
        JsonNode attente = charge("pilotage.process-step-submitted.v1", id);
        assertThat(attente.get("attente").asText()).isEqualTo("SIGNATURE");
        assertThat(textes(attente.get("destinataires"))).containsExactly(CHEF_RESEAU);
        assertThat(etape(instance(id, CHEF_RESEAU), "RECETTE").at("/moi/signer/0").asText()).isEqualTo("VISA_CHEF");

        mvc
            .perform(json(post(url(id, "RECETTE", "signature")).with(agent(TECHNICIEN_RESEAU)).header("X-Reauth-Assertion", "assertion"), Map.of("signature", "VISA_CHEF")))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("PIL_PROCESSUS_NON_SIGNATAIRE"));
        mvc
            .perform(json(post(url(id, "RECETTE", "signature")).with(chef(CHEF_RESEAU)), Map.of("signature", "VISA_CHEF")))
            .andExpect(status().isPreconditionRequired())
            .andExpect(jsonPath("$.code").value("PIL_PROCESSUS_REAUTH_REQUISE"));
        mvc
            .perform(
                json(post(url(id, "RECETTE", "signature")).with(chef(CHEF_RESEAU)).header("X-Reauth-Assertion", ReauthentificationStubAdapter.ASSERTION_REFUSEE), Map.of("signature", "VISA_CHEF"))
            )
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("PIL_PROCESSUS_REAUTH_REFUSEE"));
        mvc
            .perform(json(post(url(id, "RECETTE", "signature")).with(chef(CHEF_RESEAU)).header("X-Reauth-Assertion", "assertion"), Map.of("signature", "INCONNUE")))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("PIL_PROCESSUS_SIGNATURE_INCONNUE"));
        i = lire(
            mvc
                .perform(json(post(url(id, "RECETTE", "signature")).with(chef(CHEF_RESEAU)).header("X-Reauth-Assertion", "assertion-fido2"), Map.of("signature", "VISA_CHEF")))
                .andExpect(status().isOk())
                .andReturn()
        );
        assertThat(reauthentification.verifications()).contains(new Verification(CHEF_RESEAU, "SIGNATURE_PROCESSUS", true));
        JsonNode signature = etape(i, "RECETTE").at("/signatures/0");
        assertThat(signature.get("etat").asText()).isEqualTo("SIGNEE");
        assertThat(signature.at("/signePar/id").asText()).isEqualTo(CHEF_RESEAU);
        String shaPv = etape(i, "RECETTE").at("/livrables/0/depot/sha256").asText();
        String shaNote = etape(i, "ANALYSE").at("/livrables/0/depot/sha256").asText();
        String attendue = EmpreinteSignature.calculer(List.of(new DocumentSigne("PV_RECETTE", pv, shaPv), new DocumentSigne("NOTE_ANALYSE", note, shaNote)));
        assertThat(signature.get("empreinte").asText()).isEqualTo(attendue);
        assertThat(etape(i, "RECETTE").get("etat").asText()).isEqualTo("TERMINEE");
        assertThat(i.get("etat").asText()).isEqualTo("CLOSE");
        assertThat(i.get("closeLe").isNull()).isFalse();
        assertThat(i.at("/avancement/terminees").asInt()).isEqualTo(4);

        // Événements (outbox, sujet pilotage.process, clé = instance).
        JsonNode signee = charge("pilotage.process-step-signed.v1", id);
        assertThat(signee.get("empreinte").asText()).isEqualTo(attendue);
        assertThat(signee.get("documents").findValuesAsText("livrable")).containsExactly("PV_RECETTE", "NOTE_ANALYSE");
        assertThat(signee.get("etatEtape").asText()).isEqualTo("TERMINEE");
        assertThat(signee.get("etape").asText()).isEqualTo(etapeDe(id, "RECETTE"));
        assertThat(signee.get("libelleEtape").asText()).isEqualTo("Recette");
        JsonNode close = charge("pilotage.process-instance-closed.v1", id);
        assertThat(close.get("etat").asText()).isEqualTo("CLOSE");
        assertThat(textes(close.get("destinataires"))).containsExactly(TECHNICIEN_RESEAU);
        assertThat(charges("pilotage.process-instance-started.v1", id)).hasSize(1);
        assertThat(charges("pilotage.process-step-activated.v1", id)).hasSize(5);
        assertThat(compter("select count(*) from outbox_event where aggregate_type = 'process' and aggregate_id = ?", processus)).isEqualTo(1);
        assertThat(compter("select count(*) from tache where origine_type = 'PROCESSUS' and etat = 'TERMINEE' and origine_reference not like '%/VALIDATION' and origine_reference not like '%/SIGNATURE/%'")).isEqualTo(4);
        // Tâches d'attente (D-93) : une par validation (rouverte après le renvoi) et par signature, terminées au nom de l'acteur.
        assertThat(attente(id + "/" + etapeDe(id, "ANALYSE") + "/VALIDATION")).containsEntry("etat", "TERMINEE").containsEntry("responsable_id", CHEF_RESEAU);
        assertThat(attente(id + "/" + etapeDe(id, "RECETTE") + "/SIGNATURE/VISA_CHEF")).containsEntry("etat", "TERMINEE").containsEntry("responsable_id", CHEF_RESEAU);

        // Vues : miennes (intervenant), lancées, unité ; hors unité : refus.
        mvc.perform(get(INSTANCES).param("vue", "miennes").with(agent(INGENIEUR_RESEAU))).andExpect(jsonPath("$.total").value(1)).andExpect(jsonPath("$.contenu[0].id").value(id));
        mvc.perform(get(INSTANCES).param("vue", "miennes").with(chef(CHEF_RESEAU))).andExpect(jsonPath("$.total").value(1));
        mvc.perform(get(INSTANCES).param("vue", "lancees").with(agent(TECHNICIEN_RESEAU))).andExpect(jsonPath("$.total").value(1));
        mvc.perform(get(INSTANCES).param("vue", "lancees").with(agent(INGENIEUR_RESEAU))).andExpect(jsonPath("$.total").value(0));
        mvc.perform(get(INSTANCES).param("vue", "unite").param("etat", "CLOSE").with(chef(CHEF_DES))).andExpect(jsonPath("$.total").value(1));
        mvc.perform(get(INSTANCES).param("vue", "unite").with(agent(AGENT))).andExpect(jsonPath("$.total").value(0));
        mvc.perform(get(INSTANCES).param("vue", "autre").with(agent(AGENT))).andExpect(status().isBadRequest());
        mvc.perform(get(INSTANCES + "/" + id).with(agent(AGENT))).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("PIL_PROCESSUS_INTERDIT"));
        mvc.perform(get(INSTANCES + "/" + id).with(utilisateur(INGENIEUR_RESEAU))).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Annulation par le chef de l'unité : étapes et tâches annulées, lanceur averti ; actes refusés ensuite")
    void annulation() throws Exception {
        String processus = publie("OUVERTURE_FLUX_B", CHEF_RESEAU);
        JsonNode i = lancer(processus, TECHNICIEN_RESEAU, INGENIEUR_RESEAU);
        String id = i.get("id").asText();
        String tDemande = tache(i, "DEMANDE");
        mvc.perform(json(post(INSTANCES + "/" + id + "/annulation").with(agent(TECHNICIEN_RESEAU)), Map.of("motif", "Doublon"))).andExpect(status().isForbidden());
        // Le chef du DES dirige le pôle réseau (sous-unité de ses unités dirigées).
        i = lire(mvc.perform(json(post(INSTANCES + "/" + id + "/annulation").with(chef(CHEF_DES)), Map.of("motif", "Demande en doublon"))).andExpect(status().isOk()).andReturn());
        assertThat(i.get("etat").asText()).isEqualTo("ANNULEE");
        assertThat(i.get("motif").asText()).isEqualTo("Demande en doublon");
        assertThat(i.get("etapes").findValuesAsText("etat")).containsOnly("ANNULEE");
        mvc.perform(get("/api/v1/taches/" + tDemande).with(agent(TECHNICIEN_RESEAU))).andExpect(jsonPath("$.etat").value("ANNULEE"));
        JsonNode close = charge("pilotage.process-instance-closed.v1", id);
        assertThat(close.get("etat").asText()).isEqualTo("ANNULEE");
        assertThat(textes(close.get("destinataires"))).containsExactly(TECHNICIEN_RESEAU);
        mvc.perform(post(url(id, "DEMANDE", "fin")).with(agent(TECHNICIEN_RESEAU))).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("PIL_PROCESSUS_INSTANCE_ETAT"));
        mvc.perform(json(post(INSTANCES + "/" + id + "/annulation").with(chef(CHEF_DES)), Map.of("motif", "Encore"))).andExpect(status().isConflict());
        mvc.perform(get(INSTANCES + "/" + java.util.UUID.randomUUID()).with(agent(AGENT))).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("PIL_PROCESSUS_INSTANCE_INTROUVABLE"));
    }

    @Test
    @DisplayName("Personne nommée partie : à l'activation, l'étape passe au profil de remplacement (seul porteur : à lui) et le chef est averti")
    void personnePartieALActivation() throws Exception {
        Map<String, Object> d = new HashMap<>(json.convertValue(definition("NOMMEE"), Map.class));
        Map<String, Object> etape = new HashMap<>();
        etape.put("code", "UNIQUE");
        etape.put("libelle", "Étape nommée");
        etape.put("intervenant", Map.of("mode", "PERSONNE", "personneId", INGENIEUR_RESEAU, "remplacement", "ROLE_POLE_RESEAU"));
        d.put("etapes", List.of(etape));
        String processus = creer(chef(CHEF_RESEAU), d);
        publier(chef(CHEF_RESEAU), processus);
        organisation.enregistrer(
            new cm.gimac.des.pilotage.infrastructure.client.OrganisationStubAdapter.Compte(INGENIEUR_RESEAU, "Ingrid Mballa", "Ingénieure réseau", "POLE_RESEAU", CHEF_RESEAU, false, true)
        );
        mvc.perform(get(BASE + "/" + processus).with(chef(CHEF_RESEAU))).andExpect(jsonPath("$.avertissements[0].code").value("PERSONNE_PARTIE"));
        JsonNode i = lire(mvc.perform(json(post(BASE + "/" + processus + "/instances").with(agent(TECHNICIEN_RESEAU)), Map.of("intitule", "Essai"))).andExpect(status().isCreated()).andReturn());
        // Seul porteur actif restant du profil de remplacement dans l'unité : l'étape lui est confiée (désignation hors de l'unité).
        JsonNode e = etape(i, "UNIQUE");
        assertThat(e.at("/executant/id").asText()).isEqualTo(TECHNICIEN_RESEAU);
        assertThat(e.get("aPrendre").isNull()).isTrue();
        String id = i.get("id").asText();
        JsonNode depart = charge("pilotage.process-step-reassigned.v1", id);
        assertThat(depart.get("cause").asText()).isEqualTo("DEPART");
        assertThat(depart.get("etape").asText()).isEqualTo(etapeDe(id, "UNIQUE"));
        assertThat(depart.get("libelleEtape").asText()).isEqualTo("Étape nommée");
        assertThat(depart.get("ancienId").asText()).isEqualTo(INGENIEUR_RESEAU);
        assertThat(depart.get("executantId").asText()).isEqualTo(TECHNICIEN_RESEAU);
        assertThat(depart.get("chefId").asText()).isEqualTo(CHEF_RESEAU);
        assertThat(textes(depart.get("destinataires"))).containsExactlyInAnyOrder(TECHNICIEN_RESEAU, CHEF_RESEAU);
        assertThat(i.get("historique").findValuesAsText("action")).contains("DEPART");
    }
}
