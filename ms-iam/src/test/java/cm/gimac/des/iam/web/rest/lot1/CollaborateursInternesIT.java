package cm.gimac.des.iam.web.rest.lot1;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cm.gimac.des.iam.AbstractIamIT;
import java.util.UUID;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Annuaire interne pour la messagerie de MS-COLLABORATION (D-64), de bout en bout : jeton de service
 * INTERNAL_IAM_READ, comptes internes actifs seulement, recherche et pagination ; la liste par profil
 * de MS-HOMOLOGATION (D-58) reste inchangée.
 */
class CollaborateursInternesIT extends AbstractIamIT {

    static final RequestPostProcessor SERVICE = jwt().authorities(new SimpleGrantedAuthority("INTERNAL_IAM_READ"));

    @Test
    @DisplayName("Comptes internes actifs, filtrés et paginés ; correspondant d'un participant et compte désactivé exclus")
    void annuaire() throws Exception {
        String suffixe = UUID.randomUUID().toString().substring(0, 6);
        String interne = creer("messagerie." + suffixe, "Chargée de la messagerie " + suffixe, null);
        creer("correspondant." + suffixe, "Correspondant " + suffixe, "INST-0042");
        String parti = creer("parti." + suffixe, "Agent parti " + suffixe, null);
        mvc
            .perform(
                post("/api/v1/comptes/{id}/desactivation", parti)
                    .with(utilisateur(SUPER_ADMIN, PERMISSIONS_SUPER_ADMIN))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"motif\":\"Départ\"}")
            )
            .andExpect(status().isOk());

        mvc
            .perform(get("/api/v1/interne/collaborateurs?recherche=" + suffixe).with(SERVICE))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total").value(1))
            .andExpect(jsonPath("$.contenu[0].id").value(interne))
            .andExpect(jsonPath("$.contenu[0].fonction").value("Chargée de la messagerie " + suffixe))
            .andExpect(jsonPath("$.contenu[0].actif").value(true))
            .andExpect(jsonPath("$.contenu[0].email").doesNotExist());
        mvc
            .perform(get("/api/v1/interne/collaborateurs?page=0&taille=1").with(SERVICE))
            .andExpect(jsonPath("$.contenu.length()").value(1))
            .andExpect(jsonPath("$.taille").value(1))
            .andExpect(jsonPath("$.total").value(Matchers.greaterThanOrEqualTo(3)));
        mvc
            .perform(get("/api/v1/interne/collaborateurs?recherche=RSSI&taille=50").with(SERVICE))
            .andExpect(jsonPath("$.contenu[*].id", Matchers.hasItem(RSSI)))
            .andExpect(jsonPath("$.contenu[*].institutionId", Matchers.everyItem(Matchers.nullValue())));
        mvc.perform(get("/api/v1/interne/collaborateurs?profil=ROLE_RSSI").with(SERVICE)).andExpect(jsonPath("$[*].id", Matchers.hasItem(RSSI)));
        mvc.perform(get("/api/v1/interne/collaborateurs").with(utilisateur(SUPER_ADMIN, PERMISSIONS_SUPER_ADMIN))).andExpect(status().isForbidden());
    }

    private String creer(String identifiant, String fonction, String institution) throws Exception {
        String corps = """
            {"identifiant":"%s","prenom":"Awa","nom":"Mballa","email":"%s@gimac.test","telephone":"+237600000009","fonction":"%s",%s"profils":["ROLE_CHARGE_DDRM"],"envoyerInvitation":true}"""
            .formatted(identifiant, identifiant, fonction, institution == null ? "" : "\"institutionId\":\"" + institution + "\",");
        String reponse = mvc
            .perform(post("/api/v1/comptes").with(utilisateur(SUPER_ADMIN, PERMISSIONS_SUPER_ADMIN)).contentType(MediaType.APPLICATION_JSON).content(corps))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
        return com.jayway.jsonpath.JsonPath.read(reponse, "$.compte.id");
    }
}
