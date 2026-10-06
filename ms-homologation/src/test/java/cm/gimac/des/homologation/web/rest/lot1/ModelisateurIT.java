package cm.gimac.des.homologation.web.rest.lot1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cm.gimac.des.homologation.AbstractHomologationIT;
import cm.gimac.des.homologation.domain.cadrage.CollaboratorView;
import cm.gimac.des.homologation.domain.enumeration.TypeProjet;
import cm.gimac.des.homologation.infrastructure.client.CollaboratorStubAdapter;
import cm.gimac.des.homologation.infrastructure.client.PilotageTachesStubAdapter;
import cm.gimac.des.homologation.infrastructure.messaging.taches.RelaisTachesPilotage;
import com.jayway.jsonpath.JsonPath;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** Modélisateur de processus (D-77, docs/lot1/modelisateur-processus.md §4 et §5) par l'API. */
class ModelisateurIT extends AbstractHomologationIT {

    static final UUID INGENIEUR = UUID.fromString("00000000-0000-4000-8000-0000000e0001");
    static final UUID INGENIEUR_2 = UUID.fromString("00000000-0000-4000-8000-0000000e0002");
    static final UUID SNA = UUID.fromString("00000000-0000-4000-8000-0000000e0003");

    @Autowired
    CollaboratorStubAdapter annuaire;

    @Autowired
    PilotageTachesStubAdapter pilotage;

    @Autowired
    RelaisTachesPilotage relais;

    @BeforeEach
    void init() {
        pilotage.vider();
        annuaire.enregistrer(new CollaboratorView(INGENIEUR_2, "Ingénieure Homologation", Set.of("ROLE_INGENIEUR_HOMOLOGATION"), null, true));
        annuaire.enregistrer(new CollaboratorView(SNA, "Ingénieur SNA", Set.of("ROLE_SNA"), null, true));
    }

    static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder r, String corps) {
        return r.contentType(MediaType.APPLICATION_JSON).content(corps);
    }

    static String operation(String code, String etape, String segment, String r, String v, int delai, String preds, String extra) {
        return (
            "{" +
            (code == null ? "" : "\"code\":\"" + code + "\",") +
            "\"libelle\":\"Opération " + code + "\",\"description\":\"Actions attendues\",\"etape\":\"" + etape + "\",\"segment\":\"" + segment + "\"," +
            "\"ordre\":1,\"responsable\":\"" + r + "\",\"validateur\":\"" + v + "\",\"consultes\":[\"SNA\"],\"informes\":[\"CHEF_DDRM\"],\"externes\":[\"PARTICIPANT\"]," +
            "\"predecesseurs\":[" + preds + "],\"delaiJours\":" + delai + ",\"grain\":\"OPERATION\",\"cle\":false,\"secret\":false,\"oubliSansSymptome\":false,\"pivot\":false," +
            "\"livrables\":[{\"code\":\"L1\",\"libelle\":\"Compte rendu\",\"nature\":\"LIVRABLE\",\"deposant\":\"PARTICIPANT\",\"typesPieces\":[\"application/pdf\"],\"obligatoire\":true}]," +
            "\"geste\":\"DEPOSER_LIVRABLE\"" + extra + "}"
        );
    }

    String brouillon(String corps) throws Exception {
        String r = mvc
            .perform(json(post("/api/v1/referentiels").with(utilisateur(INGENIEUR, "HOM_REFERENTIEL_EDIT")), corps))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
        return JsonPath.read(r, "$.id");
    }

    @Test
    @DisplayName("Opérations : création au code fourni, écriture complète, graphe étendu (chemin critique), positions, arcs, retrait, publication")
    void modeliserPuisPublier() throws Exception {
        String id = brouillon("{\"typeProjet\":\"DELEGATAIRE\",\"libelle\":\"Délégataire v2\"}");
        String base = "/api/v1/referentiels/" + id;
        var edition = utilisateur(INGENIEUR, "HOM_REFERENTIEL_EDIT");
        mvc
            .perform(json(post(base + "/operations").with(edition), operation("A", "CADRAGE", "AMONT", "DDRM", "CONDUCTEUR", 3, "", ",\"position\":{\"x\":10,\"y\":20}")))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.code").value("A"))
            .andExpect(jsonPath("$.segment").value("AMONT"))
            .andExpect(jsonPath("$.etape").value("CADRAGE"))
            .andExpect(jsonPath("$.phase").value("CADRAGE"))
            .andExpect(jsonPath("$.partie").value("DES"))
            .andExpect(jsonPath("$.livrables[0].deposant").value("PARTICIPANT"))
            .andExpect(jsonPath("$.position.x").value(10.0));
        mvc.perform(json(post(base + "/operations").with(edition), operation("A", "CADRAGE", "AMONT", "DDRM", "CONDUCTEUR", 3, "", ""))).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("HOM_OPERATION_CODE_DUPLICATE"));
        mvc.perform(json(post(base + "/operations").with(edition), operation("b", "CADRAGE", "AMONT", "DDRM", "CONDUCTEUR", 3, "", ""))).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("HOM_OPERATION_CODE_INVALID"));
        mvc.perform(json(post(base + "/operations").with(edition), operation("B", "LANCEMENT", "AMONT", "SNA", "CONDUCTEUR", 5, "\"A\"", ""))).andExpect(status().isCreated());
        mvc.perform(json(post(base + "/operations").with(edition), operation("C", "LANCEMENT", "AMONT", "SNA", "CONDUCTEUR", 1, "\"A\"", ""))).andExpect(status().isCreated());
        // Écriture complète (PUT) : jalon, déclencheur, R égal à V (anomalie signalée au contrôle, pas à l'écriture).
        mvc
            .perform(json(put(base + "/operations/D").with(edition), operation(null, "MISE_EN_PRODUCTION", "BRANCHE", "RSSI", "RSSI", 2, "\"B\",\"C\"", ",\"jalon\":\"EN_PRODUCTION\"")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.segment").value("BRANCHE"))
            .andExpect(jsonPath("$.jalon").value("EN_PRODUCTION"));
        mvc.perform(json(put(base + "/operations/E").with(edition), "{\"secret\":true,\"valeurReference\":\"Code\"}")).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("HOM_OPERATION_SECRET_VALUE"));

        mvc
            .perform(get(base + "/graphe").with(utilisateur(INGENIEUR, "HOM_REFERENTIEL_READ", "HOM_REFERENTIEL_READ_ALL")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.versionId").value(id))
            .andExpect(jsonPath("$.etat").value("BROUILLON"))
            .andExpect(jsonPath("$.piloteId").value(INGENIEUR.toString()))
            .andExpect(jsonPath("$.noeuds.length()").value(4))
            .andExpect(jsonPath("$.noeuds[?(@.code=='A')].successeurs[*]").value(contains("B", "C")))
            .andExpect(jsonPath("$.noeuds[?(@.code=='D')].finAuPlusTot").value(contains(10)))
            .andExpect(jsonPath("$.noeuds[?(@.code=='D')].couche").value(contains(2)))
            .andExpect(jsonPath("$.noeuds[?(@.code=='D')].anomalies[*]").value(contains("VALIDATEUR_EGAL_RESPONSABLE")))
            .andExpect(jsonPath("$.noeuds[?(@.code=='C')].critique").value(contains(false)))
            .andExpect(jsonPath("$.noeuds[?(@.code=='A')].position.y").value(contains(20.0)))
            .andExpect(jsonPath("$.cheminCritique.codes").value(contains("A", "B", "D")))
            .andExpect(jsonPath("$.cheminCritique.dureeJours").value(10))
            .andExpect(jsonPath("$.arcs[?(@.de=='B' && @.vers=='D')].critique").value(contains(true)))
            .andExpect(jsonPath("$.anomalies[0].type").value("VALIDATEUR_EGAL_RESPONSABLE"))
            .andExpect(jsonPath("$.manquants").isEmpty());

        mvc.perform(json(put(base + "/operations/D").with(edition), operation(null, "MISE_EN_PRODUCTION", "BRANCHE", "RSSI", "CONDUCTEUR", 2, "\"B\",\"C\"", ",\"jalon\":\"EN_PRODUCTION\""))).andExpect(status().isOk());
        mvc.perform(json(put(base + "/positions").with(edition), "[{\"code\":\"B\",\"x\":300,\"y\":40}]")).andExpect(status().isNoContent());
        mvc
            .perform(json(put(base + "/arcs").with(edition), "{\"de\":\"D\",\"vers\":\"A\",\"present\":true}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("HOM_REFERENTIAL_CYCLE"));
        mvc
            .perform(json(put(base + "/arcs").with(edition), "{\"de\":\"B\",\"vers\":\"C\",\"present\":true}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("C"))
            .andExpect(jsonPath("$.predecesseurs").value(contains("A", "B")));
        mvc.perform(delete(base + "/operations/B").with(edition)).andExpect(status().isNoContent());
        mvc.perform(delete(base + "/operations/B").with(edition)).andExpect(status().isNotFound());
        mvc
            .perform(get(base + "/operations").with(utilisateur(INGENIEUR, "HOM_REFERENTIEL_READ", "HOM_REFERENTIEL_READ_ALL")))
            .andExpect(jsonPath("$[?(@.code=='C')].predecesseurs[*]").value(contains("A")))
            .andExpect(jsonPath("$[?(@.code=='D')].predecesseurs[*]").value(contains("C")));

        mvc.perform(post(base + "/publication").with(utilisateur(INGENIEUR, "HOM_REFERENTIEL_PUBLISH"))).andExpect(status().isOk()).andExpect(jsonPath("$.etat").value("PUBLIEE"));
        // Version figée : toute écriture du modélisateur est refusée.
        mvc.perform(json(post(base + "/operations").with(edition), operation("Z", "CADRAGE", "AMONT", "DDRM", "CONDUCTEUR", 3, "", ""))).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("HOM_REFERENTIAL_FROZEN"));
        mvc.perform(delete(base + "/operations/A").with(edition)).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("HOM_REFERENTIAL_FROZEN"));
        mvc.perform(json(put(base + "/positions").with(edition), "[{\"code\":\"A\",\"x\":1,\"y\":1}]")).andExpect(status().isConflict());
        mvc.perform(json(put(base + "/arcs").with(edition), "{\"de\":\"A\",\"vers\":\"D\",\"present\":true}")).andExpect(status().isConflict());
        mvc.perform(json(put(base + "/pilote").with(edition), "{\"compteId\":\"" + INGENIEUR_2 + "\"}")).andExpect(status().isConflict());

        // Tâche du pilote : créée avec le brouillon, terminée à la publication.
        relais.relayer();
        assertThat(pilotage.journal()).containsExactly("CREER referentiel/" + id + "|" + INGENIEUR, "CLOTURER referentiel/" + id + "|" + INGENIEUR);
        assertThat(pilotage.taches().get("referentiel/" + id + "|" + INGENIEUR).tache().titre()).isEqualTo("Compléter et publier le référentiel Délégataire v1");
        assertThat(pilotage.taches().get("referentiel/" + id + "|" + INGENIEUR).etat()).isEqualTo("TERMINEE");
    }

    @Test
    @DisplayName("Pilote : désigné à la création ou ensuite (profil d'édition exigé), la tâche suit ; catalogue")
    void piloteEtCatalogue() throws Exception {
        mvc
            .perform(json(post("/api/v1/referentiels").with(utilisateur(INGENIEUR, "HOM_REFERENTIEL_EDIT")), "{\"typeProjet\":\"CARTE_INTERBANCAIRE\",\"libelle\":\"Carte v2\",\"piloteId\":\"" + SNA + "\"}"))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("HOM_PILOTE_NOT_ELIGIBLE"));
        String id = brouillon("{\"typeProjet\":\"CARTE_INTERBANCAIRE\",\"libelle\":\"Carte v2\",\"piloteId\":\"" + INGENIEUR_2 + "\"}");
        mvc.perform(get("/api/v1/referentiels/" + id).with(utilisateur(INGENIEUR, "HOM_REFERENTIEL_READ"))).andExpect(jsonPath("$.piloteId").value(INGENIEUR_2.toString()));
        mvc
            .perform(json(put("/api/v1/referentiels/" + id + "/pilote").with(utilisateur(INGENIEUR, "HOM_REFERENTIEL_EDIT")), "{\"compteId\":\"" + INGENIEUR + "\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.piloteId").value(INGENIEUR.toString()));
        mvc
            .perform(json(put("/api/v1/referentiels/" + id + "/pilote").with(utilisateur(INGENIEUR, "HOM_REFERENTIEL_EDIT")), "{\"compteId\":\"" + SNA + "\"}"))
            .andExpect(status().isUnprocessableEntity());
        mvc.perform(json(put("/api/v1/referentiels/" + id + "/pilote").with(utilisateur(INGENIEUR, "HOM_REFERENTIEL_EDIT")), "{}")).andExpect(status().isBadRequest());
        relais.relayer();
        assertThat(pilotage.journal()).containsExactly(
            "CREER referentiel/" + id + "|" + INGENIEUR_2,
            "CLOTURER referentiel/" + id + "|" + INGENIEUR_2,
            "CREER referentiel/" + id + "|" + INGENIEUR
        );
        assertThat(jdbc.queryForObject("select count(*) from demande_tache_pilotage where statut = 'ENVOYEE'", Integer.class)).isEqualTo(3);

        mvc
            .perform(get("/api/v1/referentiels/catalogue").with(utilisateur(INGENIEUR, "HOM_REFERENTIEL_READ")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.fonctions[?(@.code=='SNA')].libelle").value(contains("Normes et Agréments")))
            .andExpect(jsonPath("$.fonctions[?(@.code=='SNA')].profils[*]").value(contains("ROLE_SNA", "ROLE_INGENIEUR_HOMOLOGATION")))
            .andExpect(jsonPath("$.fonctions[?(@.code=='PARTICIPANT')].externe").value(contains(true)))
            .andExpect(jsonPath("$.gestes[?(@.code=='EVENEMENT_EXTERNE')].dedie").value(contains(true)))
            .andExpect(jsonPath("$.jalons[*].code").value(contains("APTE_CERTIFICATION", "CERTIFIEE", "APPROUVEE_SECURITE", "EN_PRODUCTION")))
            .andExpect(jsonPath("$.declencheurs[*].code").value(hasItem("certification.pv-signed.v1")))
            .andExpect(jsonPath("$.etapes[?(@.code=='TESTS')].rang").value(contains(7)))
            .andExpect(jsonPath("$.segments[*].code").value(contains("AMONT", "BRANCHE", "AVAL")))
            .andExpect(jsonPath("$.plateformes.length()").value(4))
            .andExpect(jsonPath("$.particularites[*].code").value(hasItem("CEREMONIE_CLES")))
            .andExpect(jsonPath("$.typesPieces[0]").value("application/pdf"))
            .andExpect(jsonPath("$.pilotes[*].id").value(hasItem(INGENIEUR_2.toString())));
        mvc.perform(get("/api/v1/referentiels/catalogue").with(utilisateur(INGENIEUR, "HOM_REFERENTIEL_EDIT"))).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/referentiels/" + id + "/graphe").with(utilisateur(INGENIEUR, "HOM_REFERENTIEL_EDIT"))).andExpect(status().isForbidden());
        mvc.perform(delete("/api/v1/referentiels/" + id + "/operations/A").with(utilisateur(INGENIEUR, "HOM_REFERENTIEL_READ"))).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Version 1 de l'annexe A : graphe étendu conforme, raccord du rang 27 à la fin de la branche")
    void grapheDeLaVersion1() throws Exception {
        UUID v1 = fixtures.referentielPublie(TypeProjet.PAIEMENT_MOBILE);
        mvc
            .perform(get("/api/v1/referentiels/" + v1 + "/graphe").with(utilisateur(INGENIEUR, "HOM_REFERENTIEL_READ", "HOM_REFERENTIEL_READ_ALL")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.noeuds.length()").value(49))
            .andExpect(jsonPath("$.anomalies").isEmpty())
            .andExpect(jsonPath("$.noeuds[0].code").value("3"))
            .andExpect(jsonPath("$.noeuds[?(@.code=='27')].predecesseurs[*]").value(contains("M17")))
            .andExpect(jsonPath("$.noeuds[?(@.code=='12')].geste").value(contains("COORDONNEES_TECHNIQUES")))
            .andExpect(jsonPath("$.cheminCritique.codes[0]").value("3"))
            .andExpect(jsonPath("$.actionsPermises").value(contains("DERIVER", "MIGRER")));
    }
}
