package cm.gimac.des.certification.web.rest.lot1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cm.gimac.des.certification.AbstractCertificationIT;
import cm.gimac.des.certification.infrastructure.messaging.consumer.OperationsHomologationConsumer;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * D-93 §4 et D-94 de bout en bout sur PostgreSQL (Testcontainers) : événements d'opération de MS-HOMOLOGATION
 * consommés une seule fois (projection operation_liaison), gestes du procès-verbal refusés hors opération ouverte ou
 * à un acteur non désigné, circuit de signature lu dans les signatures configurées de l'opération et figé.
 */
class GestesParOperationIT extends AbstractCertificationIT {

    static final RequestPostProcessor DESIGNE = agent("u-designe", "Conducteur désigné", "CER_DOSSIER_READ", "CER_PV_ESTABLISH");
    static final RequestPostProcessor INTRUS = agent("u-intrus", "Autre conducteur", "CER_DOSSIER_READ", "CER_PV_ESTABLISH");
    static final RequestPostProcessor DDRM = agent("u-ddrm", "Chargé DDRM", "CER_DOSSIER_READ", "CER_QUITUS_TECHNIQUE_SIGN");
    static final RequestPostProcessor CHEF_DES = agent("u-chef-des", "Chef du DES", "CER_DOSSIER_READ", "CER_QUITUS_TECHNIQUE_SIGN");
    static final RequestPostProcessor PARTICIPANT = correspondant(INSTITUTION, "CER_PORTAL_READ", "CER_PORTAL_SIGN");

    @Autowired
    OperationsHomologationConsumer consommateur;

    static String evenement(String id, String type, UUID liaison, UUID operation, String code, String intervenants, String extra, Instant le) {
        String declencheur = switch (code) {
            case "18" -> "certification.pv-established.v1";
            case "19" -> "certification.pv-signed.v1";
            default -> null;
        };
        return """
            {"specversion":"1.0","id":"%s","source":"/ms-homologation","type":"%s","data":{"liaisonId":"%s","operationId":"%s",\
            "code":"%s","libelle":"Opération %s","geste":"EVENEMENT_EXTERNE","vue":"DECISION",%s%s%s"le":"%s"}}"""
            .formatted(id, type, liaison, operation, code, code, declencheur == null ? "" : "\"declencheur\":\"" + declencheur + "\",", intervenants, extra, le);
    }

    @Test
    @DisplayName("Projection idempotente ; établissement refusé hors opération ouverte et à l'intrus ; circuit de l'opération appliqué")
    void gestesParOperation() throws Exception {
        UUID id = liaison("CARTE_INTERBANCAIRE", false, 100, true, INSTITUTION).liaisonId();
        UUID op18 = UUID.randomUUID();
        Instant t = Instant.parse("2026-10-07T08:00:00Z");
        String r18 = "\"responsable\":{\"qualite\":\"R\",\"fonction\":\"CONDUCTEUR\",\"compteId\":\"u-designe\",\"aAffecter\":false},";
        String validation = evenement(UUID.randomUUID().toString(), "homologation.operation-validation-requested.v1", id, op18, "18", r18, "", t);
        consommateur.recevoir(validation);
        mvc.perform(post("/api/v1/liaisons/" + id + "/proces-verbal").with(DESIGNE)).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("CER_OPERATION_NON_OUVERTE"));

        String activee = evenement(UUID.randomUUID().toString(), "homologation.operation-activated.v1", id, op18, "18", r18, "", t.plusSeconds(60));
        consommateur.recevoir(activee);
        consommateur.recevoir(activee);
        assertThat(jdbc.queryForObject("select count(*) from operation_liaison where liaison_id = ?", Integer.class, id)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select etat from operation_liaison where id = ?", String.class, op18)).isEqualTo("OUVERTE");
        assertThat(jdbc.queryForObject("select declencheur from operation_liaison where id = ?", String.class, op18)).isEqualTo("certification.pv-established.v1");
        assertThat(jdbc.queryForObject("select count(*) from processed_event where consumer_group = 'certification-operations'", Integer.class)).isEqualTo(2);
        // Rejeu de l'ancienne demande de validation sous un autre identifiant : plus ancien, ignoré.
        consommateur.recevoir(validation.replaceFirst("\"id\":\"[^\"]+\"", "\"id\":\"" + UUID.randomUUID() + "\""));
        assertThat(jdbc.queryForObject("select etat from operation_liaison where id = ?", String.class, op18)).isEqualTo("OUVERTE");

        mvc.perform(post("/api/v1/liaisons/" + id + "/proces-verbal").with(INTRUS)).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("CER_ACTEUR_NON_DESIGNE"));
        mvc.perform(post("/api/v1/liaisons/" + id + "/proces-verbal").with(DESIGNE)).andExpect(status().isOk()).andExpect(jsonPath("$.etat").value("ETABLI"));

        // Signature : opération 19 pas encore ouverte.
        mvc
            .perform(post("/api/v1/liaisons/" + id + "/proces-verbal/signature-gimac").with(DDRM).header("X-Reauth-Assertion", "a"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("CER_OPERATION_ABSENTE"));
        UUID op19 = UUID.randomUUID();
        String r19 =
            "\"responsable\":{\"qualite\":\"R\",\"fonction\":\"DDRM\",\"compteId\":\"u-ddrm\",\"aAffecter\":false}," +
            "\"institutionsConcernees\":[\"" + INSTITUTION + "\"],";
        String signatures = "\"signatures\":[{\"code\":\"VISA_DDRM\",\"libelle\":\"Visa de la DDRM\",\"signataire\":\"DDRM\",\"ordre\":1}],";
        consommateur.recevoir(evenement(UUID.randomUUID().toString(), "homologation.operation-activated.v1", id, op19, "19", r19, signatures, t.plusSeconds(120)));

        mvc
            .perform(post("/api/v1/liaisons/" + id + "/proces-verbal/signature-gimac").with(CHEF_DES).header("X-Reauth-Assertion", "a"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("CER_SIGNATAIRE_NON_DESIGNE"))
            .andExpect(jsonPath("$.details.fonction").value("DDRM"));
        mvc
            .perform(get("/api/v1/liaisons/" + id + "/proces-verbal").with(DDRM))
            .andExpect(jsonPath("$.actionsPermises[0]").value("SIGNER_GIMAC"))
            .andExpect(jsonPath("$.circuit[0].code").value("VISA_DDRM"))
            .andExpect(jsonPath("$.circuit[1].code").value("PARTICIPANT"));
        mvc
            .perform(post("/api/v1/liaisons/" + id + "/proces-verbal/signature-gimac").with(DDRM).header("X-Reauth-Assertion", "a"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.etat").value("SIGNE_GIMAC"))
            .andExpect(jsonPath("$.signatureGimac.fonction").value("DDRM"));
        mvc
            .perform(post("/api/v1/portail/liaisons/" + id + "/proces-verbal/signature").with(PARTICIPANT).header("X-Reauth-Assertion", "a"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.etat").value("SIGNE"));
        assertThat(jdbc.queryForList("select etape from signature_verrou where liaison_id = ? order by signe_le", String.class, id)).containsExactly(
            "VISA_DDRM",
            "PARTICIPANT"
        );
        assertThat(jdbc.queryForObject("select circuit from proces_verbal_certification where liaison_id = ?", String.class, id)).contains("VISA_DDRM", "PARTICIPANT");
        assertThat(jdbc.queryForObject("select count(*) from approbation_securite where liaison_id = ?", Integer.class, id)).isEqualTo(1);

        // Clôture publiée par MS-HOMOLOGATION (operation-closed.v1) : l'opération 18 est faite.
        String cloture = """
            {"id":"%s","type":"homologation.operation-closed.v1","data":{"liaisonId":"%s","operationId":"%s","code":"18","etat":"FAITE",\
            "declencheur":"certification.pv-established.v1","le":"%s"}}""".formatted(UUID.randomUUID(), id, op18, t.plusSeconds(150));
        consommateur.recevoir(cloture);
        consommateur.recevoir(cloture);
        assertThat(jdbc.queryForObject("select etat from operation_liaison where id = ?", String.class, op18)).isEqualTo("FAITE");
        // Jalon CERTIFIEE : l'opération 19 est faite, toute nouvelle signature est refusée.
        consommateur.recevoir(
            """
            {"id":"%s","type":"homologation.liaison-milestone-reached.v1","data":{"liaisonId":"%s","operationId":"%s","code":"19","jalon":"CERTIFIEE","le":"%s"}}"""
                .formatted(UUID.randomUUID(), id, op19, t.plusSeconds(180))
        );
        assertThat(jdbc.queryForObject("select etat from operation_liaison where id = ?", String.class, op19)).isEqualTo("FAITE");
    }

    @Test
    @DisplayName("Signatures publiées vides : circuit par défaut ; déclencheur absent de l'événement : opération sans geste")
    void signaturesVidesEtSansDeclencheur() throws Exception {
        UUID id = liaison("CARTE_INTERBANCAIRE", false, 100, true, INSTITUTION).liaisonId();
        Instant t = Instant.parse("2026-10-07T08:00:00Z");
        String r = "\"responsable\":{\"qualite\":\"R\",\"fonction\":\"CONDUCTEUR\",\"compteId\":\"u-designe\",\"aAffecter\":false},";
        consommateur.recevoir(evenement(UUID.randomUUID().toString(), "homologation.operation-activated.v1", id, UUID.randomUUID(), "18", r, "", t));
        mvc.perform(post("/api/v1/liaisons/" + id + "/proces-verbal").with(DESIGNE)).andExpect(status().isOk());
        consommateur.recevoir(evenement(UUID.randomUUID().toString(), "homologation.operation-activated.v1", id, UUID.randomUUID(), "19", "", "\"signatures\":[],", t.plusSeconds(1)));
        mvc
            .perform(get("/api/v1/liaisons/" + id + "/proces-verbal").with(CHEF_DES))
            .andExpect(jsonPath("$.circuit[0].code").value("GIMAC"))
            .andExpect(jsonPath("$.circuit[1].code").value("PARTICIPANT"))
            .andExpect(jsonPath("$.actionsPermises[0]").value("SIGNER_GIMAC"));
        // Opération 20 publiée sans déclencheur : le dossier de changement n'a pas d'opération qui le porte.
        UUID autre = liaison("CARTE_INTERBANCAIRE", false, 100, true, INSTITUTION).liaisonId();
        consommateur.recevoir(evenement(UUID.randomUUID().toString(), "homologation.operation-activated.v1", autre, UUID.randomUUID(), "18", r, "", t));
        assertThat(jdbc.queryForObject("select count(*) from operation_liaison where liaison_id = ? and declencheur is null", Integer.class, autre)).isZero();
        consommateur.recevoir(evenement(UUID.randomUUID().toString(), "homologation.operation-activated.v1", autre, UUID.randomUUID(), "26", r, "", t));
        assertThat(jdbc.queryForObject("select count(*) from operation_liaison where liaison_id = ? and declencheur is null", Integer.class, autre)).isEqualTo(1);
    }
}
