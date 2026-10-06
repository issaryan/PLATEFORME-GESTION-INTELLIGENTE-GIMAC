package cm.gimac.des.homologation.web.rest.lot1;

import static cm.gimac.des.homologation.web.rest.lot1.ModelisateurIT.json;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cm.gimac.des.homologation.AbstractHomologationIT;
import cm.gimac.des.homologation.domain.enumeration.TypeProjet;
import com.jayway.jsonpath.JsonPath;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Référentiel des opérations : droits d'édition, lecture filtrée, étapes configurables (D-89,
 * docs/lot1/referentiel-droits-etapes.md), par l'API.
 */
class ReferentielDroitsEtapesIT extends AbstractHomologationIT {

    static final UUID CHEF_DES = UUID.fromString("00000000-0000-4000-8000-000000089c01");
    static final UUID INGENIEUR = UUID.fromString("00000000-0000-4000-8000-000000089e01");
    static final UUID LECTEUR = UUID.fromString("00000000-0000-4000-8000-000000089a01");

    /** Chef du DES : éditeur, publieur, lecture complète (profils du jeton, comme Keycloak les pose). */
    static RequestPostProcessor chefDes() {
        return utilisateur(CHEF_DES, "ROLE_CHEF_DES", "HOM_REFERENTIEL_READ", "HOM_REFERENTIEL_READ_ALL", "HOM_REFERENTIEL_EDIT", "HOM_REFERENTIEL_PUBLISH");
    }

    /** Ingénieur d'homologation qui n'a créé ni la version publiée ni le brouillon qu'il modifie. */
    static RequestPostProcessor ingenieur() {
        return utilisateur(INGENIEUR, "ROLE_INGENIEUR_HOMOLOGATION", "HOM_REFERENTIEL_READ", "HOM_REFERENTIEL_EDIT", "HOM_REFERENTIEL_PUBLISH");
    }

    static String operation(String code, String etape, String r, String v, String consultes, String preds, String deposant) {
        return (
            "{" +
            (code == null ? "" : "\"code\":\"" + code + "\",") +
            "\"libelle\":\"Opération " + code + "\",\"description\":\"Actions\",\"etape\":\"" + etape + "\",\"segment\":\"TRONC_COMMUN_AMONT\"," +
            "\"ordre\":1,\"responsable\":\"" + r + "\",\"validateur\":\"" + v + "\",\"consultes\":[" + consultes + "],\"informes\":[],\"externes\":[]," +
            "\"predecesseurs\":[" + preds + "],\"delaiJours\":2,\"grain\":\"OPERATION\",\"geste\":\"DEPOSER_LIVRABLE\"," +
            "\"livrables\":[{\"code\":\"L1\",\"libelle\":\"Compte rendu\",\"nature\":\"LIVRABLE\",\"deposant\":\"" + deposant + "\",\"typesPieces\":[\"application/pdf\"],\"obligatoire\":true}]}"
        );
    }

    String creer(RequestPostProcessor qui, String corps) throws Exception {
        String r = mvc.perform(json(post("/api/v1/referentiels").with(qui), corps)).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(r, "$.id");
    }

    @Test
    @DisplayName("Un ingénieur d'homologation qui n'est pas le créateur crée, dérive, modifie, déplace, relie, contrôle et publie")
    void ingenieurNonCreateur() throws Exception {
        UUID publiee = fixtures.referentielMinimalPublie(TypeProjet.DELEGATAIRE);
        // Brouillon créé et piloté par le chef du DES.
        String id = creer(chefDes(), "{\"typeProjet\":\"DELEGATAIRE\",\"libelle\":\"Délégataire v2\",\"versionSourceId\":\"" + publiee + "\"}");
        String base = "/api/v1/referentiels/" + id;
        mvc.perform(get(base).with(ingenieur())).andExpect(jsonPath("$.piloteId").value(CHEF_DES.toString())).andExpect(jsonPath("$.actionsPermises").value(hasItem("MODIFIER")));

        // L'ingénieur crée un brouillon et dérive la version publiée (créée par un autre).
        creer(ingenieur(), "{\"typeProjet\":\"PAIEMENT_MOBILE\",\"libelle\":\"Paiement mobile v1\"}");
        creer(ingenieur(), "{\"typeProjet\":\"DELEGATAIRE\",\"libelle\":\"Dérivation de l'ingénieur\",\"versionSourceId\":\"" + publiee + "\"}");

        // Il modifie le brouillon du chef : ajout, écriture, déplacement, arc, retrait, contrôle, publication.
        mvc
            .perform(json(post(base + "/operations").with(ingenieur()), operation("DL-X-01", "LANCEMENT", "SNA", "CONDUCTEUR", "", "\"DL-AMONT-01\"", "SNA")))
            .andExpect(status().isCreated());
        mvc
            .perform(json(put(base + "/operations/DL-X-01").with(ingenieur()), operation(null, "ATELIERS", "SNA", "CONDUCTEUR", "\"RSSI\"", "\"DL-AMONT-01\"", "SNA")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.etape").value("ATELIERS"))
            .andExpect(jsonPath("$.fantome").value(false));
        mvc.perform(json(post(base + "/operations").with(ingenieur()), operation("DL-X-02", "ATELIERS", "SNA", "CONDUCTEUR", "", "", "SNA"))).andExpect(status().isCreated());
        mvc.perform(delete(base + "/operations/DL-X-02").with(ingenieur())).andExpect(status().isNoContent());
        mvc.perform(json(put(base + "/positions").with(ingenieur()), "[{\"code\":\"DL-X-01\",\"x\":420,\"y\":80}]")).andExpect(status().isNoContent());
        mvc
            .perform(json(put(base + "/arcs").with(ingenieur()), "{\"de\":\"DL-X-01\",\"vers\":\"DL-BR-01\",\"present\":true}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.predecesseurs").value(hasItem("DL-X-01")));
        mvc.perform(post(base + "/controle").with(ingenieur())).andExpect(status().isOk()).andExpect(jsonPath("$.conforme").value(true));
        mvc.perform(post(base + "/publication").with(ingenieur())).andExpect(status().isOk()).andExpect(jsonPath("$.etat").value("PUBLIEE"));
        mvc.perform(get("/api/v1/referentiels/" + publiee).with(ingenieur())).andExpect(jsonPath("$.etat").value("RETIREE"));
    }

    @Test
    @DisplayName("Étapes : ajout, renommage, ordre, retrait d'une étape vide ; canonique protégée ; copiées à la dérivation ; graphe et catalogue")
    void etapes() throws Exception {
        UUID publiee = fixtures.referentielMinimalPublie(TypeProjet.CARTE_INTERBANCAIRE);
        String id = creer(chefDes(), "{\"typeProjet\":\"CARTE_INTERBANCAIRE\",\"libelle\":\"Carte v2\",\"versionSourceId\":\"" + publiee + "\"}");
        String base = "/api/v1/referentiels/" + id;
        mvc.perform(get("/api/v1/referentiels/" + publiee + "/etapes").with(ingenieur())).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(11));
        mvc
            .perform(get(base + "/etapes").with(ingenieur()))
            .andExpect(jsonPath("$[7].code").value("CERTIFICATION"))
            .andExpect(jsonPath("$[7].protegee").value(true))
            .andExpect(jsonPath("$[7].supprimable").value(false))
            .andExpect(jsonPath("$[6].jeton").value("--gm-color-status-warn-fg"));

        mvc
            .perform(json(post(base + "/etapes").with(ingenieur()), "{\"code\":\"REVUE_JURIDIQUE\",\"libelle\":\"Revue juridique\",\"couleur\":\"ACCENT\",\"ordre\":8}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.ordre").value(8))
            .andExpect(jsonPath("$.canonique").doesNotExist())
            .andExpect(jsonPath("$.canoniqueEffective").value("TESTS"))
            .andExpect(jsonPath("$.supprimable").value(true));
        mvc.perform(json(post(base + "/etapes").with(ingenieur()), "{\"code\":\"REVUE_JURIDIQUE\",\"libelle\":\"Doublon\"}")).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("HOM_ETAPE_CODE_DUPLIQUE"));
        mvc.perform(json(post(base + "/etapes").with(ingenieur()), "{\"code\":\"revue\",\"libelle\":\"Minuscules\"}")).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("HOM_ETAPE_CODE_INVALIDE"));
        mvc.perform(json(post(base + "/etapes").with(ingenieur()), "{\"code\":\"SANS_LIBELLE\"}")).andExpect(status().isBadRequest());
        mvc.perform(json(post(base + "/etapes").with(ingenieur()), "{\"code\":\"ROSE\",\"libelle\":\"Rose\",\"couleur\":\"ROSE\"}")).andExpect(status().isBadRequest());
        mvc
            .perform(json(put(base + "/etapes/REVUE_JURIDIQUE").with(ingenieur()), "{\"libelle\":\"Revue juridique et conformité\",\"couleur\":\"INFO\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.libelle").value("Revue juridique et conformité"))
            .andExpect(jsonPath("$.couleur").value("INFO"));
        mvc.perform(json(put(base + "/etapes/CERTIFICATION").with(ingenieur()), "{\"libelle\":\"Certification\"}")).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("HOM_ETAPE_CANONIQUE"));

        mvc
            .perform(json(post(base + "/operations").with(ingenieur()), operation("CI-J-01", "REVUE_JURIDIQUE", "CONFORMITE", "CONDUCTEUR", "", "\"CI-AMONT-01\"", "CONFORMITE")))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.etape").value("REVUE_JURIDIQUE"));
        mvc
            .perform(json(post(base + "/operations").with(ingenieur()), operation("CI-J-02", "INCONNUE", "CONFORMITE", "CONDUCTEUR", "", "", "CONFORMITE")))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("HOM_ETAPE_INCONNUE"));
        mvc.perform(delete(base + "/etapes/REVUE_JURIDIQUE").with(ingenieur())).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("HOM_ETAPE_NON_VIDE"));
        mvc.perform(delete(base + "/etapes/APPROBATION_SECURITE").with(ingenieur())).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("HOM_ETAPE_CANONIQUE"));
        mvc.perform(delete(base + "/etapes/SUIVI").with(ingenieur())).andExpect(status().isNoContent());
        mvc.perform(delete(base + "/etapes/SUIVI").with(ingenieur())).andExpect(status().isNotFound());
        mvc.perform(delete(base + "/etapes/ADHESION").with(utilisateur(LECTEUR, "HOM_REFERENTIEL_READ"))).andExpect(status().isForbidden());

        List<String> codes = JsonPath.read(mvc.perform(get(base + "/etapes").with(ingenieur())).andReturn().getResponse().getContentAsString(), "$[*].code");
        assertThat(codes).hasSize(11).contains("REVUE_JURIDIQUE").doesNotContain("SUIVI");
        List<String> inverse = new java.util.ArrayList<>(codes);
        java.util.Collections.reverse(inverse);
        mvc
            .perform(json(put(base + "/etapes/ordre").with(ingenieur()), "{\"codes\":" + JsonPath.parse(inverse).jsonString() + "}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].code").value(inverse.get(0)))
            .andExpect(jsonPath("$[0].ordre").value(1));
        mvc
            .perform(json(put(base + "/etapes/ordre").with(ingenieur()), "{\"codes\":[\"CADRAGE\"]}"))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("HOM_ETAPE_ORDRE_INVALIDE"));
        mvc.perform(json(put(base + "/etapes/ordre").with(ingenieur()), "{\"codes\":" + JsonPath.parse(codes).jsonString() + "}")).andExpect(status().isOk());

        mvc
            .perform(get(base + "/graphe").with(ingenieur()))
            .andExpect(jsonPath("$.filtre").value("COMPLET"))
            .andExpect(jsonPath("$.etapes.length()").value(11))
            .andExpect(jsonPath("$.etapes[?(@.code=='REVUE_JURIDIQUE')].nombreOperations").value(contains(1)))
            .andExpect(jsonPath("$.noeuds[?(@.code=='CI-J-01')].etape").value(contains("REVUE_JURIDIQUE")));
        mvc
            .perform(get("/api/v1/referentiels/catalogue").param("version", id).with(ingenieur()))
            .andExpect(jsonPath("$.etapes[*].code").value(hasItem("REVUE_JURIDIQUE")))
            .andExpect(jsonPath("$.etapes[*].code").value(not(hasItem("SUIVI"))))
            .andExpect(jsonPath("$.etapesVersion.length()").value(11))
            .andExpect(jsonPath("$.etapesCanoniques.length()").value(11))
            .andExpect(jsonPath("$.etapesCanoniques[?(@.code=='MISE_EN_PRODUCTION')].protegee").value(contains(true)))
            .andExpect(jsonPath("$.couleurs[*].code").value(hasItem("ACCENT")));
        mvc
            .perform(get("/api/v1/referentiels/catalogue").with(ingenieur()))
            .andExpect(jsonPath("$.etapes.length()").value(11))
            .andExpect(jsonPath("$.etapesVersion").doesNotExist());

        mvc.perform(post(base + "/publication").with(ingenieur())).andExpect(status().isOk());
        mvc.perform(json(post(base + "/etapes").with(ingenieur()), "{\"code\":\"APRES\",\"libelle\":\"Après\"}")).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("HOM_REFERENTIAL_FROZEN"));
        mvc.perform(delete(base + "/etapes/REVUE_JURIDIQUE").with(ingenieur())).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("HOM_REFERENTIAL_FROZEN"));
        mvc.perform(get(base + "/etapes").with(ingenieur())).andExpect(jsonPath("$[*].supprimable").value(not(hasItem(true))));

        // Dérivation : les étapes de la version sont copiées dans le nouveau brouillon.
        String derive = creer(ingenieur(), "{\"typeProjet\":\"CARTE_INTERBANCAIRE\",\"libelle\":\"Carte v3\",\"versionSourceId\":\"" + id + "\"}");
        mvc
            .perform(get("/api/v1/referentiels/" + derive + "/etapes").with(ingenieur()))
            .andExpect(jsonPath("$[*].code").value(contains(codes.toArray())))
            .andExpect(jsonPath("$[?(@.code=='REVUE_JURIDIQUE')].couleur").value(contains("INFO")));
        assertThat(jdbc.queryForObject("select count(*) from referential_etape where version_id = ?::uuid", Integer.class, derive)).isEqualTo(11);
    }

    @Test
    @DisplayName("Lecture filtrée : opérations qui concernent le lecteur, voisines en fantômes, liste, fiche, export ; READ_ALL voit tout")
    void lectureFiltree() throws Exception {
        String id = creer(chefDes(), "{\"typeProjet\":\"PAIEMENT_MOBILE\",\"libelle\":\"Paiement mobile filtré\"}");
        String base = "/api/v1/referentiels/" + id;
        mvc.perform(json(post(base + "/operations").with(chefDes()), operation("A", "CADRAGE", "DDRM", "CONDUCTEUR", "", "", "DDRM"))).andExpect(status().isCreated());
        mvc.perform(json(post(base + "/operations").with(chefDes()), operation("B", "LANCEMENT", "POLE_RESEAU", "CONDUCTEUR", "", "\"A\"", "POLE_RESEAU"))).andExpect(status().isCreated());
        mvc.perform(json(post(base + "/operations").with(chefDes()), operation("C", "ATELIERS", "POLE_SYSTEME", "CONDUCTEUR", "\"SNA\"", "\"B\"", "POLE_SYSTEME"))).andExpect(status().isCreated());
        mvc.perform(json(post(base + "/operations").with(chefDes()), operation("D", "INTEGRATION", "POLE_RESEAU", "CONDUCTEUR", "", "\"C\"", "POLE_RESEAU"))).andExpect(status().isCreated());
        mvc.perform(json(post(base + "/operations").with(chefDes()), operation("F", "TESTS", "DG", "CONDUCTEUR", "", "\"D\"", "SNA"))).andExpect(status().isCreated());
        mvc.perform(json(post(base + "/operations").with(chefDes()), operation("G", "CERTIFICATION", "CONFORMITE", "DG", "", "\"F\"", "CONFORMITE"))).andExpect(status().isCreated());
        mvc.perform(json(post(base + "/operations").with(chefDes()), operation("H", "TESTS", "RSSI", "CONDUCTEUR", "", "\"A\"", "RSSI"))).andExpect(status().isCreated());

        // SNA : consulté sur C, déposant d'un livrable de F ; B, D et G en fantômes ; A et H invisibles.
        RequestPostProcessor sna = utilisateur(LECTEUR, "ROLE_SNA", "HOM_REFERENTIEL_READ");
        String graphe = mvc
            .perform(get(base + "/graphe").with(sna))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.filtre").value("CONCERNE"))
            .andExpect(jsonPath("$.noeuds[*].code").value(containsInAnyOrder("B", "C", "D", "F", "G")))
            .andExpect(jsonPath("$.noeuds[?(@.fantome == true)].code").value(containsInAnyOrder("B", "D", "G")))
            .andExpect(jsonPath("$.noeuds[?(@.code=='C')].responsable").value(contains("POLE_SYSTEME")))
            .andExpect(jsonPath("$.arcs.length()").value(4))
            .andExpect(jsonPath("$.etapes.length()").value(11))
            .andReturn()
            .getResponse()
            .getContentAsString();
        List<Map<String, Object>> fantomes = JsonPath.read(graphe, "$.noeuds[?(@.code=='B')]");
        assertThat(fantomes.get(0)).containsEntry("libelle", "Opération B").containsEntry("fantome", true);
        assertThat(fantomes.get(0).get("responsable")).isNull();
        assertThat(fantomes.get(0).get("livrables")).isNull();
        assertThat(fantomes.get(0).get("predecesseurs")).isNull();
        List<String> arcs = JsonPath.read(graphe, "$.arcs[*].de");
        assertThat(arcs).containsExactlyInAnyOrder("B", "C", "D", "F");

        mvc
            .perform(get(base + "/operations").with(sna))
            .andExpect(header().string(ReferentialResource.EN_TETE_FILTRE, "CONCERNE"))
            .andExpect(jsonPath("$.length()").value(5))
            .andExpect(jsonPath("$[?(@.fantome == false)].code").value(containsInAnyOrder("C", "F")));
        mvc.perform(get(base).with(sna)).andExpect(jsonPath("$.filtre").value("CONCERNE")).andExpect(jsonPath("$.nombreOperations").value(2));
        mvc
            .perform(get("/api/v1/referentiels").param("type", "PAIEMENT_MOBILE").with(sna))
            .andExpect(jsonPath("$[?(@.id=='" + id + "')].nombreOperations").value(contains(2)));
        String csv = mvc
            .perform(get(base + "/export").with(sna))
            .andExpect(status().isOk())
            .andExpect(header().string(ReferentialResource.EN_TETE_FILTRE, "CONCERNE"))
            .andReturn()
            .getResponse()
            .getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertThat(csv.lines()).hasSize(6).anyMatch(l -> l.startsWith("B;Opération B;;;;;;;;;;true")).noneMatch(l -> l.startsWith("A;") || l.startsWith("H;"));

        // Sans fonction concernée : rien ; READ_ALL (DG, conformité, RSSI, chefs de département) et éditeurs : tout.
        mvc.perform(get(base + "/graphe").with(utilisateur(LECTEUR, "ROLE_COLLABORATEUR", "HOM_REFERENTIEL_READ"))).andExpect(jsonPath("$.noeuds").isEmpty()).andExpect(jsonPath("$.arcs").isEmpty());
        mvc
            .perform(get(base + "/graphe").with(utilisateur(LECTEUR, "ROLE_DG", "HOM_REFERENTIEL_READ", "HOM_REFERENTIEL_READ_ALL")))
            .andExpect(jsonPath("$.filtre").value("COMPLET"))
            .andExpect(jsonPath("$.noeuds.length()").value(7))
            .andExpect(jsonPath("$.noeuds[?(@.fantome == true)]").isEmpty());
        mvc.perform(get(base + "/operations").with(ingenieur())).andExpect(header().string(ReferentialResource.EN_TETE_FILTRE, "COMPLET")).andExpect(jsonPath("$.length()").value(7));
        String complet = mvc.perform(get(base + "/export").with(ingenieur())).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertThat(complet.lines()).hasSize(8).first().isEqualTo("code;libelle;etape;segment;responsable;validateur;consultes;informes;externes;predecesseurs;delaiJours;fantome");
    }
}
