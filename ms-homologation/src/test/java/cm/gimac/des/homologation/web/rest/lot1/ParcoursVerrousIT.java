package cm.gimac.des.homologation.web.rest.lot1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cm.gimac.des.homologation.domain.enumeration.TypeProjet;
import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;

/**
 * Verrous et mise en production sur PostgreSQL et Kafka réels : événements de MS-CERTIFICATION consommés
 * de façon idempotente (rangs 18 à 26, CERTIFIEE, APPROUVEE_SECURITE), branche carte avec cérémonie de
 * clés, tronc aval, fiche de suivi et constat de mise en service, service supplémentaire (rang 35).
 */
class ParcoursVerrousIT extends ParcoursITSupport {

    @Autowired
    KafkaTemplate<String, String> kafka;

    String statut(String id) throws Exception {
        return JsonPath.read(lire("/api/v1/liaisons/" + id + "/dossier", conducteur()), "$.statut");
    }

    @Test
    @DisplayName("Procès-verbal établi et signé (Kafka), dossier et attestations, approbation ; idempotence")
    void verrous() throws Exception {
        UUID ep = adherent("Banque Verrous", "BANQUE", "RETRAIT");
        String id = ouvrir(ep, TypeProjet.CARTE_INTERBANCAIRE);
        avancer(id, "18", ep);

        // Événements publiés sur certification.proces-verbal, consommés par le groupe homologation-certification.
        String etabli = "{\"specversion\":\"1.0\",\"id\":\"" + UUID.randomUUID() + "\",\"type\":\"certification.pv-established.v1\",\"data\":{\"liaisonId\":\"" + id + "\",\"etabliPar\":\"Conducteur\",\"empreinte\":\"" + SHA + "\"}}";
        String signe = "{\"specversion\":\"1.0\",\"id\":\"" + UUID.randomUUID() + "\",\"type\":\"certification.pv-signed.v1\",\"data\":{\"liaisonId\":\"" + id + "\",\"signatures\":[{\"fonction\":\"CHEF_DES\",\"signataire\":\"Chef DES\",\"signeLe\":\"2026-10-06\"},{\"fonction\":\"PARTICIPANT\",\"signataire\":\"Représentant\",\"signeLe\":\"2026-10-06\"}],\"empreinte\":\"" + SHA + "\"}}";
        kafka.send("certification.proces-verbal", id, etabli).get(10, TimeUnit.SECONDS);
        kafka.send("certification.proces-verbal", id, signe).get(10, TimeUnit.SECONDS);
        kafka.send("certification.proces-verbal", id, signe).get(10, TimeUnit.SECONDS);
        for (int i = 0; i < 60 && !"CERTIFIEE".equals(statut(id)); i++) {
            Thread.sleep(500);
        }
        assertThat(statut(id)).isEqualTo("CERTIFIEE");
        assertThat(etat(id, "18")).isEqualTo("FAITE");
        assertThat(etat(id, "19")).isEqualTo("FAITE");
        assertThat(jdbc.queryForObject("select count(*) from processed_event where consumer_group = 'homologation-certification'", Integer.class)).isGreaterThanOrEqualTo(2);
        mvc.perform(get("/api/v1/liaisons/" + id + "/dossier").with(conducteur())).andExpect(jsonPath("$.etapeCourante").value("APPROBATION_SECURITE"));
        mvc.perform(get("/api/v1/liaisons/" + id).with(conducteur())).andExpect(jsonPath("$.etat").value("CERTIFIEE"));

        // Tâches du participant : attestations attendues une fois le procès-verbal signé des deux parties.
        mvc
            .perform(get("/api/v1/portail/taches").with(correspondant(ep)))
            .andExpect(jsonPath("$.compteurs[?(@.cle == 'attestationsAFournir')].valeur").value(1));

        // Verrou 2 : dossier de changement, fiche référencée avant l'ouverture du rang 26, attestations, approbation.
        evenement("certification.change-record-constituted.v1", id, "\"documentRef\":\"CHG-DOS-1\",\"nom\":\"dossier.pdf\",\"empreinte\":\"" + SHA + "\"");
        evenement("certification.change-record-referenced.v1", id, "\"reference\":\"CHG-2026-0412\"");
        evenement("certification.attestation-submitted.v1", id, "\"type\":\"PCI_DSS\"");
        mvc.perform(get("/api/v1/tableaux-de-bord/acteur").param("fonctions", "RSSI").with(utilisateur(UUID.randomUUID(), "ROLE_RSSI", "HOM_DASHBOARD_READ")))
            .andExpect(jsonPath("$.compteurs[?(@.cle == 'attestationsAVerifier')].valeur").value(Matchers.hasItem(Matchers.greaterThanOrEqualTo(1))));
        evenement("certification.attestations-archived.v1", id, "\"attestations\":[{\"type\":\"PCI_DSS\",\"documentRef\":\"ATT-PCI\"}]");
        assertThat(etat(id, "21")).isEqualTo("FAITE");
        assertThat(etat(id, "26")).isEqualTo("VERROUILLEE");
        evenement("certification.security-approved.v1", id, "\"approuveeLe\":\"2026-10-06\"");
        assertThat(statut(id)).isEqualTo("APPROUVEE_SECURITE");
        assertThat(etat(id, "25")).isEqualTo("FAITE");
        assertThat(etat(id, "26")).isEqualTo("A_FAIRE");
        evenement("certification.change-record-referenced.v1", id, "\"reference\":\"CHG-2026-0412\"");
        assertThat(etat(id, "26")).isEqualTo("FAITE");
        mvc.perform(get("/api/v1/liaisons/" + id + "/dossier").with(conducteur())).andExpect(jsonPath("$.etapeCourante").value("MISE_EN_PRODUCTION"));
        evenement("certification.pv-established.v1", UUID.randomUUID().toString(), "");
    }

    @Test
    @DisplayName("Mise en production : cérémonie C1, valeurs de référence, secret en trace ; fiche de suivi ; service supplémentaire")
    void miseEnProduction() throws Exception {
        UUID ep = adherent("Banque Production", "BANQUE", "RETRAIT", "PAIEMENT");
        String id = ouvrir(ep, TypeProjet.CARTE_INTERBANCAIRE);
        avancer(id, "C1", ep);

        // C1 : la cérémonie complète est exigée ; les codes de contrôle seuls sont conservés.
        String c1 = opId(id, "C1");
        mvc
            .perform(post("/api/v1/parcours/operations/" + c1 + "/preuves").with(conducteur()).contentType(MediaType.APPLICATION_JSON).content("{\"documentId\":\"" + UUID.randomUUID() + "\",\"empreinte\":\"" + SHA + "\",\"nom\":\"pv.pdf\"}"))
            .andExpect(status().is(422))
            .andExpect(jsonPath("$.code").value("HOM_KEY_CEREMONY_INCOMPLETE"));
        avancer(id, "C2", ep);
        mvc.perform(get("/api/v1/liaisons/" + id + "/parcours/operations").param("etape", "MISE_EN_PRODUCTION").with(conducteur()))
            .andExpect(jsonPath("$[0].preuve.ceremonie.codesControle.length()").value(3))
            .andExpect(jsonPath("$[0].preuve.valeurReference").doesNotExist());
        String c2 = opId(id, "C2");
        mvc
            .perform(post("/api/v1/parcours/operations/" + c2 + "/preuves").with(conducteur()).contentType(MediaType.APPLICATION_JSON).content("{\"documentId\":\"" + UUID.randomUUID() + "\",\"empreinte\":\"" + SHA + "\",\"nom\":\"c2.png\"}"))
            .andExpect(status().is(422))
            .andExpect(jsonPath("$.code").value("HOM_REFERENCE_VALUE_REQUIRED"));
        avancer(id, "29", ep);
        String o29 = opId(id, "29");
        preuve(o29, "\"codeControle\":\"A1B2C3\"");
        valider(o29);
        mvc.perform(get("/api/v1/liaisons/" + id + "/parcours/operations").param("fonction", "POLE_SYSTEME").with(conducteur()))
            .andExpect(jsonPath("$[?(@.rang == '29')].preuve.codeControle").isEmpty())
            .andExpect(jsonPath("$[?(@.rang == '28')].oubliSansSymptome").value(true));
        avancer(id, "32", ep);
        assertThat(statut(id)).isEqualTo("EN_PRODUCTION");
        mvc.perform(get("/api/v1/liaisons/" + id).with(conducteur())).andExpect(jsonPath("$.etat").value("CLOSE"));

        // Fiche de suivi : constat refusé avant le renseignement, remise, renseignement par le participant, constat.
        mvc.perform(post("/api/v1/liaisons/" + id + "/fiche-suivi/constat").with(sna())).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("HOM_SERVICE_SHEET_INCOMPLETE"));
        mvc.perform(get("/api/v1/liaisons/" + id + "/fiche-suivi").with(conducteur())).andExpect(jsonPath("$.etat").value("A_REMETTRE")).andExpect(jsonPath("$.actionsPermises[0]").value("REMETTRE"));
        avancer(id, "33", ep);
        assertThat(etat(id, "32")).isEqualTo("FAITE");
        mvc.perform(get("/api/v1/liaisons/" + id + "/fiche-suivi").with(sna())).andExpect(jsonPath("$.etat").value("RENSEIGNEE")).andExpect(jsonPath("$.actionsPermises[0]").value("CONSTATER"));
        avancer(id, "34", ep);
        mvc.perform(get("/api/v1/liaisons/" + id + "/fiche-suivi").with(sna())).andExpect(jsonPath("$.etat").value("CONSTATEE"));
        mvc.perform(post("/api/v1/liaisons/" + id + "/fiche-suivi/remise").with(conducteur())).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("HOM_SERVICE_SHEET_ALREADY_SENT"));
        mvc.perform(get("/api/v1/liaisons/" + id + "/dossier").with(conducteur()))
            .andExpect(jsonPath("$.couvertureMiseEnService.taux").value(100))
            .andExpect(jsonPath("$.etapeCourante").value("SUIVI"))
            .andExpect(jsonPath("$.actionsPermises").value(Matchers.hasItem("OUVRIR_SERVICE_SUPPLEMENTAIRE")));

        // Service supplémentaire (rang 35) : nouvelle liaison au cadrage, hors unicité ; conducteur à désigner.
        String nouvelle = mvc
            .perform(post("/api/v1/liaisons/" + id + "/services-supplementaires").with(ddrm()).contentType(MediaType.APPLICATION_JSON).content("{\"services\":[\"COMMERCE_ELECTRONIQUE\"]}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.statut").value("CADREE"))
            .andExpect(jsonPath("$.liaisonOrigineId").value(id))
            .andExpect(header().exists("Location"))
            .andReturn()
            .getResponse()
            .getContentAsString();
        String n = JsonPath.read(nouvelle, "$.id");
        mvc.perform(get("/api/v1/liaisons/" + n + "/dossier").with(conducteur()))
            .andExpect(jsonPath("$.liaisonOrigineId").value(id))
            .andExpect(jsonPath("$.conducteur").isEmpty())
            .andExpect(jsonPath("$.perimetre[*].serviceCode").value(Matchers.contains("COMMERCE_ELECTRONIQUE")));
        mvc.perform(get("/api/v1/liaisons").param("sansConducteur", "true").with(conducteur())).andExpect(jsonPath("$.contenu[*].id").value(Matchers.hasItem(n)));
        mvc
            .perform(get("/api/v1/tableaux-de-bord/acteur").param("fonctions", "CHEF_DES").with(chefDes()))
            .andExpect(jsonPath("$.compteurs[?(@.cle == 'conducteursADesigner')].valeur").value(Matchers.hasItem(Matchers.greaterThanOrEqualTo(1))));
        mvc
            .perform(post("/api/v1/liaisons/" + n + "/services-supplementaires").with(ddrm()).contentType(MediaType.APPLICATION_JSON).content("{\"services\":[\"X\"]}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("HOM_LIAISON_NOT_IN_PRODUCTION"));
        // Désignation du conducteur : le parcours de la nouvelle liaison n'attend plus de conducteur.
        mvc
            .perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/v1/liaisons/" + n + "/chef-de-projet").with(chefDes()).contentType(MediaType.APPLICATION_JSON).content("{\"chefDeProjetId\":\"" + cm.gimac.des.homologation.infrastructure.client.CollaboratorStubAdapter.CONDUCTEUR_1 + "\",\"motif\":\"Service supplémentaire\"}"))
            .andExpect(status().isOk());
        mvc.perform(get("/api/v1/liaisons/" + n + "/dossier").with(conducteur())).andExpect(jsonPath("$.conducteur.nom").value("Aminata Ngono"));
        // Une nouvelle liaison principale reste possible : le service supplémentaire est hors unicité.
        String principale = "{\"institutionId\":\"%s\",\"typeProjet\":\"CARTE_INTERBANCAIRE\",\"chefDeProjetId\":\"%s\"}".formatted(ep, cm.gimac.des.homologation.infrastructure.client.CollaboratorStubAdapter.CONDUCTEUR_2);
        mvc.perform(post("/api/v1/liaisons").with(chefDes()).contentType(MediaType.APPLICATION_JSON).content(principale)).andExpect(status().isCreated());
    }
}
