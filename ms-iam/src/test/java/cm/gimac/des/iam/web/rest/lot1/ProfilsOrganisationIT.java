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
import cm.gimac.des.iam.application.usecase.RestaurationPresentationsProfils;
import com.jayway.jsonpath.JsonPath;
import java.util.Locale;
import java.util.Map;
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
 * Profils et organisation configurables de bout en bout (D-86, docs/lot1/profils-organisation.md §2 et §4),
 * sur PostgreSQL réel : libellé et description d'un profil sous FIDO2 (journal CONFIGURATION_PROFIL_MODIFIE,
 * trace dans profil_presentation, réapplication au démarrage), profil privilégié réservé, actions des
 * processus dans la matrice (jeu par défaut du changelog), libellés des profils demandables, membres d'une
 * unité pour le moteur de processus (route interne).
 */
class ProfilsOrganisationIT extends AbstractIamIT {

    static final RequestPostProcessor SERVICE = jwt().jwt(j -> j.subject("service-account-pilotage-service")).authorities(new SimpleGrantedAuthority("INTERNAL_IAM_READ"));

    @Autowired
    RestaurationPresentationsProfils restauration;

    @Autowired
    cm.gimac.des.iam.application.port.out.Annuaire annuaire;

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

    String creer(String identifiant, String profil) throws Exception {
        String corps = """
            {"identifiant":"%s","prenom":"%s","nom":"Mballa","email":"%s@gimac.test","fonction":"Technicien","profils":["%s"],"envoyerInvitation":true}"""
            .formatted(identifiant, identifiant.substring(0, identifiant.indexOf('.')), identifiant, profil);
        String reponse = mvc
            .perform(post("/api/v1/comptes").with(utilisateur(SUPER_ADMIN, PERMISSIONS_SUPER_ADMIN)).contentType(MediaType.APPLICATION_JSON).content(corps))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
        return JsonPath.read(reponse, "$.compte.id");
    }

    @Test
    @DisplayName("§2 Libellé et description : FIDO2 et IAM_PROFILE_EDIT, code inchangé, journal avant/après, liste avec modifieLe et modifiePar, réapplication après réimport")
    void presentation() throws Exception {
        String admin = "admin-presentation-" + suffixe();
        CleDeSecuriteEmulee cle = enroler(admin);
        RequestPostProcessor administration = utilisateur(admin, PERMISSIONS_SUPER_ADMIN);
        String u = "/api/v1/configuration/profils/ROLE_POLE_SYSTEME";
        String corps = "{\"libelle\":\" Pôle systèmes et stockage \",\"description\":\"Exploite les serveurs et le stockage.\",\"motif\":\"Réorganisation\"}";

        mvc
            .perform(get("/api/v1/configuration/profils").with(administration))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.code == 'ROLE_POLE_SYSTEME')].libelle").value(Matchers.contains("Pôle système")));
        mvc.perform(json(put(u).with(administration), corps)).andExpect(status().is(428));
        mvc.perform(json(put(u).with(utilisateur(admin, "IAM_ACCOUNT_READ")), corps).header("X-Reauth-Assertion", assertion(cle, admin))).andExpect(status().isForbidden());
        mvc
            .perform(json(put(u).with(administration), corps).header("X-Reauth-Assertion", assertion(cle, admin)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("ROLE_POLE_SYSTEME"))
            .andExpect(jsonPath("$.libelle").value("Pôle systèmes et stockage"))
            .andExpect(jsonPath("$.description").value("Exploite les serveurs et le stockage."))
            .andExpect(jsonPath("$.privilegie").value(false))
            .andExpect(jsonPath("$.cree").value(false))
            .andExpect(jsonPath("$.modifieLe").isNotEmpty())
            .andExpect(jsonPath("$.modifiePar.id").value(admin))
            .andExpect(jsonPath("$.modifiePar.nomAffiche").value("Agent " + admin));
        mvc
            .perform(get("/api/v1/configuration/profils").with(administration))
            .andExpect(jsonPath("$[?(@.code == 'ROLE_POLE_SYSTEME')].libelle").value(Matchers.contains("Pôle systèmes et stockage")))
            .andExpect(jsonPath("$[?(@.code == 'ROLE_POLE_SYSTEME')].modifiePar.id").value(Matchers.contains(admin)))
            .andExpect(jsonPath("$[?(@.code == 'ROLE_POLE_SYSTEME')].titulaires").exists());
        mvc
            .perform(get("/api/v1/pouvoirs/matrice").with(administration))
            .andExpect(jsonPath("$.profils[?(@.profil == 'ROLE_POLE_SYSTEME')].libelle").value(Matchers.contains("Pôle systèmes et stockage")));
        assertThat(
            jdbc.queryForObject("select details from journal_administration where action = 'CONFIGURATION_PROFIL_MODIFIE' and acteur_id = ? order by horodatage desc limit 1", String.class, admin)
        ).isEqualTo("avant: libelle=Pôle système ; description=, après: libelle=Pôle systèmes et stockage ; description=Exploite les serveurs et le stockage.");
        assertThat(jdbc.queryForObject("select cible_libelle from journal_administration where action = 'CONFIGURATION_PROFIL_MODIFIE' and acteur_id = ?", String.class, admin)).isEqualTo("ROLE_POLE_SYSTEME");
        Map<String, Object> trace = jdbc.queryForMap("select libelle, description, modifie_par_id from profil_presentation where code = 'ROLE_POLE_SYSTEME'");
        assertThat(trace).containsEntry("libelle", "Pôle systèmes et stockage").containsEntry("modifie_par_id", admin);

        // Réimport du domaine : la valeur du dépôt revient, MS-IAM réapplique celle de l'administration.
        annuaire.definirPresentationProfil("ROLE_POLE_SYSTEME", "Pôle système", "Mission du dépôt");
        assertThat(restauration.restaurer()).isEqualTo(1);
        assertThat(annuaire.presentationsProfils().get("ROLE_POLE_SYSTEME").libelle()).isEqualTo("Pôle systèmes et stockage");
        assertThat(restauration.restaurer()).isZero();

        // Refus : motif, libellé, profil inconnu, profil privilégié sans octroi privilégié.
        mvc
            .perform(json(put(u).with(administration), "{\"libelle\":\"Pôle\",\"description\":\"\"}").header("X-Reauth-Assertion", assertion(cle, admin)))
            .andExpect(status().isBadRequest());
        mvc
            .perform(json(put(u).with(administration), "{\"libelle\":\" \",\"motif\":\"m\"}").header("X-Reauth-Assertion", assertion(cle, admin)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("IAM_PROFIL_INVALIDE"));
        mvc
            .perform(json(put("/api/v1/configuration/profils/ROLE_INCONNU").with(administration), "{\"libelle\":\"X\",\"motif\":\"m\"}").header("X-Reauth-Assertion", assertion(cle, admin)))
            .andExpect(status().isNotFound());
        String rssi = "rssi-presentation-" + suffixe();
        CleDeSecuriteEmulee cleRssi = enroler(rssi);
        mvc
            .perform(json(put("/api/v1/configuration/profils/ROLE_RSSI").with(utilisateur(rssi, PERMISSIONS_RSSI)), "{\"libelle\":\"RSSI\",\"motif\":\"m\"}").header("X-Reauth-Assertion", assertion(cleRssi, rssi)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("IAM_PRIVILEGED_PROFILE"));

        // Remise du libellé livré (contexte partagé avec les autres tests).
        mvc
            .perform(json(put(u).with(administration), "{\"libelle\":\"Pôle système\",\"description\":\"\",\"motif\":\"Retour\"}").header("X-Reauth-Assertion", assertion(cle, admin)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.libelle").value("Pôle système"));
    }

    @Test
    @DisplayName("§2 Profil créé avec son libellé ; §4 actions des processus : jeu par défaut des responsables, réglables (contrainte de la base), dans pouvoirs/moi et la route interne")
    void profilCreeEtProcessus() throws Exception {
        String s = suffixe().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "X");
        String admin = "admin-processus-" + s.toLowerCase(Locale.ROOT);
        CleDeSecuriteEmulee cle = enroler(admin);
        RequestPostProcessor administration = utilisateur(admin, PERMISSIONS_SUPER_ADMIN);
        String code = "ROLE_METHODES_" + s;
        mvc
            .perform(
                json(post("/api/v1/configuration/profils").with(administration), "{\"code\":\"%s\",\"libelle\":\"Cellule méthodes\",\"description\":\"Méthodes et qualité\",\"permissions\":[]}".formatted(code))
                    .header("X-Reauth-Assertion", assertion(cle, admin))
            )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.libelle").value("Cellule méthodes"))
            .andExpect(jsonPath("$.modifiePar.id").value(admin));
        mvc
            .perform(get("/api/v1/configuration/profils").with(administration))
            .andExpect(jsonPath("$[?(@.code == '%s')].libelle".formatted(code)).value(Matchers.contains("Cellule méthodes")));

        mvc
            .perform(get("/api/v1/pouvoirs/matrice").with(administration))
            .andExpect(jsonPath("$.profils[?(@.profil == 'ROLE_CHEF_DES')].actions.PROCESSUS_DEFINIR").value(Matchers.contains("UNITES")))
            .andExpect(jsonPath("$.profils[?(@.profil == 'ROLE_RESPONSABLE_SERVICE')].actions.PROCESSUS_LANCER").value(Matchers.contains("UNITES")))
            .andExpect(jsonPath("$.profils[?(@.profil == 'ROLE_DG')].actions.PROCESSUS_LANCER").value(Matchers.contains("UNITES")))
            .andExpect(jsonPath("$.profils[?(@.profil == '%s')].actions.PROCESSUS_DEFINIR".formatted(code)).value(Matchers.contains("AUCUNE")));
        // Réglage : la contrainte de la base admet les nouvelles actions.
        mvc
            .perform(
                json(put("/api/v1/pouvoirs/matrice/{p}", code).with(administration), "{\"actions\":{\"PROCESSUS_DEFINIR\":\"DIRECTS\",\"PROCESSUS_LANCER\":\"EQUIPE\"},\"profilsDemandables\":[\"ROLE_COLLABORATEUR\"]}")
                    .header("X-Reauth-Assertion", assertion(cle, admin))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.libelle").value("Cellule méthodes"))
            .andExpect(jsonPath("$.actions.PROCESSUS_DEFINIR").value("DIRECTS"))
            .andExpect(jsonPath("$.actions.PROCESSUS_LANCER").value("EQUIPE"));
        assertThat(jdbc.queryForObject("select count(*) from pouvoir_profil where profil = ? and action like 'PROCESSUS_%'", Long.class, code)).isEqualTo(2L);

        String titulaire = creer("methodes." + s.toLowerCase(Locale.ROOT), code);
        mvc
            .perform(get("/api/v1/pouvoirs/moi").with(utilisateur(titulaire, "PIL_ACTIVITY_OWN")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.actions.PROCESSUS_DEFINIR").value("DIRECTS"))
            .andExpect(jsonPath("$.profilsDemandables", Matchers.contains("ROLE_COLLABORATEUR")))
            .andExpect(jsonPath("$.libellesProfils.ROLE_COLLABORATEUR").value("Collaborateur"));
        mvc
            .perform(get("/api/v1/interne/pouvoirs/{id}", titulaire).with(SERVICE))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.actions.PROCESSUS_LANCER").value("EQUIPE"))
            .andExpect(jsonPath("$.libellesProfils.ROLE_COLLABORATEUR").value("Collaborateur"));
    }

    @Test
    @DisplayName("Route interne des membres d'une unité : jeton de service, comptes internes actifs, filtre par profil, sous-unités, profils de chacun, 404")
    void membresUnite() throws Exception {
        String s = suffixe();
        RequestPostProcessor superAdmin = utilisateur(SUPER_ADMIN, PERMISSIONS_SUPER_ADMIN);
        String parent = "ATELIER_" + s.toUpperCase(Locale.ROOT).replace('-', '_');
        String enfant = parent + "_B";
        mvc
            .perform(json(post("/api/v1/organisation/unites").with(superAdmin), "{\"code\":\"%s\",\"libelle\":\"Atelier %s\",\"type\":\"CELLULE\",\"parent\":\"PROD_INFO\"}".formatted(parent, s)))
            .andExpect(status().isCreated());
        mvc
            .perform(json(post("/api/v1/organisation/unites").with(superAdmin), "{\"code\":\"%s\",\"libelle\":\"Atelier %s B\",\"type\":\"CELLULE\",\"parent\":\"%s\"}".formatted(enfant, s, parent)))
            .andExpect(status().isCreated());
        String reseau = creer("reseau." + s, "ROLE_POLE_RESEAU");
        String systeme = creer("systeme." + s, "ROLE_POLE_SYSTEME");
        String parti = creer("parti." + s, "ROLE_POLE_RESEAU");
        for (String[] r : new String[][] { { reseau, parent }, { systeme, enfant }, { parti, parent } }) {
            mvc
                .perform(json(put("/api/v1/comptes/{id}/rattachement", r[0]).with(superAdmin), "{\"unite\":\"%s\",\"responsableId\":null}".formatted(r[1])))
                .andExpect(status().isOk());
        }
        mvc
            .perform(json(post("/api/v1/comptes/{id}/desactivation", parti).with(superAdmin), "{\"motif\":\"Départ\"}"))
            .andExpect(status().isOk());

        String u = "/api/v1/interne/organisation/unites/{code}/membres";
        mvc.perform(get(u, parent).with(utilisateur(SUPER_ADMIN, PERMISSIONS_SUPER_ADMIN))).andExpect(status().isForbidden());
        mvc
            .perform(get(u, parent).with(SERVICE))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[*].id", Matchers.containsInAnyOrder(reseau, systeme)))
            .andExpect(jsonPath("$[?(@.id == '%s')].unite".formatted(systeme)).value(Matchers.contains(enfant)))
            .andExpect(jsonPath("$[?(@.id == '%s')].profils[*]".formatted(reseau)).value(Matchers.contains("ROLE_POLE_RESEAU")))
            .andExpect(jsonPath("$[?(@.id == '%s')].actif".formatted(reseau)).value(Matchers.contains(true)))
            .andExpect(jsonPath("$[?(@.id == '%s')].fonction".formatted(reseau)).value(Matchers.contains("Technicien")));
        mvc.perform(get(u, parent).param("sousUnites", "false").with(SERVICE)).andExpect(jsonPath("$[*].id", Matchers.contains(reseau)));
        mvc.perform(get(u, parent).param("profil", "ROLE_POLE_SYSTEME").with(SERVICE)).andExpect(jsonPath("$[*].id", Matchers.contains(systeme)));
        mvc.perform(get(u, "INCONNUE_" + s.toUpperCase(Locale.ROOT)).with(SERVICE)).andExpect(status().isNotFound());
    }
}
