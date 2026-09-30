package cm.gimac.des.homologation.web.rest.lot1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cm.gimac.des.homologation.domain.enumeration.TypeProjet;
import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

/**
 * Portail du participant sur PostgreSQL et Kafka réels : liaisons de l'institution du jeton, dossier
 * projeté sans nom interne, tâches avec leur geste, coordonnées d'essai (rang 12), livrable déposé pour
 * le compte du participant, fiche de suivi ; toute liaison d'une autre institution est introuvable.
 */
class ParcoursPortailIT extends ParcoursITSupport {

    @Test
    @DisplayName("Cloisonnement, projection sans nom interne, coordonnées, livrable, tâches")
    void portail() throws Exception {
        UUID ep = adherent("Émetteur Portail", "FINTECH_EME", "REMITTANCE", "FACTURE");
        UUID autre = adherent("Autre Émetteur", "ETABLISSEMENT_PAIEMENT", "REMITTANCE");
        String id = ouvrir(ep, TypeProjet.PAIEMENT_MOBILE);
        String etranger = ouvrir(autre, TypeProjet.PAIEMENT_MOBILE);

        mvc
            .perform(get("/api/v1/portail/liaisons").with(correspondant(ep)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[*].id").value(Matchers.contains(id)))
            .andExpect(jsonPath("$[0].chefDeProjetNom").value("Conducteur de projet GIMAC"))
            .andExpect(jsonPath("$[0].chefDeProjetId").isEmpty())
            .andExpect(jsonPath("$[0].statut").value("CADREE"));
        mvc
            .perform(get("/api/v1/portail/liaisons/" + etranger + "/dossier").with(correspondant(ep)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("HOM_LIAISON_NOT_FOUND"));
        mvc.perform(get("/api/v1/portail/liaisons/" + etranger + "/fiche-suivi").with(correspondant(ep))).andExpect(status().isNotFound());
        mvc
            .perform(put("/api/v1/portail/liaisons/" + etranger + "/coordonnees-techniques").with(correspondant(ep)).contentType(MediaType.APPLICATION_JSON).content("{\"adresse\":\"essai.exemple\",\"port\":\"8443\"}"))
            .andExpect(status().isNotFound());
        mvc.perform(multipart("/api/v1/portail/operations/" + opId(etranger, "3") + "/livrable").file(new MockMultipartFile("fichier", "x.pdf", "application/pdf", "x".getBytes())).with(correspondant(ep))).andExpect(status().isNotFound());

        // Coordonnées d'essai : contrôlées, attendues au rang 12 seulement, puis relisibles.
        mvc
            .perform(put("/api/v1/portail/liaisons/" + id + "/coordonnees-techniques").with(correspondant(ep)).contentType(MediaType.APPLICATION_JSON).content("{\"adresse\":\"hôte invalide!\",\"port\":\"8443\"}"))
            .andExpect(status().is(422))
            .andExpect(jsonPath("$.code").value("HOM_TECHNICAL_DATA_INVALID"));
        mvc
            .perform(put("/api/v1/portail/liaisons/" + id + "/coordonnees-techniques").with(correspondant(ep)).contentType(MediaType.APPLICATION_JSON).content("{\"adresse\":\"essai.exemple\",\"port\":\"8443\"}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("HOM_OPERATION_NOT_OPEN"));
        avancer(id, "8", ep);

        // Livrable du participant (atelier, rang 8) déposé en multipart pour son compte.
        String o8 = opId(id, "8");
        mvc
            .perform(get("/api/v1/portail/taches").with(correspondant(ep)))
            .andExpect(jsonPath("$.taches[?(@.rang == '8')].lien").value("/portail/liaisons/" + id + "/livrables/" + o8))
            .andExpect(jsonPath("$.taches[?(@.rang == '8')].actionsPermises[0]").value("DEPOSER_LIVRABLE"))
            .andExpect(jsonPath("$.taches[*].liaisonId").value(Matchers.everyItem(Matchers.is(id))));
        mvc
            .perform(multipart("/api/v1/portail/operations/" + opId(id, "13") + "/livrable").file(new MockMultipartFile("fichier", "x.pdf", "application/pdf", "x".getBytes())).with(correspondant(ep)))
            .andExpect(status().isConflict());
        mvc
            .perform(multipart("/api/v1/portail/operations/" + o8 + "/livrable").file(new MockMultipartFile("fichier", "cr-atelier.pdf", "application/pdf", "compte rendu".getBytes())).param("commentaire", "relu").with(correspondant(ep)))
            .andExpect(status().isNoContent());
        mvc
            .perform(get("/api/v1/liaisons/" + id + "/parcours/operations").param("etape", "ATELIERS").with(conducteur()))
            .andExpect(jsonPath("$[0].etat").value("FAITE"))
            .andExpect(jsonPath("$[0].preuve.nom").value("cr-atelier.pdf"))
            .andExpect(jsonPath("$[0].preuve.deposePar").value("Awa Correspondante"))
            .andExpect(jsonPath("$[0].preuve.empreinte").value(Matchers.matchesPattern("[0-9a-f]{64}")));
        avancer(id, "12", ep);
        mvc
            .perform(get("/api/v1/portail/taches").with(correspondant(ep)))
            .andExpect(jsonPath("$.taches[?(@.rang == '12')].actionsPermises[0]").value("COMMUNIQUER_COORDONNEES"))
            .andExpect(jsonPath("$.compteurs[?(@.cle == 'tachesARealiser')].valeur").exists());
        mvc
            .perform(put("/api/v1/portail/liaisons/" + id + "/coordonnees-techniques").with(correspondant(ep)).contentType(MediaType.APPLICATION_JSON).content("{\"adresse\":\"essai.exemple\",\"port\":\"9443\",\"pointsTerminaison\":\"https://essai.exemple/tx\"}"))
            .andExpect(status().isNoContent());
        String dossier = mvc
            .perform(get("/api/v1/portail/liaisons/" + id + "/dossier").with(correspondant(ep)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.coordonneesTechniques.adresse").value("essai.exemple"))
            .andExpect(jsonPath("$.coordonneesTechniques.port").value("9443"))
            .andExpect(jsonPath("$.coordonneesTechniques.pointsTerminaison").value("https://essai.exemple/tx"))
            .andExpect(jsonPath("$.conducteur.nom").value("Conducteur de projet GIMAC"))
            .andExpect(jsonPath("$.actionsPermises").isEmpty())
            .andExpect(jsonPath("$.statut").value("EN_INTEGRATION"))
            .andReturn()
            .getResponse()
            .getContentAsString();
        assertThat(dossier).doesNotContain("Aminata Ngono").doesNotContain("Ingénieur Normes").doesNotContain("Chargé DDRM");
        assertThat(JsonPath.<String>read(lire("/api/v1/interne/liaisons/" + id + "/contexte-certification", service()), "$.institutionNom")).isEqualTo("Émetteur Portail");
        assertThat(JsonPath.<Boolean>read(lire("/api/v1/interne/liaisons/" + id + "/contexte-certification", service()), "$.fintech")).isTrue();
        mvc.perform(get("/api/v1/portail/taches").with(correspondant(ep))).andExpect(jsonPath("$.taches[?(@.rang == '12')]").isEmpty());

        // Fiche de suivi : lue sur le portail, non ouverte à la saisie avant la remise.
        mvc.perform(get("/api/v1/portail/liaisons/" + id + "/fiche-suivi").with(correspondant(ep))).andExpect(jsonPath("$.etat").value("A_REMETTRE")).andExpect(jsonPath("$.actionsPermises").isEmpty());
        mvc
            .perform(put("/api/v1/portail/liaisons/" + id + "/fiche-suivi").with(correspondant(ep)).contentType(MediaType.APPLICATION_JSON).content("{\"lignes\":[]}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("HOM_SERVICE_SHEET_NOT_OPEN"));
    }
}
