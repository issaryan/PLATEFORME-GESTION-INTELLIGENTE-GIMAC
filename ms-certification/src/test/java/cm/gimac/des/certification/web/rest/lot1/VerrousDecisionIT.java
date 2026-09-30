package cm.gimac.des.certification.web.rest.lot1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cm.gimac.des.certification.AbstractCertificationIT;
import cm.gimac.des.certification.domain.event.DecisionEvenements;
import cm.gimac.des.certification.infrastructure.client.ReauthentificationStubAdapter;
import cm.gimac.des.certification.infrastructure.messaging.consumer.CouvertureEtablieConsumer;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Verrous de la décision de bout en bout, sur PostgreSQL et Kafka réels (Testcontainers) : routes du DES
 * et du portail, persistance, immuabilité des signatures, consommation du constat de couverture, événements
 * relayés sur certification.proces-verbal et certification.approbation.
 */
class VerrousDecisionIT extends AbstractCertificationIT {

    static final RequestPostProcessor CONDUCTEUR = agent("u-conducteur", "Aminata Ngono", "CER_DOSSIER_READ", "CER_PV_ESTABLISH");
    static final RequestPostProcessor CHEF_DES = agent("u-chef-des", "Chef du DES", "CER_DOSSIER_READ", "CER_QUITUS_TECHNIQUE_SIGN");
    static final RequestPostProcessor PRODUCTION = agent("u-prod", "Production monétique", "CER_DOSSIER_READ", "CER_CHANGE_RECORD_LINK");
    static final RequestPostProcessor RSSI = agent("u-rssi", "Sécurité des SI", "CER_DOSSIER_READ", "CER_SECURITY_VERIFY");
    static final RequestPostProcessor CONFORMITE = agent("u-conf", "Conformité", "CER_DOSSIER_READ", "CER_QUITUS_CONFORMITE_SIGN");
    static final RequestPostProcessor DG = agent("u-dg", "Directeur général", "CER_DOSSIER_READ", "CER_QUITUS_DG_SIGN");
    static final RequestPostProcessor PARTICIPANT = correspondant(INSTITUTION, "CER_PORTAL_READ", "CER_PORTAL_SIGN", "HOM_PORTAL_SUBMIT");
    static final RequestPostProcessor AUTRE = correspondant(AUTRE_INSTITUTION, "CER_PORTAL_READ", "CER_PORTAL_SIGN", "HOM_PORTAL_SUBMIT");

    @Autowired
    CouvertureEtablieConsumer consommateur;

    static String pv(UUID id) {
        return "/api/v1/liaisons/" + id + "/proces-verbal";
    }

    static String appro(UUID id) {
        return "/api/v1/liaisons/" + id + "/approbation-securite";
    }

    static String portail(UUID id) {
        return "/api/v1/portail/liaisons/" + id;
    }

    UUID pvSigne(String typeProjet) throws Exception {
        UUID id = liaison(typeProjet, false, 100, true, INSTITUTION).liaisonId();
        mvc.perform(post(pv(id)).with(CONDUCTEUR)).andExpect(status().isOk());
        mvc.perform(post(pv(id) + "/signature-gimac").with(CHEF_DES).header("X-Reauth-Assertion", "a")).andExpect(status().isOk());
        mvc.perform(post(portail(id) + "/proces-verbal/signature").with(PARTICIPANT).header("X-Reauth-Assertion", "a")).andExpect(status().isOk());
        return id;
    }

    void deposer(UUID id, String type, String nom) throws Exception {
        MockMultipartFile f = new MockMultipartFile("fichier", nom, "application/pdf", "%PDF-1.7 attestation".getBytes(StandardCharsets.US_ASCII));
        mvc.perform(multipart(portail(id) + "/attestations/" + type).file(f).with(PARTICIPANT)).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Verrou 1 : établi à 100 %, signé GIMAC puis participant avec FIDO2, cachet horodaté, immuable, événements relayés")
    void verrou1() throws Exception {
        UUID id = liaison("CARTE_INTERBANCAIRE", false, 100, true, INSTITUTION).liaisonId();
        mvc
            .perform(get(pv(id)).with(CONDUCTEUR))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.etat").value("NON_ETABLI"))
            .andExpect(jsonPath("$.couverture.taux").value(100))
            .andExpect(jsonPath("$.actionsPermises[0]").value("ETABLIR"));
        mvc
            .perform(post(pv(id)).with(CONDUCTEUR))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.etat").value("ETABLI"))
            .andExpect(jsonPath("$.etabliPar").value("Aminata Ngono"))
            .andExpect(jsonPath("$.empreinte").isString());
        mvc
            .perform(post(pv(id)).with(CONDUCTEUR))
            .andExpect(status().isConflict())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.code").value("CER_PV_NOT_READY"));
        mvc.perform(get(pv(id)).with(CHEF_DES)).andExpect(jsonPath("$.actionsPermises[0]").value("SIGNER_GIMAC"));
        mvc.perform(post(pv(id) + "/signature-gimac").with(CHEF_DES)).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("IAM_REAUTH_REQUIRED"));
        mvc
            .perform(post(pv(id) + "/signature-gimac").with(CHEF_DES).header("X-Reauth-Assertion", ReauthentificationStubAdapter.ASSERTION_REFUSEE))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("CER_REAUTH_INVALID"));
        mvc
            .perform(post(pv(id) + "/signature-gimac").with(CHEF_DES).header("X-Reauth-Assertion", "assertion-webauthn"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.etat").value("SIGNE_GIMAC"))
            .andExpect(jsonPath("$.signatureGimac.signataire").value("Chef du DES"))
            .andExpect(jsonPath("$.signatureGimac.horodatage").isString());
        mvc
            .perform(post(pv(id) + "/signature-gimac").with(CHEF_DES).header("X-Reauth-Assertion", "a"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("CER_PV_NOT_SIGNABLE"));
        // Portail : cloisonné, sans nom interne.
        mvc.perform(get(portail(id) + "/proces-verbal").with(AUTRE)).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("HOM_LIAISON_NOT_FOUND"));
        mvc.perform(post(portail(id) + "/proces-verbal/signature").with(AUTRE).header("X-Reauth-Assertion", "a")).andExpect(status().isNotFound());
        mvc
            .perform(get(portail(id) + "/proces-verbal").with(PARTICIPANT))
            .andExpect(jsonPath("$.etabliPar").value("CONDUCTEUR"))
            .andExpect(jsonPath("$.signatureGimac.signataire").value("CHEF_DES"))
            .andExpect(jsonPath("$.actionsPermises[0]").value("SIGNER_PARTICIPANT"));
        mvc
            .perform(post(portail(id) + "/proces-verbal/signature").with(PARTICIPANT).header("X-Reauth-Assertion", "a"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.etat").value("SIGNE"))
            .andExpect(jsonPath("$.signatureParticipant.fonction").value("PARTICIPANT"));
        // Persistance : deux signatures scellées, deux jetons d'horodatage, approbation ouverte.
        assertThat(jdbc.queryForObject("select count(*) from signature_verrou where liaison_id = ?", Integer.class, id)).isEqualTo(2);
        assertThat(
            jdbc.queryForObject(
                "select count(*) from timestamp_token t join signature_verrou s on s.jeton_horodatage_id = t.id where s.liaison_id = ? and t.objet_type = 'SIGNATURE'",
                Integer.class,
                id
            )
        ).isEqualTo(2);
        assertThat(jdbc.queryForObject("select count(*) from attestation_securite a join approbation_securite p on p.id = a.approbation_id where p.liaison_id = ?", Integer.class, id)).isEqualTo(1);
        // Immuabilité en base : ni modification ni suppression d'une signature.
        assertThatThrownBy(() -> jdbc.update("update signature_verrou set motif = 'altéré' where liaison_id = ?", id)).hasMessageContaining("signature immuable");
        assertThatThrownBy(() -> jdbc.update("delete from signature_verrou where liaison_id = ?", id)).hasMessageContaining("signature immuable");
        // Événements : outbox puis Kafka, sujet certification.proces-verbal, extension institutionid.
        assertThat(relais.types(id.toString())).containsExactlyInAnyOrder(
            DecisionEvenements.PV_ETABLI,
            DecisionEvenements.PV_SIGNATURE_ENREGISTREE,
            DecisionEvenements.PV_SIGNATURE_ENREGISTREE,
            DecisionEvenements.PV_SIGNE
        );
        relais.relayerJusquaEnvoi(id.toString());
        var recus = relais.lire(DecisionEvenements.SUJET_PROCES_VERBAL, id.toString(), 4);
        assertThat(recus).hasSize(4);
        String signe = recus.stream().map(r -> r.value()).filter(v -> v.contains(DecisionEvenements.PV_SIGNE + "\"")).findFirst().orElseThrow();
        assertThat((String) JsonPath.read(signe, "$.specversion")).isEqualTo("1.0");
        assertThat((String) JsonPath.read(signe, "$.institutionid")).isEqualTo(INSTITUTION);
        assertThat((Integer) JsonPath.read(signe, "$.data.signatures.length()")).isEqualTo(2);
        assertThat((String) JsonPath.read(signe, "$.data.liaisonId")).isEqualTo(id.toString());
    }

    @Test
    @DisplayName("Couverture incomplète : ETABLIR_BLOQUE et 409 ; constat reçu par homologation.coverage-established.v1 (idempotent)")
    void couvertureEtConstat() throws Exception {
        UUID incomplete = liaison("CARTE_INTERBANCAIRE", false, 75, true, INSTITUTION).liaisonId();
        mvc.perform(get(pv(incomplete)).with(CONDUCTEUR)).andExpect(jsonPath("$.actionsPermises[0]").value("ETABLIR_BLOQUE"));
        mvc.perform(post(pv(incomplete)).with(CONDUCTEUR)).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("CER_COVERAGE_INCOMPLETE"));
        UUID id = liaison("PAIEMENT_MOBILE", false, 100, false, INSTITUTION).liaisonId();
        mvc.perform(get(pv(id)).with(CONDUCTEUR)).andExpect(jsonPath("$.actionsPermises").isEmpty());
        mvc.perform(post(pv(id)).with(CONDUCTEUR)).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("CER_PV_NOT_READY"));
        String evenement = """
            {"specversion":"1.0","id":"evt-%s","type":"homologation.coverage-established.v1","source":"/gimac/des/homologation",
             "data":{"liaisonId":"%s","taux":100,"servicesCouverts":["CASH_IN"]}}""".formatted(id, id);
        consommateur.recevoir(evenement);
        consommateur.recevoir(evenement);
        assertThat(jdbc.queryForObject("select count(*) from processed_event where event_id = ?", Integer.class, "evt-" + id)).isEqualTo(1);
        mvc.perform(get(pv(id)).with(CONDUCTEUR)).andExpect(jsonPath("$.actionsPermises[0]").value("ETABLIR"));
        mvc.perform(post(pv(id)).with(CONDUCTEUR)).andExpect(status().isOk()).andExpect(jsonPath("$.etat").value("ETABLI"));
        mvc.perform(get(pv(UUID.randomUUID())).with(CONDUCTEUR)).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("HOM_LIAISON_NOT_FOUND"));
    }

    @Test
    @DisplayName("Verrou 2 : dossier, attestation déposée au portail, rejet motivé, nouveau dépôt, vérification, archivage, fiche lue : APPROUVEE")
    void verrou2() throws Exception {
        UUID avant = liaison("CARTE_INTERBANCAIRE", false, 100, true, INSTITUTION).liaisonId();
        String dossier = "{\"documentId\":\"" + UUID.randomUUID() + "\",\"empreinte\":\"" + "d".repeat(64) + "\",\"nom\":\"dossier-changement.pdf\"}";
        mvc
            .perform(post(appro(avant) + "/dossier").with(PRODUCTION).contentType(MediaType.APPLICATION_JSON).content(dossier))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("CER_PV_NOT_SIGNED"));
        UUID id = pvSigne("CARTE_INTERBANCAIRE");
        mvc
            .perform(get(appro(id)).with(PRODUCTION))
            .andExpect(jsonPath("$.etat").value("EN_ATTENTE_DOSSIER"))
            .andExpect(jsonPath("$.attestations[0].type").value("PCI_DSS"))
            .andExpect(jsonPath("$.actionsPermises[0]").value("CONSTITUER_DOSSIER"));
        mvc
            .perform(post(appro(id) + "/dossier").with(PRODUCTION).contentType(MediaType.APPLICATION_JSON).content(dossier))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.etat").value("DOSSIER_CONSTITUE"))
            .andExpect(jsonPath("$.dossierChangement.nom").value("dossier-changement.pdf"));
        mvc.perform(get(portail(id) + "/approbation-securite").with(AUTRE)).andExpect(status().isNotFound());
        mvc
            .perform(get(portail(id) + "/approbation-securite").with(PARTICIPANT))
            .andExpect(jsonPath("$.visas").isEmpty())
            .andExpect(jsonPath("$.dossierChangement.deposePar").value("PRODUCTION_MONETIQUE"))
            .andExpect(jsonPath("$.actionsPermises[0]").value("DEPOSER_ATTESTATION"));
        MockMultipartFile png = new MockMultipartFile("fichier", "att.png", "image/png", new byte[] { 1, 2, 3 });
        mvc
            .perform(multipart(portail(id) + "/attestations/PCI_DSS").file(png).with(PARTICIPANT))
            .andExpect(status().isUnprocessableContent())
            .andExpect(jsonPath("$.code").value("CER_ATTESTATION_FORMAT"));
        mvc
            .perform(multipart(portail(id) + "/attestations/GSMA").file(new MockMultipartFile("fichier", "g.pdf", "application/pdf", "%PDF".getBytes(StandardCharsets.US_ASCII))).with(PARTICIPANT))
            .andExpect(status().isUnprocessableContent())
            .andExpect(jsonPath("$.code").value("CER_ATTESTATION_NOT_REQUIRED"));
        deposer(id, "PCI_DSS", "pci-dss.pdf");
        mvc
            .perform(post(appro(id) + "/attestations/PCI_DSS/verification").with(RSSI).contentType(MediaType.APPLICATION_JSON).content("{\"conforme\":false,\"motif\":\" \"}"))
            .andExpect(status().isUnprocessableContent())
            .andExpect(jsonPath("$.code").value("CER_REJECTION_REASON_REQUIRED"));
        mvc
            .perform(post(appro(id) + "/attestations/PCI_DSS/verification").with(RSSI).contentType(MediaType.APPLICATION_JSON).content("{\"conforme\":false,\"motif\":\"Attestation expirée\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.attestations[0].motifRejet").value("Attestation expirée"))
            .andExpect(jsonPath("$.attestations[0].preuve").isEmpty());
        mvc.perform(post(appro(id) + "/archivage").with(RSSI)).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("CER_ATTESTATIONS_INCOMPLETE"));
        deposer(id, "PCI_DSS", "pci-dss-2026.pdf");
        mvc
            .perform(post(appro(id) + "/attestations/PCI_DSS/verification").with(RSSI).contentType(MediaType.APPLICATION_JSON).content("{\"conforme\":true,\"motif\":\"\"}"))
            .andExpect(jsonPath("$.attestations[0].verifieePar").value("Sécurité des SI"));
        mvc.perform(post(appro(id) + "/archivage").with(RSSI)).andExpect(status().isOk()).andExpect(jsonPath("$.etat").value("VERIFIE_ARCHIVE"));
        mvc.perform(post(appro(id) + "/fiche/lecture").with(PRODUCTION)).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("CER_CHANGE_RECORD_MISSING"));
        mvc
            .perform(put(appro(id) + "/fiche").with(PRODUCTION).contentType(MediaType.APPLICATION_JSON).content("{\"reference\":\"mauvaise\"}"))
            .andExpect(status().isUnprocessableContent())
            .andExpect(jsonPath("$.code").value("CER_CHANGE_RECORD_INVALID"));
        mvc
            .perform(put(appro(id) + "/fiche").with(PRODUCTION).contentType(MediaType.APPLICATION_JSON).content("{\"reference\":\"CHG-2026-0413\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.etat").value("APPROUVEE"))
            .andExpect(jsonPath("$.ficheChangement.etatApprobation").value("APPROUVEE"))
            .andExpect(jsonPath("$.approuveeLe").isString());
        mvc.perform(post(appro(id) + "/fiche/lecture").with(RSSI)).andExpect(status().isOk()).andExpect(jsonPath("$.etat").value("APPROUVEE"));
        assertThat(relais.types(id.toString())).contains(
            DecisionEvenements.DOSSIER_CHANGEMENT_CONSTITUE,
            DecisionEvenements.ATTESTATION_DEPOSEE,
            DecisionEvenements.ATTESTATION_VERIFIEE,
            DecisionEvenements.ATTESTATIONS_ARCHIVEES,
            DecisionEvenements.FICHE_CHANGEMENT_REFERENCEE,
            DecisionEvenements.SECURITE_APPROUVEE
        );
        relais.relayerJusquaEnvoi(id.toString());
        var recus = relais.lire(DecisionEvenements.SUJET_APPROBATION, id.toString(), (int) relais.types(id.toString()).stream().filter(t -> !t.startsWith("certification.pv-")).count());
        assertThat(recus).extracting(r -> (String) JsonPath.read(r.value(), "$.type")).contains(DecisionEvenements.SECURITE_APPROUVEE, DecisionEvenements.ATTESTATIONS_ARCHIVEES);
        assertThat(recus).allSatisfy(r -> assertThat(r.value()).doesNotContain("%PDF"));
    }

    @Test
    @DisplayName("Visas activés (délégataire) : conformité et DG signés avec FIDO2 avant l'approbation ; défavorable motivé")
    void visas() throws Exception {
        UUID id = pvSigne("DELEGATAIRE");
        mvc
            .perform(get(appro(id)).with(CONFORMITE))
            .andExpect(jsonPath("$.visas[0].actif").value(true))
            .andExpect(jsonPath("$.actionsPermises[0]").value("SIGNER_VISA_CONFORMITE"));
        String avis = "{\"favorable\":true,\"motif\":\"Conforme\"}";
        mvc
            .perform(post(appro(id) + "/visas/CONFORMITE").with(CONFORMITE).contentType(MediaType.APPLICATION_JSON).content(avis))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("IAM_REAUTH_REQUIRED"));
        mvc
            .perform(post(appro(id) + "/visas/CONFORMITE").with(DG).header("X-Reauth-Assertion", "a").contentType(MediaType.APPLICATION_JSON).content(avis))
            .andExpect(status().isForbidden());
        mvc
            .perform(post(appro(id) + "/visas/CONFORMITE").with(CONFORMITE).header("X-Reauth-Assertion", "a").contentType(MediaType.APPLICATION_JSON).content(avis))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.visas[0].etat").value("SIGNE_FAVORABLE"))
            .andExpect(jsonPath("$.visas[0].signature.fonction").value("CONFORMITE"));
        mvc
            .perform(post(appro(id) + "/visas/CONFORMITE").with(CONFORMITE).header("X-Reauth-Assertion", "a").contentType(MediaType.APPLICATION_JSON).content(avis))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("CER_VISA_ALREADY_SIGNED"));
        mvc
            .perform(
                post(appro(id) + "/visas/DIRECTION_GENERALE").with(DG).header("X-Reauth-Assertion", "a").contentType(MediaType.APPLICATION_JSON).content("{\"favorable\":false,\"motif\":\"\"}")
            )
            .andExpect(status().isUnprocessableContent())
            .andExpect(jsonPath("$.code").value("CER_REJECTION_REASON_REQUIRED"));
        mvc
            .perform(
                post(appro(id) + "/visas/DIRECTION_GENERALE")
                    .with(DG)
                    .header("X-Reauth-Assertion", "a")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"favorable\":false,\"motif\":\"Risque de place\"}")
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.etat").value("REFUSEE"));
        assertThat(jdbc.queryForObject("select count(*) from signature_verrou where liaison_id = ? and objet = 'VISA'", Integer.class, id)).isEqualTo(2);
        UUID carte = pvSigne("CARTE_INTERBANCAIRE");
        mvc
            .perform(post(appro(carte) + "/visas/DIRECTION_GENERALE").with(DG).header("X-Reauth-Assertion", "a").contentType(MediaType.APPLICATION_JSON).content(avis))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("CER_VISA_NOT_REQUIRED"));
    }
}
