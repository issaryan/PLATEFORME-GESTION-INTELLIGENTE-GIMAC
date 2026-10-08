package cm.gimac.des.iam.web.rest.lot1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cm.gimac.des.iam.AbstractIamIT;
import cm.gimac.des.iam.CleDeSecuriteEmulee;
import cm.gimac.des.iam.application.port.out.Annuaire;
import cm.gimac.des.iam.application.usecase.InitialisationFonctionnalites;
import cm.gimac.des.iam.application.usecase.InitialisationProfilsExternes;
import com.jayway.jsonpath.JsonPath;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Écrans pilotés par la configuration (D-94, docs/lot1/ecrans-par-configuration.md), de bout en bout sur PostgreSQL
 * réel : catalogue des fonctionnalités chargé au démarrage (Liquibase, fichier des valeurs de départ), modification
 * par le RSSI sous FIDO2 (journal CONFIGURATION_FONCTIONNALITE_MODIFIEE, conservée au rechargement), attribut
 * externe d'un profil créé (PUT /configuration/profils/{code}, rattachement à une institution, trace dans
 * profil_presentation), valeur de départ gimac_externe posée sur ROLE_CORRESPONDANT.
 */
class EcransParConfigurationIT extends AbstractIamIT {

    static final String U = "/api/v1/configuration/fonctionnalites";

    @Autowired
    InitialisationFonctionnalites fonctionnalites;

    @Autowired
    InitialisationProfilsExternes externes;

    @Autowired
    Annuaire annuaire;

    @Autowired
    org.springframework.transaction.PlatformTransactionManager transactions;

    String suffixe() {
        return UUID.randomUUID().toString().substring(0, 6).toLowerCase(Locale.ROOT);
    }

    CleDeSecuriteEmulee enroler(String sujet) throws Exception {
        CleDeSecuriteEmulee cle = new CleDeSecuriteEmulee();
        String options = mvc.perform(post("/api/v1/moi/cles-securite/defi").with(utilisateur(sujet))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        mvc
            .perform(post("/api/v1/moi/cles-securite").with(utilisateur(sujet)).contentType(MediaType.APPLICATION_JSON).content(cle.enrolement(options, "Clé")))
            .andExpect(status().isOk());
        return cle;
    }

    String assertion(CleDeSecuriteEmulee cle, String sujet) throws Exception {
        String defi = mvc
            .perform(post("/api/v1/reauthentification/defi").with(utilisateur(sujet)).contentType(MediaType.APPLICATION_JSON).content("{\"purpose\":\"ADMIN_SENSITIVE\"}"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
        return cle.assertion(defi);
    }

    static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder b, String corps) {
        return b.contentType(MediaType.APPLICATION_JSON).content(corps);
    }

    @Test
    @DisplayName("§2 Catalogue servi : valeurs de départ chargées ; le RSSI renomme, regroupe, réordonne, masque et change la permission ; conservé au rechargement")
    void catalogueDesFonctionnalites() throws Exception {
        String rssi = "rssi-fonctionnalites-" + suffixe();
        CleDeSecuriteEmulee cle = enroler(rssi);
        RequestPostProcessor administration = utilisateur(rssi, PERMISSIONS_RSSI);
        // Annuaire de substitution : permissions des profils de démonstration (HOM_DASHBOARD_READ, PIL_TEAM_READ…).
        // Tout compte interne lit le catalogue, même sans permission.
        String des = mvc
            .perform(get(U).param("application", "des").with(utilisateur("agent-" + suffixe())))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
        List<String> applications = JsonPath.read(des, "$[*].application");
        assertThat(applications).isNotEmpty().containsOnly("des");
        List<List<String>> liaisons = JsonPath.read(des, "$[?(@.cle == 'liaisons')].permissions.unDe");
        assertThat(liaisons).singleElement().asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.LIST).containsExactly("HOM_LIAISON_LIST");
        List<List<String>> matrice = JsonPath.read(des, "$[?(@.cle == 'adminMatrice')].permissions.tous");
        assertThat(matrice).singleElement().asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.LIST).containsExactly("IAM_ACCOUNT_READ", "IAM_PROFILE_EDIT");
        List<String> ref = JsonPath.read(des, "$[?(@.cle == 'referentielsMetierAdmin')].droits[*].permission");
        assertThat(ref).containsExactly("REF_METIER_ADMIN");
        mvc
            .perform(get(U).with(jwt().jwt(j -> j.subject("corr").claim("acr", "2").claim("institution_id", "INST-1")).authorities(new SimpleGrantedAuthority("HOM_PORTAL_READ"))))
            .andExpect(status().isForbidden());

        String corps = """
            {"application":"des","module":"homologation","libelle":"Projets d'homologation","groupe":"pilotage","ordre":5,"icone":"route","visible":false,
             "permissions":{"tous":["HOM_DASHBOARD_READ"],"unDe":[]},
             "widgets":[{"cle":"cockpit","permissions":{"tous":["HOM_DASHBOARD_READ"],"unDe":["PIL_TEAM_READ"]}}],
             "droits":[{"cle":"HOM_DASHBOARD_READ","nature":"CONSULTER","permission":"HOM_DASHBOARD_READ","libelle":"droits.permission.HOM_DASHBOARD_READ"}],
             "motif":"Revue de la navigation"}""";
        mvc.perform(json(put(U + "/liaisons").with(administration), corps)).andExpect(status().is(428));
        mvc
            .perform(json(put(U + "/liaisons").with(administration), corps.replace("HOM_DASHBOARD_READ\"],\"unDe\":[]", "PERM_INEXISTANTE\"],\"unDe\":[]")).header("X-Reauth-Assertion", assertion(cle, rssi)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("IAM_PERMISSION_UNKNOWN"));
        mvc
            .perform(json(put(U + "/liaisons").with(administration), corps.replace("\"application\":\"des\"", "\"application\":\"console\"")).header("X-Reauth-Assertion", assertion(cle, rssi)))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("IAM_FONCTIONNALITE_APPLICATION_IMMUABLE"));
        mvc
            .perform(json(put(U + "/liaisons").with(administration), corps).header("X-Reauth-Assertion", assertion(cle, rssi)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.libelle").value("Projets d'homologation"))
            .andExpect(jsonPath("$.groupe").value("pilotage"))
            .andExpect(jsonPath("$.ordre").value(5))
            .andExpect(jsonPath("$.visible").value(false))
            .andExpect(jsonPath("$.modifiePar.id").value(rssi));
        mvc
            .perform(get(U).param("application", "des").with(utilisateur("agent-" + suffixe())))
            .andExpect(jsonPath("$[0].cle").value("liaisons"))
            .andExpect(jsonPath("$[0].permissions.tous[0]").value("HOM_DASHBOARD_READ"));
        assertThat(jdbc.queryForObject("select details from journal_administration where action = 'CONFIGURATION_FONCTIONNALITE_MODIFIEE' and acteur_id = ? order by horodatage desc limit 1", String.class, rssi))
            .startsWith("avant: libelle=nav.liaisons ; groupe=homologation ; ordre=")
            .contains("après: libelle=Projets d'homologation ; groupe=pilotage ; ordre=5 ; visible=false ; permissions=tous[HOM_DASHBOARD_READ] unDe[]");
        assertThat(jdbc.queryForObject("select count(*) from outbox_event where aggregate_id = 'FONCTIONNALITE_MODIFIEE'", Integer.class)).isPositive();

        // Nouveau déploiement : le registre livré ne remplace pas la modification de la plateforme.
        assertThat(fonctionnalites.charger().conservees()).isGreaterThanOrEqualTo(1);
        assertThat(jdbc.queryForObject("select definition from fonctionnalite where cle = 'liaisons'", String.class)).contains("Projets d'homologation");
        // Retour à la valeur de départ (pour les autres tests) : une ligne sans trace suit le registre.
        new org.springframework.transaction.support.TransactionTemplate(transactions).executeWithoutResult(t ->
            jdbc.update("update fonctionnalite set modifie_le = null, modifie_par_id = null, modifie_par_nom = null where cle = 'liaisons'")
        );
        assertThat(fonctionnalites.charger().remplacees()).isEqualTo(1);
        assertThat(jdbc.queryForObject("select definition from fonctionnalite where cle = 'liaisons'", String.class)).contains("nav.liaisons");
    }

    @Test
    @DisplayName("§3 Attribut externe : valeur de départ sur ROLE_CORRESPONDANT ; un profil créé marqué externe se rattache à une institution ; trace gardée")
    void profilExterne() throws Exception {
        assertThat(annuaire.attributsExternes()).containsEntry("ROLE_CORRESPONDANT", true);
        assertThat(externes.initialiser()).isZero();
        String admin = "admin-externe-" + suffixe();
        CleDeSecuriteEmulee cle = enroler(admin);
        RequestPostProcessor administration = utilisateur(admin, PERMISSIONS_SUPER_ADMIN);
        String code = "ROLE_PARTENAIRE_" + suffixe().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "X");
        mvc
            .perform(
                json(post("/api/v1/configuration/profils").with(administration), "{\"code\":\"" + code + "\",\"libelle\":\"Partenaire\",\"description\":\"Compte d'un partenaire\",\"permissions\":[]}")
                    .header("X-Reauth-Assertion", assertion(cle, admin))
            )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.externe").value(false));
        String compte = "{\"identifiant\":\"partenaire.%s\",\"prenom\":\"P\",\"nom\":\"Mballa\",\"email\":\"partenaire.%s@banque.test\",\"institutionId\":\"INST-7\",\"profils\":[\"%s\"],\"envoyerInvitation\":true}";
        String s = suffixe();
        mvc
            .perform(json(post("/api/v1/comptes").with(administration), compte.formatted(s, s, code)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("IAM_INSTITUTION_INTERNAL_ACCOUNT"));
        mvc
            .perform(
                json(put("/api/v1/configuration/profils/" + code).with(administration), "{\"libelle\":\"Partenaire\",\"description\":\"Compte d'un partenaire\",\"externe\":true,\"motif\":\"Ouverture\"}")
                    .header("X-Reauth-Assertion", assertion(cle, admin))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.externe").value(true));
        assertThat(jdbc.queryForObject("select externe from profil_presentation where code = ?", Boolean.class, code)).isTrue();
        mvc.perform(json(post("/api/v1/comptes").with(administration), compte.formatted(s, s, code))).andExpect(status().isCreated());
        mvc
            .perform(get("/api/v1/configuration/profils").with(administration))
            .andExpect(jsonPath("$[?(@.code == '" + code + "')].externe").value(Matchers.contains(true)))
            .andExpect(jsonPath("$[?(@.code == 'ROLE_CORRESPONDANT')].externe").value(Matchers.contains(true)));
        // Le profil a désormais un titulaire : sa qualité externe ne change plus.
        mvc
            .perform(
                json(put("/api/v1/configuration/profils/" + code).with(administration), "{\"libelle\":\"Partenaire\",\"description\":\"\",\"externe\":false,\"motif\":\"Retour\"}")
                    .header("X-Reauth-Assertion", assertion(cle, admin))
            )
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("IAM_PROFIL_EXTERNE_TITULAIRES"));
        // Réimport du domaine qui perd l'attribut : réappliqué au démarrage depuis la trace de MS-IAM.
        annuaire.definirExterne(code, false);
        assertThat(externes.initialiser()).isEqualTo(1);
        assertThat(annuaire.attributsExternes()).containsEntry(code, true);
    }
}
