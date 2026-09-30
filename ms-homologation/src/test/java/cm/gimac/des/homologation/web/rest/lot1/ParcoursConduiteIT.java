package cm.gimac.des.homologation.web.rest.lot1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cm.gimac.des.homologation.domain.enumeration.TypeProjet;
import cm.gimac.des.homologation.infrastructure.client.DocumentStubAdapter;
import cm.gimac.des.homologation.infrastructure.client.ReauthStubAdapter;
import com.jayway.jsonpath.JsonPath;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.kafka.test.utils.KafkaTestUtils;

/**
 * Conduite du parcours sur PostgreSQL et Kafka réels (étapes 3 à 7) : référentiel de l'annexe A instancié
 * à l'ouverture, dossier numérique, livrables revérifiés, ressources, qualités, preuves et validation
 * FIDO2, blocages datés, constat de la couverture publié vers MS-CERTIFICATION, contexte interne.
 */
class ParcoursConduiteIT extends ParcoursITSupport {

    @Autowired
    DocumentStubAdapter documents;

    @Autowired
    org.springframework.transaction.PlatformTransactionManager transactions;

    @Test
    @DisplayName("Cadrage, lancement, ateliers, intégration, essais, constat : chaque route du DES et ses refus")
    void conduite() throws Exception {
        UUID ep = adherent("Établissement de paiement Parcours", "ETABLISSEMENT_PAIEMENT", "RETRAIT", "PAIEMENT");
        String id = ouvrir(ep, TypeProjet.CARTE_INTERBANCAIRE);

        // Référentiel de l'annexe A instancié à l'ouverture : 43 opérations pour la carte, rang 3 à faire.
        assertThat(operations(id)).hasSize(43);
        mvc
            .perform(get("/api/v1/liaisons/" + id + "/dossier").with(conducteur()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.reference").value(Matchers.startsWith("LIA-")))
            .andExpect(jsonPath("$.statut").value("CADREE"))
            .andExpect(jsonPath("$.etapeCourante").value("CADRAGE"))
            .andExpect(jsonPath("$.conducteur.nom").value("Aminata Ngono"))
            .andExpect(jsonPath("$.perimetre[*].serviceCode").value(Matchers.contains("PAIEMENT", "RETRAIT")))
            .andExpect(jsonPath("$.etapes.length()").value(11))
            .andExpect(jsonPath("$.actionsPermises").value(Matchers.hasItems("DECLARER_BLOCAGE")));
        mvc
            .perform(get("/api/v1/liaisons/" + id + "/parcours/operations").param("etape", "CADRAGE").param("fonction", "DDRM").with(conducteur()))
            .andExpect(jsonPath("$[*].rang").value(Matchers.contains("3", "4", "5")))
            .andExpect(jsonPath("$[0].etat").value("A_FAIRE"))
            .andExpect(jsonPath("$[0].actionsPermises").value(Matchers.contains("DEPOSER_PREUVE", "ATTRIBUER")))
            .andExpect(jsonPath("$[0].raci.R.nom").value("Chargé DDRM"));
        mvc.perform(get("/api/v1/liaisons").param("statut", "CADREE").param("etape", "CADRAGE,LANCEMENT").with(conducteur())).andExpect(jsonPath("$.contenu[*].id").value(Matchers.hasItem(id)));
        mvc.perform(get("/api/v1/liaisons").param("statut", "EN_TEST").with(conducteur())).andExpect(jsonPath("$.contenu[*].id").value(Matchers.not(Matchers.hasItem(id))));

        // Livrable : pièce revérifiée auprès de MS-DOCUMENT (empreinte divergente, pièce inconnue).
        String o3 = opId(id, "3");
        UUID doc = UUID.randomUUID();
        documents.enregistrer(doc, "b".repeat(64));
        String divergente = "{\"documentId\":\"" + doc + "\",\"empreinte\":\"" + SHA + "\",\"nom\":\"perimetre.pdf\"}";
        mvc
            .perform(post("/api/v1/parcours/operations/" + o3 + "/livrable").with(ddrm()).contentType(MediaType.APPLICATION_JSON).content(divergente))
            .andExpect(status().is(422))
            .andExpect(jsonPath("$.code").value("HOM_DOCUMENT_HASH_MISMATCH"));
        UUID absente = UUID.randomUUID();
        documents.retirer(absente);
        mvc
            .perform(post("/api/v1/parcours/operations/" + o3 + "/livrable").with(ddrm()).contentType(MediaType.APPLICATION_JSON).content("{\"documentId\":\"" + absente + "\",\"empreinte\":\"" + SHA + "\",\"nom\":\"x.pdf\"}"))
            .andExpect(status().is(422))
            .andExpect(jsonPath("$.code").value("HOM_DOCUMENT_UNKNOWN"));
        livrable(o3, ddrm());
        assertThat(etat(id, "3")).isEqualTo("FAITE");
        mvc
            .perform(post("/api/v1/parcours/operations/" + o3 + "/livrable").with(ddrm()).contentType(MediaType.APPLICATION_JSON).content(piece("x.pdf")))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("HOM_OPERATION_NOT_OPEN"));
        mvc.perform(post("/api/v1/parcours/operations/" + UUID.randomUUID() + "/livrable").with(ddrm()).contentType(MediaType.APPLICATION_JSON).content(piece("x.pdf"))).andExpect(status().isNotFound());

        // Ressources (rang 6) : au moins une, personne de l'annuaire détenant le profil de la fonction.
        mvc
            .perform(put("/api/v1/liaisons/" + id + "/ressources").with(chefDes()).contentType(MediaType.APPLICATION_JSON).content("{\"affectations\":[]}"))
            .andExpect(status().is(422))
            .andExpect(jsonPath("$.code").value("HOM_RESOURCES_REQUIRED"));
        mvc
            .perform(put("/api/v1/liaisons/" + id + "/ressources").with(chefDes()).contentType(MediaType.APPLICATION_JSON).content("{\"affectations\":[{\"fonction\":\"RSSI\",\"personneId\":\"" + SNA + "\"}]}"))
            .andExpect(status().is(422))
            .andExpect(jsonPath("$.code").value("HOM_PERSON_NOT_ELIGIBLE"));
        avancer(id, "7", ep);
        assertThat(etat(id, "6")).isEqualTo("FAITE");
        mvc
            .perform(put("/api/v1/liaisons/" + id + "/ressources").with(chefDes()).contentType(MediaType.APPLICATION_JSON).content("{\"affectations\":[{\"fonction\":\"SNA\",\"personneId\":\"" + SNA + "\"}]}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("HOM_STEP_NOT_OPEN"));

        // Qualités : le validateur n'est jamais le responsable ; personne inconnue refusée.
        String o7 = opId(id, "7");
        String r7 = JsonPath.read(lire("/api/v1/liaisons/" + id + "/parcours/operations?etape=LANCEMENT", conducteur()), "$[1].raci.R.id");
        mvc
            .perform(put("/api/v1/parcours/operations/" + o7 + "/intervenants").with(conducteur()).contentType(MediaType.APPLICATION_JSON).content("{\"qualite\":\"R\",\"personneId\":\"" + SNA + "\",\"motif\":\"relève\"}"))
            .andExpect(status().isNoContent());
        mvc
            .perform(put("/api/v1/parcours/operations/" + o7 + "/intervenants").with(conducteur()).contentType(MediaType.APPLICATION_JSON).content("{\"qualite\":\"V\",\"personneId\":\"" + SNA + "\",\"motif\":\"x\"}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("HOM_VALIDATOR_IS_RESPONSIBLE"));
        mvc
            .perform(put("/api/v1/parcours/operations/" + o7 + "/intervenants").with(conducteur()).contentType(MediaType.APPLICATION_JSON).content("{\"qualite\":\"I\",\"personneId\":\"inconnu\",\"motif\":\"x\"}"))
            .andExpect(status().is(422))
            .andExpect(jsonPath("$.code").value("HOM_PERSON_UNKNOWN"));
        assertThat(r7).isNotEqualTo(SNA.toString());
        assertThat((String) JsonPath.read(lire("/api/v1/liaisons/" + id + "/parcours/operations?etape=LANCEMENT", conducteur()), "$[1].raci.R.id")).isEqualTo(SNA.toString());

        // Preuve au grain de l'opération, validation après réauthentification forte, jamais par R.
        avancer(id, "13", ep);
        String o13 = opId(id, "13");
        preuve(o13, "");
        assertThat(etat(id, "13")).isEqualTo("EN_VALIDATION");
        mvc
            .perform(post("/api/v1/parcours/operations/" + o13 + "/validation").with(validateur()))
            .andExpect(status().is(428))
            .andExpect(jsonPath("$.code").value("IAM_REAUTH_REQUIRED"));
        mvc
            .perform(post("/api/v1/parcours/operations/" + o13 + "/validation").header("X-Reauth-Assertion", ReauthStubAdapter.ASSERTION_REFUSEE).with(validateur()))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("HOM_REAUTH_INVALID"));
        valider(o13);
        mvc
            .perform(post("/api/v1/parcours/operations/" + o13 + "/validation").header("X-Reauth-Assertion", "a").with(validateur()))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("HOM_OPERATION_NOT_IN_VALIDATION"));
        mvc
            .perform(post("/api/v1/parcours/operations/" + opId(id, "14") + "/preuves").with(conducteur()).contentType(MediaType.APPLICATION_JSON).content("{\"documentId\":\"" + UUID.randomUUID() + "\",\"empreinte\":\"" + SHA + "\",\"nom\":\"k.pdf\",\"cleEnClair\":\"0011\"}"))
            .andExpect(status().is(422))
            .andExpect(jsonPath("$.code").value("HOM_EVIDENCE_FIELD_REFUSED"));

        // Blocage daté : les opérations ouvertes sont bloquées, puis rouvertes à la levée.
        String blocage = "{\"motif\":\"Élément de sécurité à renouveler\",\"prochaineAction\":\"Obtenir le nouvel élément\",\"responsableId\":\"" + SNA + "\",\"echeanceLevee\":\"2026-12-31\"}";
        String b = mvc
            .perform(post("/api/v1/liaisons/" + id + "/blocages").with(conducteur()).contentType(MediaType.APPLICATION_JSON).content(blocage))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", Matchers.startsWith("/api/v1/blocages/")))
            .andExpect(jsonPath("$.etape").value("INTEGRATION"))
            .andExpect(jsonPath("$.responsable.id").value(SNA.toString()))
            .andExpect(jsonPath("$.actionsPermises[0]").value("LEVER"))
            .andReturn()
            .getResponse()
            .getContentAsString();
        String blocageId = JsonPath.read(b, "$.id");
        assertThat(etat(id, "14")).isEqualTo("BLOQUEE");
        mvc.perform(get("/api/v1/liaisons").param("bloquees", "true").with(conducteur())).andExpect(jsonPath("$.contenu[*].id").value(Matchers.hasItem(id)));
        mvc.perform(get("/api/v1/liaisons/" + id + "/dossier").with(conducteur())).andExpect(jsonPath("$.blocages[0].leveLe").isEmpty());
        mvc.perform(post("/api/v1/blocages/" + blocageId + "/levee").with(conducteur()).contentType(MediaType.APPLICATION_JSON).content("{\"commentaire\":\"reçu\"}")).andExpect(status().isNoContent());
        mvc
            .perform(post("/api/v1/blocages/" + blocageId + "/levee").with(conducteur()).contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("HOM_BLOCKAGE_ALREADY_LIFTED"));
        mvc.perform(post("/api/v1/blocages/" + UUID.randomUUID() + "/levee").with(conducteur())).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("HOM_BLOCKAGE_NOT_FOUND"));
        assertThat(etat(id, "14")).isEqualTo("A_FAIRE");

        // Constat de la couverture (rang 17) : refusé avant la fin des essais, puis publié vers MS-CERTIFICATION.
        mvc.perform(post("/api/v1/liaisons/" + id + "/couverture/constat").with(sna())).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("HOM_COVERAGE_NOT_READY"));
        avancer(id, "17", ep);
        mvc.perform(get("/api/v1/liaisons/" + id + "/dossier").with(sna())).andExpect(jsonPath("$.statut").value("EN_TEST")).andExpect(jsonPath("$.actionsPermises").value(Matchers.hasItem("CONSTATER_COUVERTURE")));
        mvc.perform(get("/api/v1/interne/liaisons/" + id + "/contexte-certification").with(service())).andExpect(jsonPath("$.couvertureConstatee").value(false));
        mvc.perform(post("/api/v1/liaisons/" + id + "/couverture/constat").with(sna())).andExpect(status().isNoContent());
        mvc
            .perform(get("/api/v1/interne/liaisons/" + id + "/contexte-certification").with(service()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.couvertureConstatee").value(true))
            .andExpect(jsonPath("$.couvertureCertification.taux").value(100))
            .andExpect(jsonPath("$.servicesCouverts").value(Matchers.contains("PAIEMENT", "RETRAIT")))
            .andExpect(jsonPath("$.institutionId").value(ep.toString()))
            .andExpect(jsonPath("$.conducteurNom").value("Aminata Ngono"));
        assertThat(etat(id, "18")).isEqualTo("A_FAIRE");
        mvc.perform(get("/api/v1/liaisons/" + id).with(conducteur())).andExpect(jsonPath("$.etat").value("APTE_CERTIFICATION")).andExpect(jsonPath("$.statut").value("EN_TEST"));
        relayerOutboxJusquaEnvoi(id);
        try (var c = consommateur("homologation.liaison")) {
            var recus = KafkaTestUtils.getRecords(c, Duration.ofSeconds(20)).records("homologation.liaison");
            assertThat(recus).anySatisfy(r -> {
                assertThat(r.key()).isEqualTo(id);
                assertThat(JsonPath.<String>read(r.value(), "$.type")).isEqualTo("homologation.coverage-established.v1");
                assertThat(JsonPath.<Integer>read(r.value(), "$.data.taux")).isEqualTo(100);
                assertThat(JsonPath.<List<String>>read(r.value(), "$.data.servicesCouverts")).containsExactly("PAIEMENT", "RETRAIT");
                assertThat(JsonPath.<String>read(r.value(), "$.institutionid")).isEqualTo(ep.toString());
            });
        }
        mvc.perform(get("/api/v1/interne/liaisons/" + UUID.randomUUID() + "/contexte-certification").with(service())).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/liaisons/" + UUID.randomUUID() + "/dossier").with(conducteur())).andExpect(jsonPath("$.code").value("HOM_LIAISON_NOT_FOUND"));
    }

    @Test
    @DisplayName("Couverture mesurée par le simulateur : 75 % constatés quand un service n'est pas certifié")
    void couvertureIncomplete() throws Exception {
        UUID ep = adherent("Banque Couverture", "BANQUE", "RETRAIT", "PAIEMENT", "TRANSFERT", "COMMERCE_ELECTRONIQUE");
        String id = ouvrir(ep, TypeProjet.CARTE_INTERBANCAIRE);
        // Taux repris du simulateur (HOM-12), écrits et validés comme par la reprise (la réserve n'est pas en validation automatique).
        var tx = new org.springframework.transaction.support.TransactionTemplate(transactions);
        for (String[] c : new String[][] { { "RETRAIT", "100" }, { "PAIEMENT", "100" }, { "TRANSFERT", "100" }, { "COMMERCE_ELECTRONIQUE", "60" } }) {
            tx.executeWithoutResult(s -> jdbc.update(
                "insert into coverage (id, service_code, nature, numerateur, denominateur, taux, calculable, date_calcul, liaison_id) values (?, ?, 'CERTIFICATION', 1, 1, ?, true, now(), ?::uuid)",
                UUID.randomUUID(),
                c[0],
                new java.math.BigDecimal(c[1]),
                id
            ));
        }
        avancer(id, "17", ep);
        mvc
            .perform(get("/api/v1/liaisons/" + id + "/dossier").with(conducteur()))
            .andExpect(jsonPath("$.couvertureCertification.taux").value(75))
            .andExpect(jsonPath("$.perimetre[?(@.serviceCode == 'COMMERCE_ELECTRONIQUE')].etat").value("EN_TEST"));
        mvc.perform(post("/api/v1/liaisons/" + id + "/couverture/constat").with(sna())).andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/interne/liaisons/" + id + "/contexte-certification").with(service())).andExpect(jsonPath("$.couvertureCertification.taux").value(75));
    }
}
