package cm.gimac.des.homologation.web.rest.lot1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cm.gimac.des.homologation.domain.cadrage.CollaboratorView;
import cm.gimac.des.homologation.domain.enumeration.TypeProjet;
import com.jayway.jsonpath.JsonPath;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/**
 * D-93 et D-94 de bout en bout : conducteur et pilote par permission (un profil créé qui reçoit la permission en obtient
 * l'effet, un profil qui la perd ne l'a plus), dossier lu dans les vraies étapes de la version et le RACI, vue et
 * livrables des opérations, fonctions de l'appelant.
 */
class EffetReelReferentielsIT extends ParcoursITSupport {

    static final UUID CREE = UUID.fromString("00000000-0000-4000-8000-0000000e0001");

    @AfterEach
    void compositions() {
        annuaire.reinitialiserCompositions();
    }

    String ouverture(UUID institution, UUID conducteur) {
        return "{\"institutionId\":\"%s\",\"typeProjet\":\"%s\",\"chefDeProjetId\":\"%s\"}".formatted(institution, TypeProjet.DELEGATAIRE, conducteur);
    }

    @Test
    @DisplayName("Conducteur et pilote par permission : un profil créé qui reçoit HOM_PROJECT_CONDUCT ou HOM_REFERENTIEL_EDIT en obtient l'effet")
    void parPermission() throws Exception {
        annuaire.enregistrer(new CollaboratorView(CREE, "Profil créé", Set.of("ROLE_PROFIL_CREE"), null, true));
        fixtures.referentielPublie(TypeProjet.DELEGATAIRE);
        UUID ep = adherent("Établissement Permission", "ETABLISSEMENT_PAIEMENT", "RETRAIT");
        mvc
            .perform(post("/api/v1/liaisons").with(chefDes()).contentType(MediaType.APPLICATION_JSON).content(ouverture(ep, CREE)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("HOM_LIAISON_PROJECT_LEAD_NOT_HABILITATED"));
        assertThat(JsonPath.<List<String>>read(lire("/api/v1/liaisons/conducteurs", chefDes()), "$[*].id")).doesNotContain(CREE.toString());
        annuaire.composer("ROLE_PROFIL_CREE", Set.of("HOM_PROJECT_CONDUCT", "HOM_REFERENTIEL_EDIT"));
        assertThat(JsonPath.<List<String>>read(lire("/api/v1/liaisons/conducteurs", chefDes()), "$[*].id")).contains(CREE.toString());
        mvc.perform(post("/api/v1/liaisons").with(chefDes()).contentType(MediaType.APPLICATION_JSON).content(ouverture(ep, CREE))).andExpect(status().isCreated());
        assertThat(JsonPath.<List<String>>read(lire("/api/v1/referentiels/catalogue", chefDes()), "$.pilotes[*].id")).contains(CREE.toString());
        annuaire.reinitialiserCompositions();
        assertThat(JsonPath.<List<String>>read(lire("/api/v1/referentiels/catalogue", chefDes()), "$.pilotes[*].id")).doesNotContain(CREE.toString());
    }

    @Test
    @DisplayName("Dossier : étapes réelles de la version, intervenants lus du RACI, vue et livrables des opérations, fonctions de l'appelant")
    void dossier() throws Exception {
        UUID ep = adherent("Établissement Dossier", "ETABLISSEMENT_PAIEMENT", "RETRAIT");
        String liaison = ouvrir(ep, TypeProjet.CARTE_INTERBANCAIRE);
        mvc
            .perform(get("/api/v1/liaisons/" + liaison + "/dossier").with(conducteur()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.etapesVersion.length()").value(11))
            .andExpect(jsonPath("$.etapesVersion[2].code").value("CADRAGE"))
            .andExpect(jsonPath("$.etapesVersion[2].jeton").isNotEmpty())
            .andExpect(jsonPath("$.etapesVersion[0].etat").value("CLOSE"))
            .andExpect(jsonPath("$.intervenants[?(@.fonction == 'DDRM')].aAffecter").value(Matchers.contains(true)))
            .andExpect(jsonPath("$.intervenants[?(@.fonction == 'CONDUCTEUR')].personne.id").value(Matchers.contains(cm.gimac.des.homologation.infrastructure.client.CollaboratorStubAdapter.CONDUCTEUR_1.toString())))
            .andExpect(jsonPath("$.responsableFonctionnel").doesNotExist());
        mvc
            .perform(get("/api/v1/liaisons/" + liaison + "/parcours/operations").with(conducteur()))
            .andExpect(jsonPath("$[?(@.rang == '6')].vue").value(Matchers.contains("AFFECTATION")))
            .andExpect(jsonPath("$[?(@.rang == '12')].vue").value(Matchers.contains("COORDONNEES")))
            .andExpect(jsonPath("$[?(@.rang == '3')].etapeCode").value(Matchers.contains("CADRAGE")))
            .andExpect(jsonPath("$[*].livrablesAttendus[*].obligatoire").value(Matchers.hasItem(true)));
        mvc
            .perform(get("/api/v1/moi/fonctions").with(utilisateur(UUID.randomUUID(), "ROLE_CONDUCTEUR_PROJET")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[*].code").value(Matchers.contains("CONDUCTEUR")));
        mvc
            .perform(get("/api/v1/referentiels/catalogue").with(chefDes()))
            .andExpect(jsonPath("$.vuesOperation[*].code").value(Matchers.hasItems("AUCUNE", "AFFECTATION", "CEREMONIE")));
    }
}
