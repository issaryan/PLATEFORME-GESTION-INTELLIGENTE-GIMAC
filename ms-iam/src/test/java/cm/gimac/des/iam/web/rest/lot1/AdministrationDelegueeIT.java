package cm.gimac.des.iam.web.rest.lot1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cm.gimac.des.iam.AbstractIamIT;
import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Administration déléguée de la sandbox (D-115, US-EXI-03) de bout en bout : permission SBX_HAB_ADMINISTRATION,
 * institution lue dans le jeton, compte d'une autre institution indistinguable d'un inexistant, rejeu d'une étape de
 * saga par sa clé d'idempotence (table etape_idempotente), sans second compte.
 */
class AdministrationDelegueeIT extends AbstractIamIT {

    static RequestPostProcessor administrateur(String sujet, String institution) {
        return jwt()
            .jwt(j -> j.subject(sujet).claim("acr", "2").claim("name", "Administrateur " + institution).claim("institution_id", institution))
            .authorities(new SimpleGrantedAuthority("SBX_HAB_ADMINISTRATION"));
    }

    private String creer(String institution, String identifiant, String cle) throws Exception {
        var r = mvc
            .perform(
                post("/api/v1/administration-deleguee/comptes")
                    .with(administrateur("admin-" + institution, institution))
                    .header("Idempotency-Key", cle)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"identifiant\":\"" + identifiant + "\",\"prenom\":\"Awa\",\"nom\":\"Ndiaye\",\"email\":\"" + identifiant + "@institution.test\"," +
                        "\"roles\":[\"ROLE_SBX_TESTEUR\"]}"
                    )
            )
            .andReturn();
        assertThat(r.getResponse().getStatus()).as(r.getResponse().getContentAsString()).isIn(200, 201);
        return JsonPath.read(r.getResponse().getContentAsString(), "$.compte.id");
    }

    @Test
    @DisplayName("Création rattachée à l'institution du jeton, rejouée par sa clé sans second compte, listée dans l'institution seule")
    void creationRejouee() throws Exception {
        String s = UUID.randomUUID().toString().substring(0, 6);
        String cle = "saga-" + s + "/etape-1";
        String id = creer("99001", "testeur." + s, cle);
        mvc
            .perform(get("/api/v1/administration-deleguee/comptes/{id}", id).with(administrateur("admin-99001", "99001")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.institutionId").value("99001"))
            .andExpect(jsonPath("$.profils[0]").value("ROLE_SBX_TESTEUR"));
        assertThat(creer("99001", "testeur." + s, cle)).isEqualTo(id);
        Integer lignes = jdbc.queryForObject("select count(*) from etape_idempotente where cle = ?", Integer.class, cle);
        assertThat(lignes).isEqualTo(1);
        // Autre institution : comme un compte inexistant.
        mvc.perform(get("/api/v1/administration-deleguee/comptes/{id}", id).with(administrateur("admin-99002", "99002"))).andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("IAM_ACCOUNT_NOT_FOUND"));
        mvc
            .perform(
                put("/api/v1/administration-deleguee/comptes/{id}/roles", id)
                    .with(administrateur("admin-99002", "99002"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"roles\":[\"ROLE_SBX_LECTEUR\"],\"motif\":\"x\"}")
            )
            .andExpect(status().isNotFound());
        mvc
            .perform(get("/api/v1/administration-deleguee/comptes?taille=100").with(administrateur("admin-99002", "99002")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.contenu[?(@.id == '" + id + "')]").isEmpty());
        mvc
            .perform(get("/api/v1/administration-deleguee/comptes?taille=100").with(administrateur("admin-99001", "99001")))
            .andExpect(jsonPath("$.contenu[?(@.id == '" + id + "')].institutionId").value(org.hamcrest.Matchers.contains("99001")));
    }

    @Test
    @DisplayName("Rôles et état : remplacés et suspendu dans l'institution ; rôle hors portée refusé ; rôles délégables listés")
    void rolesEtEtat() throws Exception {
        String s = UUID.randomUUID().toString().substring(0, 6);
        String id = creer("99001", "lecteur." + s, null);
        mvc
            .perform(
                put("/api/v1/administration-deleguee/comptes/{id}/roles", id)
                    .with(administrateur("admin-99001", "99001"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"roles\":[\"ROLE_SBX_LECTEUR\"],\"motif\":\"Lecture seule\"}")
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.profils").value(org.hamcrest.Matchers.contains("ROLE_SBX_LECTEUR")));
        mvc
            .perform(
                put("/api/v1/administration-deleguee/comptes/{id}/roles", id)
                    .with(administrateur("admin-99001", "99001"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"roles\":[\"ROLE_SUPER_ADMIN\"],\"motif\":\"x\"}")
            )
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("IAM_SBX_ROLE_HORS_PORTEE"));
        mvc
            .perform(
                put("/api/v1/administration-deleguee/comptes/{id}/etat", id)
                    .with(administrateur("admin-99001", "99001"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"actif\":false,\"motif\":\"Départ\"}")
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.actif").value(false));
        mvc
            .perform(get("/api/v1/administration-deleguee/roles").with(administrateur("admin-99001", "99001")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").value(org.hamcrest.Matchers.hasItems("ROLE_SBX_ADMIN_INSTITUTION", "ROLE_SBX_TESTEUR", "ROLE_SBX_LECTEUR")))
            .andExpect(jsonPath("$").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem("ROLE_CORRESPONDANT"))));
    }

    @Test
    @DisplayName("Accès : permission exigée, institution exigée dans le jeton, second facteur exigé, corps validé")
    void acces() throws Exception {
        mvc.perform(get("/api/v1/administration-deleguee/comptes").with(utilisateur(SUPER_ADMIN, PERMISSIONS_SUPER_ADMIN))).andExpect(status().isForbidden());
        mvc
            .perform(get("/api/v1/administration-deleguee/comptes").with(jwt().jwt(j -> j.subject("sans-institution").claim("acr", "2")).authorities(new SimpleGrantedAuthority("SBX_HAB_ADMINISTRATION"))))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("IAM_SBX_SANS_INSTITUTION"));
        mvc
            .perform(get("/api/v1/administration-deleguee/roles").with(jwt().jwt(j -> j.subject("sans-institution").claim("acr", "2")).authorities(new SimpleGrantedAuthority("SBX_HAB_ADMINISTRATION"))))
            .andExpect(status().isForbidden());
        mvc
            .perform(
                get("/api/v1/administration-deleguee/comptes")
                    .with(jwt().jwt(j -> j.subject("un-facteur").claim("acr", "1").claim("institution_id", "99001")).authorities(new SimpleGrantedAuthority("SBX_HAB_ADMINISTRATION")))
            )
            .andExpect(status().isForbidden());
        mvc
            .perform(post("/api/v1/administration-deleguee/comptes").with(administrateur("admin-99001", "99001")).contentType(MediaType.APPLICATION_JSON).content("{\"roles\":[]}"))
            .andExpect(status().isBadRequest());
        mvc
            .perform(get("/api/v1/administration-deleguee/comptes/{id}", "inexistant").with(administrateur("admin-99001", "99001")))
            .andExpect(status().isNotFound())
            .andExpect(header().doesNotExist("Location"));
    }

    @org.springframework.beans.factory.annotation.Autowired
    cm.gimac.des.iam.application.port.out.Annuaire annuaire;

    @Test
    @DisplayName("US-EXI-23 : verrouillage après les échecs paramétrés ; déblocage refusé à l'institution et journalisé, fait par l'administrateur du GIMAC")
    void verrouillageEtDeblocage() throws Exception {
        String s = UUID.randomUUID().toString().substring(0, 6);
        String id = creer("99001", "bloque." + s, null);
        for (int i = 0; i < 5; i++) {
            annuaire.verifierMotDePasse("bloque." + s, "faux-" + i);
        }
        mvc.perform(get("/api/v1/comptes/{id}/verrouillage", id).with(utilisateur(SUPER_ADMIN, "IAM_ACCOUNT_READ"))).andExpect(status().isOk())
            .andExpect(jsonPath("$.verrouille").value(true)).andExpect(jsonPath("$.echecs").value(5));
        mvc
            .perform(post("/api/v1/administration-deleguee/comptes/{id}/deblocage", id).with(administrateur("admin-99001", "99001")).contentType(MediaType.APPLICATION_JSON).content("{\"motif\":\"Testeur bloqué\"}"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("IAM_SBX_DEBLOCAGE_RESERVE"));
        assertThat(jdbc.queryForObject("select count(*) from outbox_event where aggregate_id = ? and payload::text like '%TENTATIVE_DEBLOCAGE%'", Integer.class, id)).isEqualTo(1);
        mvc.perform(post("/api/v1/administration-deleguee/comptes/{id}/deblocage", id).with(administrateur("admin-99002", "99002"))).andExpect(status().isNotFound());
        mvc.perform(post("/api/v1/comptes/{id}/deblocage", id).with(utilisateur(SUPER_ADMIN, "IAM_ACCOUNT_READ")).contentType(MediaType.APPLICATION_JSON).content("{\"motif\":\"x\"}"))
            .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/comptes/{id}/deblocage", id).with(utilisateur(SUPER_ADMIN, "IAM_ACCOUNT_SECURITY")).contentType(MediaType.APPLICATION_JSON).content("{\"motif\":\"Appel au support\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.verrouille").value(false)).andExpect(jsonPath("$.echecs").value(0));
        assertThat(jdbc.queryForObject("select count(*) from outbox_event where aggregate_id = ? and payload::text like '%COMPTE_DEBLOQUE%'", Integer.class, id)).isEqualTo(1);
    }
}
