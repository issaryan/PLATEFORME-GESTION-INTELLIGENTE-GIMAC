package cm.gimac.des.iam.web.rest.lot1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cm.gimac.des.iam.AbstractIamIT;
import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Organisation du GIMAC de bout en bout (D-76, docs/lot1/mon-activite.md §1), sur PostgreSQL réel :
 * jeu initial du catalogue, création et désactivation d'une unité, rattachement d'un compte avec les
 * refus du contrat, journal COMPTE_RATTACHEMENT_MODIFIE, lecture « Mon activité », routes internes
 * (rattachement, équipe récursive) et annuaire des collaborateurs complété.
 */
class OrganisationIT extends AbstractIamIT {

    static final RequestPostProcessor SERVICE = jwt().jwt(j -> j.subject("service-account-pilotage-service")).authorities(new SimpleGrantedAuthority("INTERNAL_IAM_READ"));

    static RequestPostProcessor superAdmin() {
        return utilisateur(SUPER_ADMIN, PERMISSIONS_SUPER_ADMIN);
    }

    String creer(String identifiant, String prenom, String institution) throws Exception {
        String corps = """
            {"identifiant":"%s","prenom":"%s","nom":"Mballa","email":"%s@gimac.test","telephone":"+237600000009","fonction":"Fonction de %s",%s"profils":["%s"],"envoyerInvitation":true}"""
            .formatted(identifiant, prenom, identifiant, prenom, institution == null ? "" : "\"institutionId\":\"" + institution + "\",", institution == null ? "ROLE_CHARGE_DDRM" : "ROLE_CORRESPONDANT");
        String reponse = mvc
            .perform(post("/api/v1/comptes").with(superAdmin()).contentType(MediaType.APPLICATION_JSON).content(corps))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.compte.unite").value(Matchers.nullValue()))
            .andExpect(jsonPath("$.compte.responsableId").value(Matchers.nullValue()))
            .andReturn()
            .getResponse()
            .getContentAsString();
        return JsonPath.read(reponse, "$.compte.id");
    }

    org.springframework.test.web.servlet.ResultActions rattacher(String id, String unite, String responsable) throws Exception {
        String corps = "{\"unite\":%s,\"responsableId\":%s}".formatted(unite == null ? "null" : "\"" + unite + "\"", responsable == null ? "null" : "\"" + responsable + "\"");
        return mvc.perform(put("/api/v1/comptes/{id}/rattachement", id).with(superAdmin()).contentType(MediaType.APPLICATION_JSON).content(corps));
    }

    @Test
    @DisplayName("Catalogue : jeu initial, création, refus (code, direction avec parent, parent inconnu, cycle), désactivation")
    void catalogue() throws Exception {
        mvc
            .perform(get("/api/v1/organisation/unites").with(utilisateur("agent", "PIL_ACTIVITY_OWN")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[*].code", Matchers.hasItems("DG", "DES", "SNA", "PROD_MONETIQUE", "PROD_INFO", "POLE_RESEAU", "POLE_SYSTEME", "DDRM", "SSI", "CONFORMITE")))
            .andExpect(jsonPath("$[?(@.code == 'POLE_RESEAU')].libelle").value(Matchers.contains("Pôle réseau")))
            .andExpect(jsonPath("$[?(@.code == 'POLE_RESEAU')].type").value(Matchers.contains("POLE")))
            .andExpect(jsonPath("$[?(@.code == 'POLE_RESEAU')].parent").value(Matchers.contains("PROD_INFO")))
            .andExpect(jsonPath("$[?(@.code == 'DG')].parent").value(Matchers.contains(Matchers.nullValue())));

        String code = "CELLULE_" + UUID.randomUUID().toString().substring(0, 6).toUpperCase(java.util.Locale.ROOT).replace('-', '_');
        String u = "/api/v1/organisation/unites";
        mvc
            .perform(post(u).with(superAdmin()).contentType(MediaType.APPLICATION_JSON).content("{\"code\":\"%s\",\"libelle\":\"Cellule qualité\",\"type\":\"CELLULE\",\"parent\":\"SNA\"}".formatted(code)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.code").value(code))
            .andExpect(jsonPath("$.active").value(true));
        mvc
            .perform(post(u).with(superAdmin()).contentType(MediaType.APPLICATION_JSON).content("{\"code\":\"%s\",\"libelle\":\"x\",\"type\":\"CELLULE\"}".formatted(code)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("ORG_UNITE_EXISTANTE"));
        mvc
            .perform(post(u).with(superAdmin()).contentType(MediaType.APPLICATION_JSON).content("{\"code\":\"minuscule\",\"libelle\":\"x\",\"type\":\"CELLULE\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("ORG_UNITE_INVALIDE"));
        mvc
            .perform(post(u).with(superAdmin()).contentType(MediaType.APPLICATION_JSON).content("{\"code\":\"DIR_BIS\",\"libelle\":\"x\",\"type\":\"DIRECTION\",\"parent\":\"DG\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("ORG_UNITE_INVALIDE"));
        mvc
            .perform(post(u).with(superAdmin()).contentType(MediaType.APPLICATION_JSON).content("{\"code\":\"POLE_ORPHELIN\",\"libelle\":\"x\",\"type\":\"POLE\",\"parent\":\"INCONNUE\"}"))
            .andExpect(status().is(422))
            .andExpect(jsonPath("$.code").value("ORG_UNITE_INCONNUE"));
        mvc
            .perform(put(u + "/DES").with(superAdmin()).contentType(MediaType.APPLICATION_JSON).content("{\"libelle\":\"x\",\"type\":\"DEPARTEMENT\",\"parent\":\"POLE_RESEAU\",\"active\":true}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("ORG_CYCLE"));
        mvc
            .perform(put(u + "/SNA").with(superAdmin()).contentType(MediaType.APPLICATION_JSON).content("{\"libelle\":\"x\",\"type\":\"SERVICE\",\"parent\":\"DES\",\"active\":false}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("ORG_UNITE_SOUS_UNITES_ACTIVES"));
        mvc
            .perform(put(u + "/" + code).with(superAdmin()).contentType(MediaType.APPLICATION_JSON).content("{\"libelle\":\"Cellule qualité\",\"type\":\"CELLULE\",\"parent\":\"SNA\",\"active\":false}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.active").value(false));
        mvc.perform(get(u + "?actives=true").with(utilisateur(RSSI, PERMISSIONS_RSSI))).andExpect(jsonPath("$[*].code", Matchers.not(Matchers.hasItem(code))));
        mvc.perform(post(u).with(utilisateur(RSSI, "IAM_ACCOUNT_READ")).contentType(MediaType.APPLICATION_JSON).content("{\"code\":\"X_Y\",\"libelle\":\"x\",\"type\":\"POLE\"}")).andExpect(status().isForbidden());
        assertThat(jdbc.queryForObject("select count(*) from journal_administration where action = 'CONFIGURATION_UNITE_ORGANISATIONNELLE' and details like ?", Long.class, "%code=" + code + "%")).isEqualTo(2L);
    }

    @Test
    @DisplayName("Rattachement : fiche complétée, refus du contrat, journal avant/après, Mon compte, organisation/moi, routes internes, équipe")
    void rattachement() throws Exception {
        String s = UUID.randomUUID().toString().substring(0, 6);
        String dg = creer("dg." + s, "Directeur", null);
        String chef = creer("chef." + s, "Chef", null);
        String agent = creer("agent." + s, "Agent", null);
        String corr = creer("corr." + s, "Corr", "INST-0042");

        rattacher(chef, "DES", dg).andExpect(status().isOk()).andExpect(jsonPath("$.unite").value("DES")).andExpect(jsonPath("$.responsableId").value(dg));
        rattacher(agent, "POLE_RESEAU", chef)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(agent))
            .andExpect(jsonPath("$.unite").value("POLE_RESEAU"))
            .andExpect(jsonPath("$.responsableId").value(chef))
            .andExpect(jsonPath("$.identifiant").value("agent." + s));

        rattacher(corr, "DES", null).andExpect(status().is(422)).andExpect(jsonPath("$.code").value("ORG_COMPTE_EXTERNE"));
        rattacher(agent, "INCONNUE", chef).andExpect(status().is(422)).andExpect(jsonPath("$.code").value("ORG_UNITE_INCONNUE"));
        rattacher(agent, "POLE_RESEAU", corr).andExpect(status().is(422)).andExpect(jsonPath("$.code").value("ORG_RESPONSABLE_INVALIDE"));
        rattacher(agent, "POLE_RESEAU", agent).andExpect(status().is(422)).andExpect(jsonPath("$.code").value("ORG_RESPONSABLE_INVALIDE"));
        rattacher(dg, "DG", agent).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("ORG_CYCLE"));
        mvc
            .perform(put("/api/v1/comptes/{id}/rattachement", agent).with(utilisateur(RSSI, "IAM_ACCOUNT_READ")).contentType(MediaType.APPLICATION_JSON).content("{\"unite\":\"DES\"}"))
            .andExpect(status().isForbidden());

        assertThat(
            jdbc.queryForObject(
                "select details from journal_administration where action = 'COMPTE_RATTACHEMENT_MODIFIE' and cible_id = ?",
                String.class,
                agent
            )
        ).isEqualTo("avant: unite=null responsable=null, après: unite=POLE_RESEAU responsable=" + chef);

        mvc.perform(get("/api/v1/comptes/{id}", agent).with(superAdmin())).andExpect(jsonPath("$.unite").value("POLE_RESEAU")).andExpect(jsonPath("$.responsableId").value(chef));
        mvc.perform(get("/api/v1/moi").with(utilisateur(agent))).andExpect(status().isOk()).andExpect(jsonPath("$.compte.unite").value("POLE_RESEAU")).andExpect(jsonPath("$.compte.responsableId").value(chef));

        mvc
            .perform(get("/api/v1/organisation/moi").with(utilisateur(agent, "PIL_ACTIVITY_OWN")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(agent))
            .andExpect(jsonPath("$.nomAffiche").value("Agent Mballa"))
            .andExpect(jsonPath("$.unite.code").value("POLE_RESEAU"))
            .andExpect(jsonPath("$.chemin[*].code", Matchers.contains("DG", "DES", "PROD_INFO", "POLE_RESEAU")))
            .andExpect(jsonPath("$.responsable.id").value(chef))
            .andExpect(jsonPath("$.responsable.nomAffiche").value("Chef Mballa"))
            .andExpect(jsonPath("$.responsable.fonction").value("Fonction de Chef"))
            .andExpect(jsonPath("$.chaine", Matchers.contains(chef, dg)))
            .andExpect(jsonPath("$.equipeDirecte").value(0));
        mvc.perform(get("/api/v1/organisation/moi").with(utilisateur(agent))).andExpect(status().isForbidden());

        mvc.perform(get("/api/v1/interne/organisation/{id}", chef).with(SERVICE)).andExpect(status().isOk()).andExpect(jsonPath("$.equipeDirecte").value(1));
        mvc
            .perform(get("/api/v1/interne/organisation/{id}/equipe", dg).with(SERVICE))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].id").value(chef))
            .andExpect(jsonPath("$[0].niveau").value(1))
            .andExpect(jsonPath("$[0].unite").value("DES"))
            .andExpect(jsonPath("$[0].responsableId").value(dg))
            .andExpect(jsonPath("$[1].id").value(agent))
            .andExpect(jsonPath("$[1].niveau").value(2))
            .andExpect(jsonPath("$[1].fonction").value("Fonction de Agent"))
            .andExpect(jsonPath("$[1].actif").value(true));
        mvc.perform(get("/api/v1/interne/organisation/{id}/equipe", dg).with(utilisateur(dg, "PIL_TEAM_READ", "PIL_ACTIVITY_OWN"))).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/interne/organisation/{id}", "inconnu").with(SERVICE)).andExpect(status().isNotFound());

        mvc
            .perform(get("/api/v1/interne/collaborateurs/{id}", agent).with(SERVICE))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.unite").value("POLE_RESEAU"))
            .andExpect(jsonPath("$.responsableId").value(chef))
            .andExpect(jsonPath("$.fonction").value("Fonction de Agent"));

        // Retrait du rattachement.
        rattacher(agent, null, null).andExpect(status().isOk()).andExpect(jsonPath("$.unite").value(Matchers.nullValue())).andExpect(jsonPath("$.responsableId").value(Matchers.nullValue()));
        mvc.perform(get("/api/v1/interne/organisation/{id}/equipe", dg).with(SERVICE)).andExpect(jsonPath("$.length()").value(1));
    }
}
