package cm.gimac.des.iam.web.rest.lot1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Pouvoirs des chefs sur leurs subordonnés de bout en bout (D-81, docs/lot1/pouvoirs-hierarchiques.md §2 à
 * §5), sur PostgreSQL et Kafka réels : responsable d'une unité et périmètres, matrice des pouvoirs
 * (réglage sous FIDO2, journal), pouvoirs du compte connecté et route interne, demandes à
 * l'administration (403 ORG_HORS_PERIMETRE, 422 ORG_PROFIL_NON_DEMANDABLE, 409 ORG_DEMANDE_EXISTANTE,
 * acceptation sous FIDO2 qui crée réellement le compte, refus, annulation, événements iam.request-*),
 * profils créés et supprimés par l'administration.
 */
class PouvoirsHierarchiquesIT extends AbstractIamIT {

    static final RequestPostProcessor SERVICE = jwt().jwt(j -> j.subject("service-account-pilotage-service")).authorities(new SimpleGrantedAuthority("INTERNAL_IAM_READ"));
    static final String[] PERMISSIONS_CHEF = { "PIL_ACTIVITY_OWN", "PIL_TEAM_READ", "PIL_TASK_ASSIGN" };

    static RequestPostProcessor superAdmin() {
        return utilisateur(SUPER_ADMIN, PERMISSIONS_SUPER_ADMIN);
    }

    String suffixe() {
        return UUID.randomUUID().toString().substring(0, 6).toLowerCase(Locale.ROOT);
    }

    String creer(String identifiant, String profil) throws Exception {
        String corps = """
            {"identifiant":"%s","prenom":"%s","nom":"Mballa","email":"%s@gimac.test","fonction":"Fonction","profils":["%s"],"envoyerInvitation":true}"""
            .formatted(identifiant, identifiant.substring(0, identifiant.indexOf('.')), identifiant, profil);
        String reponse = mvc
            .perform(post("/api/v1/comptes").with(superAdmin()).contentType(MediaType.APPLICATION_JSON).content(corps))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
        return JsonPath.read(reponse, "$.compte.id");
    }

    void rattacher(String id, String unite, String responsable) throws Exception {
        String corps = "{\"unite\":\"%s\",\"responsableId\":%s}".formatted(unite, responsable == null ? "null" : "\"" + responsable + "\"");
        mvc.perform(put("/api/v1/comptes/{id}/rattachement", id).with(superAdmin()).contentType(MediaType.APPLICATION_JSON).content(corps)).andExpect(status().isOk());
    }

    /** Cellule propre au test, sous PROD_INFO, dirigée par le responsable donné. */
    String cellule(String s, String responsable) throws Exception {
        String code = "CELLULE_" + s.toUpperCase(Locale.ROOT).replace('-', '_');
        mvc
            .perform(
                post("/api/v1/organisation/unites").with(superAdmin()).contentType(MediaType.APPLICATION_JSON).content("{\"code\":\"%s\",\"libelle\":\"Cellule %s\",\"type\":\"CELLULE\",\"parent\":\"PROD_INFO\"}".formatted(code, s))
            )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.responsableId").value(Matchers.nullValue()));
        mvc
            .perform(
                put("/api/v1/organisation/unites/{code}", code)
                    .with(superAdmin())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"libelle\":\"Cellule %s\",\"type\":\"CELLULE\",\"parent\":\"PROD_INFO\",\"active\":true,\"responsableId\":\"%s\"}".formatted(s, responsable))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.responsableId").value(responsable));
        return code;
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

    ResultActions demander(String sujet, String corps) throws Exception {
        return mvc.perform(json(post("/api/v1/demandes").with(utilisateur(sujet, PERMISSIONS_CHEF)), corps));
    }

    static String id(ResultActions r) throws Exception {
        return JsonPath.read(r.andReturn().getResponse().getContentAsString(), "$.id");
    }

    @Test
    @DisplayName("§3 Matrice : jeu par défaut des responsables, AUCUNE ailleurs ; réglage sous FIDO2 et IAM_PROFILE_EDIT, journal ; profil privilégié non demandable")
    void matrice() throws Exception {
        mvc
            .perform(get("/api/v1/pouvoirs/matrice").with(utilisateur(RSSI, PERMISSIONS_RSSI)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.actions.length()").value(10))
            .andExpect(jsonPath("$.actions[8]").value("PROCESSUS_DEFINIR"))
            .andExpect(jsonPath("$.actions[9]").value("PROCESSUS_LANCER"))
            .andExpect(jsonPath("$.profils[?(@.profil == 'ROLE_CHEF_DES')].libelle").value(Matchers.contains("Chef du DES")))
            .andExpect(jsonPath("$.portees", Matchers.contains("AUCUNE", "DIRECTS", "EQUIPE", "UNITES", "TOUTES")))
            .andExpect(jsonPath("$.profils[?(@.profil == 'ROLE_CHEF_DES')].actions.TACHE_CONFIER").value(Matchers.contains("UNITES")))
            .andExpect(jsonPath("$.profils[?(@.profil == 'ROLE_CHEF_DES')].actions.ACTIVITE_LIRE").value(Matchers.contains("EQUIPE")))
            .andExpect(jsonPath("$.profils[?(@.profil == 'ROLE_CHEF_DES')].actions.RATTACHEMENT_MODIFIER").value(Matchers.contains("DIRECTS")))
            .andExpect(jsonPath("$.profils[?(@.profil == 'ROLE_DG')].actions.COMPTE_DEMANDER").value(Matchers.contains("UNITES")))
            .andExpect(jsonPath("$.profils[?(@.profil == 'ROLE_RESPONSABLE_SERVICE')].actions.PROFIL_DEMANDER").value(Matchers.contains("EQUIPE")))
            .andExpect(jsonPath("$.profils[?(@.profil == 'ROLE_CHEF_DES')].profilsDemandables[*]").value(Matchers.hasItems("ROLE_SNA", "ROLE_POLE_RESEAU", "ROLE_COLLABORATEUR")))
            .andExpect(jsonPath("$.profils[?(@.profil == 'ROLE_SNA')].actions.TACHE_CONFIER").value(Matchers.contains("AUCUNE")))
            .andExpect(jsonPath("$.profils[?(@.profil == 'ROLE_SNA')].actions.PROCESSUS_LANCER").value(Matchers.contains("AUCUNE")))
            .andExpect(jsonPath("$.profils[?(@.profil == 'ROLE_SUPER_ADMIN')].privilegie").value(Matchers.contains(true)))
            .andExpect(jsonPath("$.profils[?(@.profil == 'ROLE_SUPER_ADMIN')].actions.COMPTE_DEMANDER").value(Matchers.contains("AUCUNE")))
            .andExpect(jsonPath("$.profils[*].profilsDemandables[*]").value(Matchers.not(Matchers.hasItems("ROLE_SUPER_ADMIN"))))
            .andExpect(jsonPath("$.profils[*].profilsDemandables[*]").value(Matchers.not(Matchers.hasItems("ROLE_RSSI"))));
        mvc.perform(get("/api/v1/pouvoirs/matrice").with(utilisateur("agent", "PIL_ACTIVITY_OWN"))).andExpect(status().isForbidden());

        String admin = "admin-matrice-" + suffixe();
        CleDeSecuriteEmulee cle = enroler(admin);
        String corps = "{\"actions\":{\"TACHE_SUIVRE\":\"DIRECTS\",\"ACTIVITE_LIRE\":\"EQUIPE\"},\"profilsDemandables\":[\"ROLE_COLLABORATEUR\"],\"motif\":\"Chefs de projet\"}";
        String u = "/api/v1/pouvoirs/matrice/ROLE_CONDUCTEUR_PROJET";
        mvc.perform(json(put(u).with(utilisateur(admin, PERMISSIONS_SUPER_ADMIN)), corps)).andExpect(status().is(428));
        mvc.perform(json(put(u).with(utilisateur(admin, "IAM_ACCOUNT_READ")), corps).header("X-Reauth-Assertion", assertion(cle, admin))).andExpect(status().isForbidden());
        mvc
            .perform(json(put(u).with(utilisateur(admin, PERMISSIONS_SUPER_ADMIN)), corps).header("X-Reauth-Assertion", assertion(cle, admin)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.profil").value("ROLE_CONDUCTEUR_PROJET"))
            .andExpect(jsonPath("$.privilegie").value(false))
            .andExpect(jsonPath("$.cree").value(false))
            .andExpect(jsonPath("$.actions.TACHE_SUIVRE").value("DIRECTS"))
            .andExpect(jsonPath("$.actions.TACHE_CONFIER").value("AUCUNE"))
            .andExpect(jsonPath("$.profilsDemandables", Matchers.contains("ROLE_COLLABORATEUR")));
        assertThat(
            jdbc.queryForObject(
                "select details from journal_administration where action = 'MATRICE_POUVOIRS_MODIFIEE' and acteur_id = ? order by horodatage desc limit 1",
                String.class,
                admin
            )
        ).isEqualTo("avant: AUCUNE demandables=, après: TACHE_SUIVRE=DIRECTS ACTIVITE_LIRE=EQUIPE demandables=ROLE_COLLABORATEUR");

        mvc
            .perform(json(put(u).with(utilisateur(admin, PERMISSIONS_SUPER_ADMIN)), "{\"actions\":{},\"profilsDemandables\":[\"ROLE_RSSI\"]}").header("X-Reauth-Assertion", assertion(cle, admin)))
            .andExpect(status().is(422))
            .andExpect(jsonPath("$.code").value("ORG_PROFIL_NON_DEMANDABLE"));
        mvc
            .perform(json(put("/api/v1/pouvoirs/matrice/ROLE_INCONNU").with(utilisateur(admin, PERMISSIONS_SUPER_ADMIN)), "{\"actions\":{},\"profilsDemandables\":[]}").header("X-Reauth-Assertion", assertion(cle, admin)))
            .andExpect(status().isNotFound());
        // Remise de la ligne à AUCUNE (contexte partagé avec les autres tests).
        mvc
            .perform(json(put(u).with(utilisateur(admin, PERMISSIONS_SUPER_ADMIN)), "{\"actions\":{},\"profilsDemandables\":[]}").header("X-Reauth-Assertion", assertion(cle, admin)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.actions.TACHE_SUIVRE").value("AUCUNE"));
    }

    @Test
    @DisplayName("§2-§3 Responsable d'unité, unités dirigées, pouvoirs du compte connecté, route interne des pouvoirs et du périmètre")
    void pouvoirsEtPerimetre() throws Exception {
        String s = suffixe();
        String chef = creer("chef." + s, "ROLE_CHEF_DES");
        String agent = creer("agent." + s, "ROLE_SNA");
        String membre = creer("membre." + s, "ROLE_SNA");
        String code = cellule(s, chef);
        rattacher(agent, code, chef);
        rattacher(membre, code, null);

        mvc.perform(get("/api/v1/organisation/moi").with(utilisateur(chef, "PIL_ACTIVITY_OWN"))).andExpect(jsonPath("$.unitesDirigees", Matchers.contains(code)));
        mvc
            .perform(get("/api/v1/pouvoirs/moi").with(utilisateur(chef, PERMISSIONS_CHEF)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.actions.TACHE_CONFIER").value("UNITES"))
            .andExpect(jsonPath("$.actions.RAPPORT_VISER").value("EQUIPE"))
            .andExpect(jsonPath("$.actions.RATTACHEMENT_MODIFIER").value("DIRECTS"))
            .andExpect(jsonPath("$.profilsDemandables", Matchers.hasItems("ROLE_SNA", "ROLE_COLLABORATEUR")))
            .andExpect(jsonPath("$.profilsDemandables", Matchers.not(Matchers.hasItem("ROLE_RSSI"))))
            .andExpect(jsonPath("$.subordonnes[*].id", Matchers.contains(agent, membre)))
            .andExpect(jsonPath("$.subordonnes[0].niveau").value(1))
            .andExpect(jsonPath("$.subordonnes[1].niveau").value(0))
            .andExpect(jsonPath("$.subordonnes[0].unite").value(code))
            .andExpect(jsonPath("$.unitesDirigees", Matchers.contains(code)));
        mvc.perform(get("/api/v1/pouvoirs/moi").with(utilisateur(chef, "IAM_ACCOUNT_READ"))).andExpect(status().isForbidden());
        mvc
            .perform(get("/api/v1/pouvoirs/moi").with(utilisateur(agent, "PIL_ACTIVITY_OWN")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.actions.TACHE_CONFIER").value("AUCUNE"))
            .andExpect(jsonPath("$.subordonnes.length()").value(0))
            .andExpect(jsonPath("$.profilsDemandables.length()").value(0));

        mvc
            .perform(get("/api/v1/interne/pouvoirs/{id}", chef).with(SERVICE))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.actions.TACHE_SUIVRE").value("UNITES"))
            .andExpect(jsonPath("$.subordonnes.length()").value(2));
        mvc.perform(get("/api/v1/interne/pouvoirs/{id}", chef).with(utilisateur(chef, PERMISSIONS_CHEF))).andExpect(status().isForbidden());
        mvc
            .perform(get("/api/v1/interne/organisation/{id}/perimetre", chef).param("portee", "DIRECTS").with(SERVICE))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.portee").value("DIRECTS"))
            .andExpect(jsonPath("$.comptes[*].id", Matchers.contains(agent)))
            .andExpect(jsonPath("$.unites.length()").value(0));
        mvc
            .perform(get("/api/v1/interne/organisation/{id}/perimetre", chef).param("portee", "UNITES").with(SERVICE))
            .andExpect(jsonPath("$.comptes[*].id", Matchers.contains(agent, membre)))
            .andExpect(jsonPath("$.unites", Matchers.contains(code)));

        // Responsable d'unité invalide ; requête sans responsable : conservé.
        mvc
            .perform(
                put("/api/v1/organisation/unites/{code}", code)
                    .with(superAdmin())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"libelle\":\"Cellule\",\"type\":\"CELLULE\",\"parent\":\"PROD_INFO\",\"active\":true,\"responsableId\":\"inconnu\"}")
            )
            .andExpect(status().is(422))
            .andExpect(jsonPath("$.code").value("ORG_RESPONSABLE_INVALIDE"));
        mvc
            .perform(put("/api/v1/organisation/unites/{code}", code).with(superAdmin()).contentType(MediaType.APPLICATION_JSON).content("{\"libelle\":\"Cellule renommée\",\"type\":\"CELLULE\",\"parent\":\"PROD_INFO\",\"active\":true}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.responsableId").value(chef));
    }

    @Test
    @DisplayName("§4 Demandes : périmètre (403), profils demandables (422), unicité (409), acceptation sous FIDO2 qui crée le compte, refus, annulation, statut, événements")
    void demandes() throws Exception {
        String s = suffixe();
        String chef = creer("chef." + s, "ROLE_CHEF_DES");
        String agent = creer("agent." + s, "ROLE_SNA");
        String etranger = creer("etranger." + s, "ROLE_SNA");
        String code = cellule(s, chef);
        rattacher(agent, code, chef);
        String admin = "admin-demandes-" + s;
        CleDeSecuriteEmulee cle = enroler(admin);
        RequestPostProcessor administration = utilisateur(admin, PERMISSIONS_SUPER_ADMIN);

        // 403 hors périmètre ; 403 sans pouvoir (agent sans profil responsable).
        demander(chef, "{\"type\":\"STATUT\",\"cibleId\":\"%s\",\"contenu\":{\"actif\":false},\"motif\":\"Départ en congé\"}".formatted(etranger))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("ORG_HORS_PERIMETRE"));
        demander(agent, "{\"type\":\"STATUT\",\"cibleId\":\"%s\",\"contenu\":{\"actif\":false},\"motif\":\"Départ en congé\"}".formatted(chef))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("ORG_HORS_PERIMETRE"));
        String creationHors = """
            {"type":"CREATION_COMPTE","contenu":{"identifiant":"hors.%s","prenom":"Hors","nom":"Champ","email":"hors.%s@gimac.test","fonction":"Analyste","unite":"SSI","responsableId":"%s","profils":["ROLE_COLLABORATEUR"]},"motif":"Renfort de l'équipe"}"""
            .formatted(s, s, chef);
        demander(chef, creationHors).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("ORG_HORS_PERIMETRE"));

        // 422 profils non demandables (privilégié, hors du domaine du chef).
        for (String profil : List.of("ROLE_RSSI", "ROLE_SUPER_ADMIN", "ROLE_CHARGE_DDRM")) {
            demander(chef, "{\"type\":\"PROFILS\",\"cibleId\":\"%s\",\"contenu\":{\"ajouter\":[\"%s\"]},\"motif\":\"Nouvelles missions\"}".formatted(agent, profil))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.code").value("ORG_PROFIL_NON_DEMANDABLE"));
        }
        demander(chef, "{\"type\":\"PROFILS\",\"cibleId\":\"%s\",\"contenu\":{\"ajouter\":[\"ROLE_SNA\"]},\"motif\":\"Rien ne change\"}".formatted(agent))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("ORG_DEMANDE_INVALIDE"));
        demander(chef, "{\"type\":\"PROFILS\",\"cibleId\":\"%s\",\"contenu\":{\"ajouter\":[\"ROLE_POLE_RESEAU\"]},\"motif\":\"abc\"}".formatted(agent)).andExpect(status().isBadRequest());

        // Demande de profils, puis 409 pour une seconde demande en attente.
        String profils = "{\"type\":\"PROFILS\",\"cibleId\":\"%s\",\"contenu\":{\"ajouter\":[\"ROLE_POLE_RESEAU\"],\"retirer\":[]},\"motif\":\"Nouvelles missions réseau\"}".formatted(agent);
        ResultActions r = demander(chef, profils)
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.type").value("PROFILS"))
            .andExpect(jsonPath("$.demandeur.id").value(chef))
            .andExpect(jsonPath("$.demandeur.nomAffiche").value("chef Mballa"))
            .andExpect(jsonPath("$.cible.id").value(agent))
            .andExpect(jsonPath("$.cible.nomAffiche").value("agent Mballa"))
            .andExpect(jsonPath("$.contenu.ajouter", Matchers.contains("ROLE_POLE_RESEAU")))
            .andExpect(jsonPath("$.contenu.identifiant").doesNotExist())
            .andExpect(jsonPath("$.motif").value("Nouvelles missions réseau"))
            .andExpect(jsonPath("$.etat").value("EN_ATTENTE"))
            .andExpect(jsonPath("$.decision").value(Matchers.nullValue()))
            .andExpect(jsonPath("$.creeeLe").isNotEmpty());
        String demandeProfils = id(r);
        demander(chef, profils).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("ORG_DEMANDE_EXISTANTE"));

        // Création de compte proposée dans la cellule dirigée.
        String identifiant = "nouveau." + s;
        String creation = """
            {"type":"CREATION_COMPTE","contenu":{"identifiant":"%s","prenom":"Nouvel","nom":"Agent","email":"%s@gimac.test","telephone":"+237600000011","fonction":"Technicien réseau","unite":"%s","responsableId":"%s","profils":["ROLE_COLLABORATEUR","ROLE_POLE_RESEAU"]},"motif":"Renfort de la cellule"}"""
            .formatted(identifiant, identifiant, code, chef);
        String demandeCreation = id(
            demander(chef, creation)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cible").value(Matchers.nullValue()))
                .andExpect(jsonPath("$.contenu.unite").value(code))
                .andExpect(jsonPath("$.contenu.profils", Matchers.contains("ROLE_COLLABORATEUR", "ROLE_POLE_RESEAU")))
        );
        demander(chef, creation).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("ORG_DEMANDE_EXISTANTE"));
        String soumis = jdbc.queryForObject("select payload from outbox_event where aggregate_id = ? and event_type = 'iam.request-submitted.v1'", String.class, demandeCreation);
        assertThat(JsonPath.<List<String>>read(soumis, "$.destinataires")).contains(SUPER_ADMIN).doesNotContain(chef);
        assertThat(JsonPath.<String>read(soumis, "$.demandeurId")).isEqualTo(chef);

        // Lectures : miennes, à traiter (administration seulement), une demande (demandeur ou administration).
        mvc
            .perform(get("/api/v1/demandes").param("vue", "miennes").with(utilisateur(chef, PERMISSIONS_CHEF)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total").value(2))
            .andExpect(jsonPath("$.contenu[0].id").value(demandeCreation))
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.taille").value(25));
        mvc.perform(get("/api/v1/demandes").param("vue", "a-traiter").with(utilisateur(chef, PERMISSIONS_CHEF))).andExpect(status().isForbidden());
        mvc
            .perform(get("/api/v1/demandes").param("vue", "a-traiter").param("etat", "EN_ATTENTE").param("taille", "100").with(administration))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.contenu[*].id", Matchers.hasItems(demandeCreation, demandeProfils)));
        mvc.perform(get("/api/v1/demandes/{id}", demandeProfils).with(utilisateur(etranger, "PIL_ACTIVITY_OWN"))).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/demandes/{id}", demandeProfils).with(administration)).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(demandeProfils));

        // Acceptation de la création : FIDO2 exigé, permission de l'acte, compte réellement créé et rattaché.
        String acceptation = "/api/v1/demandes/{id}/acceptation";
        mvc.perform(post(acceptation, demandeCreation).with(administration)).andExpect(status().is(428));
        mvc.perform(post(acceptation, demandeCreation).with(utilisateur(admin, "IAM_ACCOUNT_READ"))).andExpect(status().isForbidden());
        mvc
            .perform(post(acceptation, demandeCreation).with(utilisateur(admin, "IAM_ACCOUNT_ROLES", "IAM_ACCOUNT_READ")).header("X-Reauth-Assertion", assertion(cle, admin)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("ORG_DEMANDE_INTERDITE"));
        String accepte = mvc
            .perform(json(post(acceptation, demandeCreation).with(administration), "{\"motif\":\"Validé par la DG\"}").header("X-Reauth-Assertion", assertion(cle, admin)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.etat").value("ACCEPTEE"))
            .andExpect(jsonPath("$.decision.par.id").value(admin))
            .andExpect(jsonPath("$.decision.motif").value("Validé par la DG"))
            .andExpect(jsonPath("$.decision.le").isNotEmpty())
            .andExpect(jsonPath("$.cible.id").isNotEmpty())
            .andReturn()
            .getResponse()
            .getContentAsString();
        String cree = JsonPath.read(accepte, "$.cible.id");
        mvc
            .perform(get("/api/v1/comptes/{id}", cree).with(superAdmin()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.identifiant").value(identifiant))
            .andExpect(jsonPath("$.unite").value(code))
            .andExpect(jsonPath("$.responsableId").value(chef))
            .andExpect(jsonPath("$.profils", Matchers.contains("ROLE_COLLABORATEUR", "ROLE_POLE_RESEAU")))
            .andExpect(jsonPath("$.premierAccesEnAttente").value(true));
        assertThat(jdbc.queryForList("select action from journal_administration where cible_id = ?", String.class, cree)).contains("CREATION", "INVITATION", "COMPTE_RATTACHEMENT_MODIFIE");
        assertThat(jdbc.queryForList("select action from journal_administration where cible_id = ?", String.class, demandeCreation)).contains("DEMANDE_SOUMISE", "DEMANDE_ACCEPTEE");
        mvc
            .perform(post(acceptation, demandeCreation).with(administration).header("X-Reauth-Assertion", assertion(cle, admin)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("ORG_DEMANDE_TRAITEE"));

        relais.relayerJusquaEnvoi(demandeCreation);
        var publies = relais.lire("iam.request", demandeCreation);
        assertThat(publies).anySatisfy(e -> {
            assertThat(JsonPath.<String>read(e.value(), "$.type")).isEqualTo("iam.request-decided.v1");
            assertThat(JsonPath.<String>read(e.value(), "$.data.etat")).isEqualTo("ACCEPTEE");
            assertThat(JsonPath.<String>read(e.value(), "$.data.cibleId")).isEqualTo(cree);
            assertThat(JsonPath.<List<String>>read(e.value(), "$.data.destinataires")).containsExactly(chef);
        });
        assertThat(publies).anySatisfy(e -> {
            assertThat(JsonPath.<String>read(e.value(), "$.type")).isEqualTo("iam.request-submitted.v1");
            assertThat(JsonPath.<String>read(e.value(), "$.data.demandeurId")).isEqualTo(chef);
            assertThat(JsonPath.<String>read(e.value(), "$.data.type")).isEqualTo("CREATION_COMPTE");
        });

        // Refus motivé de la demande de profils.
        mvc.perform(json(post("/api/v1/demandes/{id}/refus", demandeProfils).with(utilisateur(chef, PERMISSIONS_CHEF)), "{\"motif\":\"Refus du chef\"}")).andExpect(status().isForbidden());
        mvc.perform(json(post("/api/v1/demandes/{id}/refus", demandeProfils).with(administration), "{\"motif\":\"non\"}")).andExpect(status().isBadRequest());
        mvc
            .perform(json(post("/api/v1/demandes/{id}/refus", demandeProfils).with(administration), "{\"motif\":\"Profil réseau non justifié\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.etat").value("REFUSEE"))
            .andExpect(jsonPath("$.decision.motif").value("Profil réseau non justifié"));
        mvc.perform(get("/api/v1/comptes/{id}", agent).with(superAdmin())).andExpect(jsonPath("$.profils", Matchers.contains("ROLE_SNA")));
        assertThat(jdbc.queryForObject("select payload from outbox_event where aggregate_id = ? and event_type = 'iam.request-decided.v1'", String.class, demandeProfils))
            .contains("REFUSEE")
            .contains(chef);

        // Annulation par le demandeur seulement ; une nouvelle demande devient possible ; acceptation du statut.
        String statut = "{\"type\":\"STATUT\",\"cibleId\":\"%s\",\"contenu\":{\"actif\":false},\"motif\":\"Fin de mission\"}".formatted(agent);
        String demandeStatut = id(demander(chef, statut).andExpect(status().isCreated()));
        mvc.perform(post("/api/v1/demandes/{id}/annulation", demandeStatut).with(utilisateur(admin, "PIL_ACTIVITY_OWN", "IAM_ACCOUNT_READ"))).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/demandes/{id}/annulation", demandeStatut).with(utilisateur(chef, PERMISSIONS_CHEF))).andExpect(status().isOk()).andExpect(jsonPath("$.etat").value("ANNULEE"));
        mvc.perform(post("/api/v1/demandes/{id}/annulation", demandeStatut).with(utilisateur(chef, PERMISSIONS_CHEF))).andExpect(status().isConflict());
        String demandeStatut2 = id(demander(chef, statut).andExpect(status().isCreated()));
        mvc
            .perform(post(acceptation, demandeStatut2).with(utilisateur(admin, "IAM_ACCOUNT_STATUS")).header("X-Reauth-Assertion", assertion(cle, admin)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.etat").value("ACCEPTEE"));
        mvc.perform(get("/api/v1/comptes/{id}", agent).with(superAdmin())).andExpect(jsonPath("$.actif").value(false));
    }

    @Test
    @DisplayName("§5 Profils créés : FIDO2, permissions communes, attribuable et dans la matrice, INTERNAL_* 422, privilégié réservé, livré non supprimable, titulaires")
    void profilsCrees() throws Exception {
        String s = suffixe().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "X");
        String code = "ROLE_QUALITE_" + s;
        String admin = "admin-profils-" + s.toLowerCase(Locale.ROOT);
        CleDeSecuriteEmulee cle = enroler(admin);
        RequestPostProcessor administration = utilisateur(admin, PERMISSIONS_SUPER_ADMIN);
        String corps = "{\"code\":\"%s\",\"description\":\"Contrôle qualité\",\"permissions\":[\"HOM_DASHBOARD_READ\"]}".formatted(code);
        String u = "/api/v1/configuration/profils";

        mvc.perform(json(post(u).with(administration), corps)).andExpect(status().is(428));
        mvc.perform(json(post(u).with(utilisateur(admin, "IAM_ACCOUNT_READ")), corps).header("X-Reauth-Assertion", assertion(cle, admin))).andExpect(status().isForbidden());
        mvc
            .perform(json(post(u).with(administration), corps).header("X-Reauth-Assertion", assertion(cle, admin)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.code").value(code))
            .andExpect(jsonPath("$.description").value("Contrôle qualité"))
            .andExpect(jsonPath("$.privilegie").value(false))
            .andExpect(jsonPath("$.cree").value(true))
            .andExpect(jsonPath("$.titulaires").value(0))
            .andExpect(jsonPath("$.permissions", Matchers.containsInAnyOrder("HOM_DASHBOARD_READ", "COL_MESSAGE_READ", "COL_MESSAGE_SEND", "PIL_ACTIVITY_OWN", "NOT_INBOX_READ")));
        mvc.perform(json(post(u).with(administration), corps).header("X-Reauth-Assertion", assertion(cle, admin))).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("IAM_PROFIL_EXISTANT"));
        mvc
            .perform(get(u).with(administration))
            .andExpect(jsonPath("$[?(@.code == '%s')].cree".formatted(code)).value(Matchers.contains(true)))
            .andExpect(jsonPath("$[?(@.code == 'ROLE_SNA')].cree").value(Matchers.contains(false)));
        mvc
            .perform(get("/api/v1/pouvoirs/matrice").with(administration))
            .andExpect(jsonPath("$.profils[?(@.profil == '%s')].cree".formatted(code)).value(Matchers.contains(true)))
            .andExpect(jsonPath("$.profils[?(@.profil == '%s')].actions.TACHE_CONFIER".formatted(code)).value(Matchers.contains("AUCUNE")));
        assertThat(jdbc.queryForObject("select count(*) from journal_administration where action = 'CONFIGURATION_PROFIL_CREE' and details like ?", Long.class, "%code=" + code + "%")).isEqualTo(1L);

        // Aussitôt attribuable ; non supprimable tant qu'il a un titulaire.
        String titulaire = creer("qualite." + s.toLowerCase(Locale.ROOT), code);
        mvc
            .perform(delete(u + "/{code}", code).with(administration).header("X-Reauth-Assertion", assertion(cle, admin)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("IAM_PROFIL_TITULAIRES"));
        mvc
            .perform(json(put("/api/v1/comptes/{id}/profils", titulaire).with(administration), "{\"profils\":[\"ROLE_SNA\"],\"motif\":\"Mutation\"}").header("X-Reauth-Assertion", assertion(cle, admin)))
            .andExpect(status().isOk());
        mvc.perform(delete(u + "/{code}", code).with(administration)).andExpect(status().is(428));
        mvc.perform(delete(u + "/{code}", code).param("motif", "Profil abandonné").with(administration).header("X-Reauth-Assertion", assertion(cle, admin))).andExpect(status().isNoContent());
        mvc.perform(get(u).with(administration)).andExpect(jsonPath("$[*].code", Matchers.not(Matchers.hasItem(code))));
        mvc.perform(delete(u + "/{code}", code).with(administration).header("X-Reauth-Assertion", assertion(cle, admin))).andExpect(status().isNotFound());

        // Refus : livré, INTERNAL_*, code invalide, privilégié sans octroi privilégié.
        mvc
            .perform(delete(u + "/ROLE_SNA").with(administration).header("X-Reauth-Assertion", assertion(cle, admin)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("IAM_PROFIL_SYSTEME"));
        mvc
            .perform(json(post(u).with(administration), "{\"code\":\"ROLE_SERVICE_%s\",\"description\":\"x\",\"permissions\":[\"INTERNAL_IAM_READ\"]}".formatted(s)).header("X-Reauth-Assertion", assertion(cle, admin)))
            .andExpect(status().is(422))
            .andExpect(jsonPath("$.code").value("IAM_PROFIL_PERMISSION_INTERNE"));
        mvc
            .perform(json(post(u).with(administration), "{\"code\":\"QUALITE\",\"description\":\"x\",\"permissions\":[]}").header("X-Reauth-Assertion", assertion(cle, admin)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("IAM_PROFIL_CODE_INVALIDE"));
        String rssi = "rssi-profils-" + s.toLowerCase(Locale.ROOT);
        CleDeSecuriteEmulee cleRssi = enroler(rssi);
        mvc
            .perform(
                json(post(u).with(utilisateur(rssi, PERMISSIONS_RSSI)), "{\"code\":\"ROLE_AUDIT_%s\",\"description\":\"Audit\",\"permissions\":[\"IAM_ACCOUNT_READ\"]}".formatted(s)).header(
                    "X-Reauth-Assertion",
                    assertion(cleRssi, rssi)
                )
            )
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("IAM_PRIVILEGED_PROFILE"));
        // Profil privilégié créé par le super administrateur : signalé comme tel.
        mvc
            .perform(json(post(u).with(administration), "{\"code\":\"ROLE_AUDIT_%s\",\"description\":\"Audit\",\"permissions\":[\"IAM_ACCOUNT_READ\"]}".formatted(s)).header("X-Reauth-Assertion", assertion(cle, admin)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.privilegie").value(true));
        mvc
            .perform(get("/api/v1/pouvoirs/matrice").with(administration))
            .andExpect(jsonPath("$.profils[?(@.profil == 'ROLE_AUDIT_%s')].privilegie".formatted(s)).value(Matchers.contains(true)));
    }

    ResultActions rattacherSubordonne(String sujet, String cible, String corps, String... permissions) throws Exception {
        return mvc.perform(json(put("/api/v1/pouvoirs/subordonnes/{id}/rattachement", cible).with(utilisateur(sujet, permissions)), corps));
    }

    @Test
    @DisplayName("§3 Rattachement d'un subordonné (RATTACHEMENT_MODIFIER, DIRECTS) : succès journalisé, hors périmètre, unité non dirigée, n+1 hors périmètre, soi-même, matrice AUCUNE")
    void rattachementSubordonne() throws Exception {
        String s = suffixe();
        String chef = creer("chef." + s, "ROLE_CHEF_DES");
        String agent = creer("agent." + s, "ROLE_SNA");
        String technicien = creer("technicien." + s, "ROLE_SNA");
        String membre = creer("membre." + s, "ROLE_SNA");
        String etranger = creer("etranger." + s, "ROLE_SNA");
        String code = cellule(s, chef);
        rattacher(agent, "PROD_INFO", chef);
        rattacher(technicien, "PROD_INFO", agent);
        rattacher(membre, code, null);
        String corps = "{\"unite\":\"%s\",\"responsableId\":\"%s\",\"motif\":\"Réorganisation\"}";

        // Sans PIL_ACTIVITY_OWN : 403.
        rattacherSubordonne(chef, agent, corps.formatted(code, chef), "IAM_ACCOUNT_READ").andExpect(status().isForbidden());
        // Succès : unité dirigée, le chef reste n+1 ; réponse : la fiche du compte.
        rattacherSubordonne(chef, agent, corps.formatted(code, chef), PERMISSIONS_CHEF)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(agent))
            .andExpect(jsonPath("$.unite").value(code))
            .andExpect(jsonPath("$.responsableId").value(chef))
            .andExpect(jsonPath("$.identifiant").value("agent." + s));
        assertThat(
            jdbc.queryForObject(
                "select acteur_id || '|' || motif || '|' || details from journal_administration where action = 'COMPTE_RATTACHEMENT_MODIFIE' and cible_id = ? order by horodatage desc limit 1",
                String.class,
                agent
            )
        ).isEqualTo(chef + "|Réorganisation|avant: unite=PROD_INFO responsable=" + chef + ", après: unite=" + code + " responsable=" + chef + ", pouvoir=RATTACHEMENT_MODIFIER portee=DIRECTS");
        assertThat(jdbc.queryForObject("select count(*) from outbox_event where aggregate_id = ? and event_type = 'iam.account-administered.v1'", Integer.class, agent)).isPositive();

        // Hors périmètre DIRECTS : n+2 et membre d'une unité dirigée hors chaîne ; compte étranger.
        for (String cible : List.of(technicien, membre, etranger)) {
            rattacherSubordonne(chef, cible, corps.formatted(code, chef), PERMISSIONS_CHEF).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("ORG_HORS_PERIMETRE"));
        }
        // Unité non dirigée ; n+1 hors périmètre ; soi-même.
        rattacherSubordonne(chef, agent, corps.formatted("DG", chef), PERMISSIONS_CHEF).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("ORG_HORS_PERIMETRE"));
        rattacherSubordonne(chef, agent, corps.formatted(code, etranger), PERMISSIONS_CHEF).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("ORG_HORS_PERIMETRE"));
        rattacherSubordonne(chef, chef, corps.formatted(code, chef), PERMISSIONS_CHEF).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("IAM_SELF_ADMINISTRATION"));
        // Règles habituelles : un compte n'est pas son propre n+1.
        rattacherSubordonne(chef, agent, corps.formatted(code, agent), PERMISSIONS_CHEF).andExpect(status().is(422)).andExpect(jsonPath("$.code").value("ORG_RESPONSABLE_INVALIDE"));
        // Matrice AUCUNE (ROLE_SNA) : l'agent ne rattache pas son propre subordonné.
        rattacherSubordonne(agent, technicien, corps.formatted("PROD_INFO", agent), PERMISSIONS_CHEF).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("ORG_HORS_PERIMETRE"));
        mvc
            .perform(get("/api/v1/comptes/{id}", agent).with(superAdmin()))
            .andExpect(jsonPath("$.unite").value(code))
            .andExpect(jsonPath("$.responsableId").value(chef));
    }
}
