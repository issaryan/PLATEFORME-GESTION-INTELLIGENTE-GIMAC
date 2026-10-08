package cm.gimac.des.iam.web.rest.lot1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cm.gimac.des.iam.AbstractIamIT;
import cm.gimac.des.iam.CleDeSecuriteEmulee;
import com.jayway.jsonpath.JsonPath;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Administration des référentiels métier, tous domaines (D-92, docs/lot1/referentiels-metier-admin.md §2 bis),
 * côté MS-IAM, sur PostgreSQL réel : portée TOUTES de la matrice (admise pour PROCESSUS_DEFINIR et
 * PROCESSUS_LANCER, 422 ORG_PORTEE_NON_ADMISE ailleurs, contrainte de la base), exposition (portées admises par
 * action, pouvoirs du compte, route interne), périmètre TOUTES, cumul de profils ; permission ordinaire
 * REF_METIER_ADMIN reçue par un profil créé et ajoutée puis retirée d'un profil livré, sans droit d'octroi
 * privilégié.
 */
class ReferentielsMetierAdminIT extends AbstractIamIT {

    static final RequestPostProcessor SERVICE = jwt().jwt(j -> j.subject("service-account-pilotage-service")).authorities(new SimpleGrantedAuthority("INTERNAL_IAM_READ"));
    /** Administration des profils sans IAM_ROLE_PRIVILEGED_GRANT. */
    static final String[] PERMISSIONS_ADMIN_ORDINAIRE = { "IAM_ACCOUNT_READ", "IAM_PROFILE_EDIT" };

    String suffixe() {
        return UUID.randomUUID().toString().substring(0, 6).toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "X");
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

    String creer(String identifiant, String... profils) throws Exception {
        String liste = java.util.Arrays.stream(profils).map(p -> "\"" + p + "\"").collect(Collectors.joining(","));
        String corps = """
            {"identifiant":"%s","prenom":"%s","nom":"Mballa","email":"%s@gimac.test","fonction":"Ingénieur","profils":[%s],"envoyerInvitation":true}"""
            .formatted(identifiant, identifiant.substring(0, identifiant.indexOf('.')), identifiant, liste);
        String reponse = mvc
            .perform(post("/api/v1/comptes").with(utilisateur(SUPER_ADMIN, PERMISSIONS_SUPER_ADMIN)).contentType(MediaType.APPLICATION_JSON).content(corps))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
        return JsonPath.read(reponse, "$.compte.id");
    }

    @Test
    @DisplayName("§2 bis Matrice : TOUTES exposée et admise pour les processus, refusée ailleurs (422, base inchangée, contrainte) ; pouvoirs, cumul, périmètre de toutes les unités")
    void porteeToutes() throws Exception {
        String s = suffixe();
        String admin = "admin-toutes-" + s.toLowerCase(Locale.ROOT);
        CleDeSecuriteEmulee cle = enroler(admin);
        RequestPostProcessor administration = utilisateur(admin, PERMISSIONS_SUPER_ADMIN);
        String code = "ROLE_REFERENTIELS_" + s;
        mvc
            .perform(json(post("/api/v1/configuration/profils").with(administration), "{\"code\":\"%s\",\"libelle\":\"Référentiels\",\"description\":\"Processus de toutes les unités\",\"permissions\":[]}".formatted(code)).header("X-Reauth-Assertion", assertion(cle, admin)))
            .andExpect(status().isCreated());

        mvc
            .perform(get("/api/v1/pouvoirs/matrice").with(administration))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.portees", Matchers.contains("AUCUNE", "DIRECTS", "EQUIPE", "UNITES", "TOUTES")))
            .andExpect(jsonPath("$.porteesAdmises.PROCESSUS_DEFINIR", Matchers.contains("AUCUNE", "DIRECTS", "EQUIPE", "UNITES", "TOUTES")))
            .andExpect(jsonPath("$.porteesAdmises.PROCESSUS_LANCER", Matchers.hasItem("TOUTES")))
            .andExpect(jsonPath("$.porteesAdmises.TACHE_CONFIER", Matchers.contains("AUCUNE", "DIRECTS", "EQUIPE", "UNITES")))
            .andExpect(jsonPath("$.porteesAdmises.RATTACHEMENT_MODIFIER", Matchers.not(Matchers.hasItem("TOUTES"))));

        String u = "/api/v1/pouvoirs/matrice/{p}";
        mvc
            .perform(json(put(u, code).with(administration), "{\"actions\":{\"PROCESSUS_DEFINIR\":\"TOUTES\",\"TACHE_CONFIER\":\"TOUTES\"},\"profilsDemandables\":[]}").header("X-Reauth-Assertion", assertion(cle, admin)))
            .andExpect(status().is(422))
            .andExpect(jsonPath("$.code").value("ORG_PORTEE_NON_ADMISE"))
            .andExpect(jsonPath("$.details.actions", Matchers.contains("TACHE_CONFIER")));
        assertThat(jdbc.queryForObject("select count(*) from pouvoir_profil where profil = ?", Long.class, code)).isZero();
        mvc
            .perform(
                json(put(u, code).with(administration), "{\"actions\":{\"PROCESSUS_DEFINIR\":\"TOUTES\",\"PROCESSUS_LANCER\":\"TOUTES\"},\"profilsDemandables\":[],\"motif\":\"D-92\"}").header(
                    "X-Reauth-Assertion",
                    assertion(cle, admin)
                )
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.actions.PROCESSUS_DEFINIR").value("TOUTES"))
            .andExpect(jsonPath("$.actions.PROCESSUS_LANCER").value("TOUTES"))
            .andExpect(jsonPath("$.actions.TACHE_CONFIER").value("AUCUNE"));
        assertThat(jdbc.queryForList("select portee from pouvoir_profil where profil = ? order by action", String.class, code)).containsExactly("TOUTES", "TOUTES");
        assertThat(
            jdbc.queryForObject("select details from journal_administration where action = 'MATRICE_POUVOIRS_MODIFIEE' and acteur_id = ? order by horodatage desc limit 1", String.class, admin)
        ).isEqualTo("avant: AUCUNE demandables=, après: PROCESSUS_DEFINIR=TOUTES PROCESSUS_LANCER=TOUTES demandables=");
        // La base refuse TOUTES hors des processus, même sans passer par le service.
        assertThatThrownBy(() -> jdbc.update("insert into pouvoir_profil (profil, action, portee) values (?, 'TACHE_CONFIER', 'TOUTES')", code)).isInstanceOf(DataIntegrityViolationException.class);

        // Pouvoirs du titulaire et cumul avec un profil responsable (UNITES) : TOUTES l'emporte.
        String titulaire = creer("referentiels." + s.toLowerCase(Locale.ROOT), code);
        String cumul = creer("cumul." + s.toLowerCase(Locale.ROOT), code, "ROLE_CHEF_DES");
        mvc
            .perform(get("/api/v1/pouvoirs/moi").with(utilisateur(titulaire, "PIL_ACTIVITY_OWN")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.actions.PROCESSUS_DEFINIR").value("TOUTES"))
            .andExpect(jsonPath("$.actions.PROCESSUS_LANCER").value("TOUTES"))
            .andExpect(jsonPath("$.actions.TACHE_CONFIER").value("AUCUNE"))
            .andExpect(jsonPath("$.subordonnes.length()").value(0));
        mvc
            .perform(get("/api/v1/interne/pouvoirs/{id}", cumul).with(SERVICE))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.actions.PROCESSUS_DEFINIR").value("TOUTES"))
            .andExpect(jsonPath("$.actions.PROCESSUS_LANCER").value("TOUTES"))
            .andExpect(jsonPath("$.actions.TACHE_CONFIER").value("UNITES"));

        // Périmètre TOUTES : unités actives et inactives, comptes internes actifs, jamais soi-même.
        String cellule = "CELLULE_FERMEE_" + s;
        RequestPostProcessor superAdmin = utilisateur(SUPER_ADMIN, PERMISSIONS_SUPER_ADMIN);
        mvc
            .perform(json(post("/api/v1/organisation/unites").with(superAdmin), "{\"code\":\"%s\",\"libelle\":\"Cellule fermée\",\"type\":\"CELLULE\",\"parent\":\"PROD_INFO\"}".formatted(cellule)))
            .andExpect(status().isCreated());
        mvc
            .perform(json(put("/api/v1/organisation/unites/{code}", cellule).with(superAdmin), "{\"libelle\":\"Cellule fermée\",\"type\":\"CELLULE\",\"parent\":\"PROD_INFO\",\"active\":false}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.active").value(false));
        String perimetre = mvc
            .perform(get("/api/v1/interne/organisation/{id}/perimetre", titulaire).param("portee", "TOUTES").with(SERVICE))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.portee").value("TOUTES"))
            .andExpect(jsonPath("$.unites", Matchers.hasItems("DG", "PROD_INFO", cellule)))
            .andExpect(jsonPath("$.comptes[*].id", Matchers.hasItem(cumul)))
            .andExpect(jsonPath("$.comptes[*].id", Matchers.not(Matchers.hasItem(titulaire))))
            .andReturn()
            .getResponse()
            .getContentAsString();
        List<Boolean> actifs = JsonPath.read(perimetre, "$.comptes[*].actif");
        assertThat(actifs).isNotEmpty().containsOnly(true).hasSizeLessThanOrEqualTo(500);
        assertThat(jdbc.queryForObject("select count(*) from unite_organisationnelle", Long.class)).isEqualTo(((List<?>) JsonPath.read(perimetre, "$.unites")).size());
    }

    @Test
    @DisplayName("§2 bis Composition : REF_METIER_ADMIN connue, non privilégiée ; profil créé qui la reçoit dès sa création ; ajout puis retrait sur un profil livré, sans droit d'octroi privilégié")
    void permissionOrdinaire() throws Exception {
        String s = suffixe();
        String admin = "admin-refmetier-" + s.toLowerCase(Locale.ROOT);
        CleDeSecuriteEmulee cle = enroler(admin);
        RequestPostProcessor administration = utilisateur(admin, PERMISSIONS_ADMIN_ORDINAIRE);

        mvc.perform(get("/api/v1/configuration/permissions").with(administration)).andExpect(status().isOk()).andExpect(jsonPath("$", Matchers.hasItem("REF_METIER_ADMIN")));

        String code = "ROLE_REF_METIER_" + s;
        mvc
            .perform(
                json(post("/api/v1/configuration/profils").with(administration), "{\"code\":\"%s\",\"libelle\":\"Référentiels métier\",\"description\":\"Administration des référentiels métier\",\"permissions\":[\"REF_METIER_ADMIN\"]}".formatted(code)).header(
                    "X-Reauth-Assertion",
                    assertion(cle, admin)
                )
            )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.permissions", Matchers.hasItems("REF_METIER_ADMIN", "PIL_ACTIVITY_OWN")))
            .andExpect(jsonPath("$.privilegie").value(false))
            .andExpect(jsonPath("$.cree").value(true));
        assertThat(jdbc.queryForObject("select privilegie from profil_cree where code = ?", Boolean.class, code)).isFalse();

        // Profil livré non privilégié : composition actuelle, ajout, puis retour à l'état initial.
        String profils = mvc.perform(get("/api/v1/configuration/profils").with(administration)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        List<String> initiales = JsonPath.read(profils, "$[?(@.code == 'ROLE_POLE_RESEAU')].permissions[*]");
        assertThat(initiales).isNotEmpty().doesNotContain("REF_METIER_ADMIN");
        String liste = initiales.stream().map(p -> "\"" + p + "\"").collect(Collectors.joining(","));
        String u = "/api/v1/configuration/profils/ROLE_POLE_RESEAU/permissions";
        mvc
            .perform(json(put(u).with(administration), "{\"permissions\":[%s,\"REF_METIER_ADMIN\"],\"motif\":\"Référentiels confiés au pôle\"}".formatted(liste)).header("X-Reauth-Assertion", assertion(cle, admin)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.permissions", Matchers.hasItem("REF_METIER_ADMIN")))
            .andExpect(jsonPath("$.privilegie").value(false))
            .andExpect(jsonPath("$.cree").value(false));
        assertThat(
            jdbc.queryForObject("select count(*) from journal_administration where action = 'CONFIGURATION_PROFIL_ROLE_POLE_RESEAU' and acteur_id = ?", Long.class, admin)
        ).isEqualTo(1L);
        mvc
            .perform(json(put(u).with(administration), "{\"permissions\":[%s],\"motif\":\"Retour à la composition livrée\"}".formatted(liste)).header("X-Reauth-Assertion", assertion(cle, admin)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.permissions", Matchers.not(Matchers.hasItem("REF_METIER_ADMIN"))))
            .andExpect(jsonPath("$.permissions.length()").value(initiales.size()));
    }
}
