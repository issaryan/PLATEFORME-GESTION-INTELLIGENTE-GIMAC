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
}
