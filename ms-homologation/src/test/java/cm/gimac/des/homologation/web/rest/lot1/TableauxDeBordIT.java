package cm.gimac.des.homologation.web.rest.lot1;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cm.gimac.des.homologation.domain.enumeration.TypeProjet;
import java.util.UUID;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/**
 * Tableaux de bord par fonction, pilotage et intervenants sur PostgreSQL réel : compteurs et liens calculés
 * par le service, fonctions retenues selon les profils du jeton, six questions de l'annexe B.
 */
class TableauxDeBordIT extends ParcoursITSupport {

    @Test
    @DisplayName("Compteurs par fonction, tâches, pilotage et intervenants ; fonction non détenue refusée")
    void tableaux() throws Exception {
        UUID a = adherent("Banque Tableaux", "BANQUE", "RETRAIT");
        UUID b = adherent("Émetteur Tableaux", "ETABLISSEMENT_PAIEMENT", "REMITTANCE");
        String carte = ouvrir(a, TypeProjet.CARTE_INTERBANCAIRE);
        String mobile = ouvrir(b, TypeProjet.PAIEMENT_MOBILE);
        avancer(carte, "17", a);
        avancer(mobile, "8", b);
        String blocage = "{\"motif\":\"Pièce manquante\",\"prochaineAction\":\"Relancer\",\"responsableId\":\"" + SNA + "\",\"echeanceLevee\":\"2026-12-31\"}";
        mvc.perform(post("/api/v1/liaisons/" + mobile + "/blocages").with(conducteur()).contentType(MediaType.APPLICATION_JSON).content(blocage)).andExpect(status().isCreated());

        mvc
            .perform(get("/api/v1/tableaux-de-bord/acteur").param("fonctions", "SNA").with(sna()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.fonction").value("SNA"))
            .andExpect(jsonPath("$.compteurs[?(@.cle == 'couverturesAConstater')].valeur").value(Matchers.hasItem(Matchers.greaterThanOrEqualTo(1))))
            .andExpect(jsonPath("$.taches[?(@.rang == '17')].lien").value(Matchers.hasItem(Matchers.containsString("/des/parcours/" + carte + "?onglet=operations&operation="))));
        mvc
            .perform(get("/api/v1/tableaux-de-bord/acteur").param("fonctions", "CONDUCTEUR").with(conducteur()))
            .andExpect(jsonPath("$.compteurs[?(@.cle == 'mesProjets')].valeur").value(Matchers.hasItem(Matchers.greaterThanOrEqualTo(2))))
            .andExpect(jsonPath("$.liaisons[*].id").value(Matchers.hasItems(carte, mobile)))
            .andExpect(jsonPath("$.blocages[*].liaisonId").value(Matchers.hasItem(mobile)));
        mvc
            .perform(get("/api/v1/tableaux-de-bord/acteur").param("fonctions", "CHEF_DDRM").with(conducteur()))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("HOM_FUNCTION_NOT_HELD"));
        mvc.perform(get("/api/v1/tableaux-de-bord/acteur").with(chefDes())).andExpect(jsonPath("$.fonction").value("CHEF_DES"));
        mvc
            .perform(get("/api/v1/tableaux-de-bord/pilotage").with(chefDes()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.projetsActifs").value(Matchers.greaterThanOrEqualTo(2)))
            .andExpect(jsonPath("$.projetsBloques").value(Matchers.greaterThanOrEqualTo(1)))
            .andExpect(jsonPath("$.repartitionParEtape.length()").value(9))
            .andExpect(jsonPath("$.questions.projetsBloques[*].liaisonId").value(Matchers.hasItem(mobile)))
            .andExpect(jsonPath("$.questions.servicesPlusDemandes").isNotEmpty())
            .andExpect(jsonPath("$.questions.lancementsParService12Mois").isNotEmpty());
        mvc.perform(get("/api/v1/intervenants").param("fonction", "SNA").with(conducteur())).andExpect(jsonPath("$[*].id").value(Matchers.hasItem(SNA.toString())));
        mvc.perform(get("/api/v1/intervenants").param("fonction", "CONDUCTEUR").with(conducteur())).andExpect(jsonPath("$[*].nom").value(Matchers.hasItem("Aminata Ngono")));
    }

    @Test
    @DisplayName("D-98 : mes opérations par personne (à faire, à prendre, à valider, à suivre, jalons) ; preuve du chargé DDRM tel que livré")
    void mesOperations() throws Exception {
        UUID a = adherent("Banque Mes Actions", "BANQUE", "RETRAIT");
        String liaison = ouvrir(a, TypeProjet.CARTE_INTERBANCAIRE);
        String o3 = opId(liaison, "3");
        String filtre = "$.aAgir[?(@.operationId == '" + o3 + "')]";
        // Chargé de la DDRM tel que livré (HOM_STEP_MANAGE, sans HOM_EVIDENCE_SUBMIT) : la preuve du rang 3 est à prendre.
        org.springframework.test.web.servlet.request.RequestPostProcessor chargeDdrm = utilisateur(DDRM, avec(COMMUN, "ROLE_CHARGE_DDRM", "HOM_STEP_MANAGE", "HOM_FUNCTION_DDRM"));
        mvc
            .perform(get("/api/v1/parcours/mes-operations").with(chargeDdrm))
            .andExpect(status().isOk())
            .andExpect(jsonPath(filtre + ".qualite").value(Matchers.hasItem("R")))
            .andExpect(jsonPath(filtre + ".aPrendre").value(Matchers.hasItem(true)))
            .andExpect(jsonPath(filtre + ".actionsPermises[*]").value(Matchers.hasItem("DEPOSER_PREUVE")))
            .andExpect(jsonPath(filtre + ".lien").value(Matchers.hasItem("/des/parcours/" + liaison + "?onglet=operations&operation=" + o3)))
            .andExpect(jsonPath(filtre + ".liaisonReference").value(Matchers.hasItem(Matchers.startsWith("LIA-"))));
        String corps = "{\"documentId\":\"" + UUID.randomUUID() + "\",\"empreinte\":\"" + SHA + "\",\"nom\":\"perimetre.pdf\"}";
        mvc.perform(post("/api/v1/parcours/operations/" + o3 + "/preuves").with(chargeDdrm).contentType(MediaType.APPLICATION_JSON).content(corps)).andExpect(status().isNoContent());
        // Le conducteur, validateur désigné, la reçoit à valider ; il suit sa liaison et ses jalons.
        String liaisonSuivie = "$.liaisons[?(@.liaisonId == '" + liaison + "')]";
        mvc
            .perform(get("/api/v1/parcours/mes-operations").with(conducteur()))
            .andExpect(status().isOk())
            .andExpect(jsonPath(filtre + ".qualite").value(Matchers.hasItem("V")))
            .andExpect(jsonPath(filtre + ".etat").value(Matchers.hasItem("EN_VALIDATION")))
            .andExpect(jsonPath(filtre + ".actionsPermises[*]").value(Matchers.hasItems("VALIDER", "RENVOYER")))
            .andExpect(jsonPath(liaisonSuivie + ".conducteur").value(Matchers.hasItem(true)))
            .andExpect(jsonPath(liaisonSuivie + ".statut").value(Matchers.hasItem("CADREE")))
            .andExpect(jsonPath(liaisonSuivie + ".jalons[*].jalon").value(Matchers.hasItems("CERTIFIEE", "EN_PRODUCTION")))
            .andExpect(jsonPath(liaisonSuivie + ".jalons[?(@.jalon == 'EN_PRODUCTION')].atteint").value(Matchers.hasItem(false)));
        // Le chargé ne l'a plus à faire ; une personne étrangère à la liaison ne la voit pas.
        mvc.perform(get("/api/v1/parcours/mes-operations").with(chargeDdrm)).andExpect(jsonPath(filtre).value(Matchers.empty()));
        mvc
            .perform(get("/api/v1/parcours/mes-operations").with(utilisateur(UUID.randomUUID(), "CER_SECURITY_VERIFY")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.aAgir[?(@.liaisonId == '" + liaison + "')]").value(Matchers.empty()))
            .andExpect(jsonPath(liaisonSuivie).value(Matchers.empty()));
    }
}
