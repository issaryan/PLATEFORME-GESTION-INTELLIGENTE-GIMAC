package cm.gimac.des.certification.cucumber.stepdefs;

import static cm.gimac.des.certification.AbstractCertificationIT.AUTRE_INSTITUTION;
import static cm.gimac.des.certification.AbstractCertificationIT.INSTITUTION;
import static cm.gimac.des.certification.AbstractCertificationIT.agent;
import static cm.gimac.des.certification.AbstractCertificationIT.correspondant;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cm.gimac.des.certification.AbstractCertificationIT;
import cm.gimac.des.certification.application.port.out.ContexteCertificationPort.ContexteCertification;
import cm.gimac.des.certification.application.port.out.ContexteCertificationPort.Couverture;
import cm.gimac.des.certification.infrastructure.client.ContexteCertificationStubAdapter;
import com.jayway.jsonpath.JsonPath;
import io.cucumber.java.After;
import io.cucumber.java.fr.Alors;
import io.cucumber.java.fr.Et;
import io.cucumber.java.fr.Quand;
import io.cucumber.java.fr.Étantdonné;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Étapes des scénarios des verrous de la décision (D-56) : les acteurs agissent par les routes réelles
 * (MockMvc), jeton à deux facteurs portant la matrice des rôles de leur profil (frontend permissions.ts).
 */
public class DecisionStepDefs extends StepDefs {

    static final RequestPostProcessor CONDUCTEUR = agent("u-conducteur", "Aminata Ngono", "CER_DOSSIER_READ", "CER_PV_ESTABLISH");
    static final RequestPostProcessor CHEF_DES = agent("u-chef-des", "Chef du DES", "CER_DOSSIER_READ", "CER_QUITUS_TECHNIQUE_SIGN");
    static final RequestPostProcessor PRODUCTION = agent("u-prod", "Production monétique", "CER_DOSSIER_READ", "CER_CHANGE_RECORD_LINK");
    static final RequestPostProcessor RSSI = agent("u-rssi", "Sécurité des SI", "CER_DOSSIER_READ", "CER_SECURITY_VERIFY");
    static final RequestPostProcessor CONFORMITE = agent("u-conf", "Conformité", "CER_DOSSIER_READ", "CER_QUITUS_CONFORMITE_SIGN");
    static final RequestPostProcessor DG = agent("u-dg", "Directeur général", "CER_DOSSIER_READ", "CER_QUITUS_DG_SIGN");
    static final RequestPostProcessor PARTICIPANT = correspondant(INSTITUTION, "CER_PORTAL_READ", "CER_PORTAL_SIGN", "HOM_PORTAL_SUBMIT");
    static final RequestPostProcessor AUTRE = correspondant(AUTRE_INSTITUTION, "CER_PORTAL_READ", "CER_PORTAL_SIGN", "HOM_PORTAL_SUBMIT");

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    ContexteCertificationStubAdapter contextes;

    UUID liaison;

    @After
    public void effacer() {
        AbstractCertificationIT.effacer(jdbc);
    }

    String pv() {
        return "/api/v1/liaisons/" + liaison + "/proces-verbal";
    }

    String appro() {
        return "/api/v1/liaisons/" + liaison + "/approbation-securite";
    }

    String portail() {
        return "/api/v1/portail/liaisons/" + liaison;
    }

    void enregistrer(String typeProjet, boolean fintech, int taux) {
        liaison = UUID.randomUUID();
        contextes.enregistrer(
            new ContexteCertification(
                liaison,
                "LIA-2026-" + liaison.toString().substring(0, 3),
                INSTITUTION,
                "Établissement de paiement A",
                typeProjet,
                fintech,
                "EN_TEST",
                new Couverture(taux, taux / 25, 4),
                true,
                List.of("RETRAIT", "PAIEMENT", "TRANSFERT", "COMMERCE_ELECTRONIQUE").subList(0, taux / 25),
                "u-conducteur",
                "Aminata Ngono"
            )
        );
    }

    String corps() throws Exception {
        return actions.andReturn().getResponse().getContentAsString();
    }

    // ------------------------------------------------------------------ Contexte

    @Étantdonné("une liaison {string} dont la couverture de certification constatée vaut {int} %")
    public void liaisonCouverte(String typeProjet, int taux) {
        enregistrer(typeProjet, false, taux);
    }

    @Étantdonné("une liaison {string} dont le procès-verbal est établi")
    public void pvEtabli(String typeProjet) throws Exception {
        enregistrer(typeProjet, false, 100);
        mvc.perform(post(pv()).with(CONDUCTEUR)).andExpect(status().isOk());
    }

    @Étantdonné("une liaison {string} dont le procès-verbal est signé des deux parties")
    public void pvSigne(String typeProjet) throws Exception {
        pvSigne(typeProjet, false);
    }

    @Étantdonné("une liaison {string} d'une fintech dont le procès-verbal est signé des deux parties")
    public void pvSigneFintech(String typeProjet) throws Exception {
        pvSigne(typeProjet, true);
    }

    void pvSigne(String typeProjet, boolean fintech) throws Exception {
        enregistrer(typeProjet, fintech, 100);
        mvc.perform(post(pv()).with(CONDUCTEUR)).andExpect(status().isOk());
        mvc.perform(post(pv() + "/signature-gimac").with(CHEF_DES).header("X-Reauth-Assertion", "a")).andExpect(status().isOk());
        mvc.perform(post(portail() + "/proces-verbal/signature").with(PARTICIPANT).header("X-Reauth-Assertion", "a")).andExpect(status().isOk());
    }

    // ------------------------------------------------------------------ Verrou 1

    @Quand("le conducteur de projet consulte le procès-verbal")
    public void consulterPv() throws Exception {
        actions = mvc.perform(get(pv()).with(CONDUCTEUR));
    }

    @Quand("le conducteur de projet établit le procès-verbal")
    public void etablirPv() throws Exception {
        actions = mvc.perform(post(pv()).with(CONDUCTEUR));
    }

    @Quand("le chef du DES signe le procès-verbal sans réauthentification")
    public void signerSansReauth() throws Exception {
        actions = mvc.perform(post(pv() + "/signature-gimac").with(CHEF_DES));
    }

    @Quand("le chef du DES signe le procès-verbal avec son assertion FIDO2")
    public void signerGimac() throws Exception {
        actions = mvc.perform(post(pv() + "/signature-gimac").with(CHEF_DES).header("X-Reauth-Assertion", "assertion-chef-des"));
    }

    @Quand("le participant signe le procès-verbal au portail avec son assertion FIDO2")
    public void signerParticipant() throws Exception {
        actions = mvc.perform(post(portail() + "/proces-verbal/signature").with(PARTICIPANT).header("X-Reauth-Assertion", "assertion-participant"));
    }

    @Quand("le correspondant d'une autre institution consulte le procès-verbal au portail")
    public void consulterAutreInstitution() throws Exception {
        actions = mvc.perform(get(portail() + "/proces-verbal").with(AUTRE));
    }

    @Alors("les actions permises sont {string}")
    public void actionsPermises(String attendues) throws Exception {
        actions.andExpect(status().isOk());
        assertThat(JsonPath.<List<String>>read(corps(), "$.actionsPermises")).containsExactly(attendues.split(","));
    }

    @Alors("le procès-verbal est à l'état {string} avec une empreinte")
    public void pvEtatEmpreinte(String etat) throws Exception {
        pvEtat(etat);
        assertThat(JsonPath.<String>read(corps(), "$.empreinte")).matches("[0-9a-f]{64}");
    }

    @Alors("le procès-verbal est à l'état {string}")
    public void pvEtat(String etat) throws Exception {
        actions.andExpect(status().isOk()).andExpect(jsonPath("$.etat").value(etat));
    }

    @Alors("la réponse a le statut {int} et le code {string}")
    public void refus(int statut, String code) throws Exception {
        actions.andExpect(status().is(statut)).andExpect(jsonPath("$.code").value(code));
    }

    @Et("l'événement {string} est écrit dans l'outbox")
    public void evenement(String type) {
        Integer n = jdbc.queryForObject(
            "select count(*) from outbox_event where aggregate_id = ? and event_type = ?",
            Integer.class,
            liaison.toString(),
            type
        );
        assertThat(n).isEqualTo(1);
    }

    @Et("chaque signature est scellée par un cachet horodaté et immuable")
    public void signaturesScellees() {
        List<String> cachets = jdbc.queryForList(
            "select s.cachet from signature_verrou s join timestamp_token t on t.id = s.jeton_horodatage_id where s.liaison_id = ?",
            String.class,
            liaison
        );
        assertThat(cachets).hasSize(2).allMatch(c -> !c.isBlank());
        assertThatThrownBy(() -> jdbc.update("update signature_verrou set signataire_nom = 'autre' where liaison_id = ?", liaison)).hasMessageContaining(
            "signature immuable"
        );
    }

    // ------------------------------------------------------------------ Verrou 2

    @Alors("les attestations exigées sont {string}")
    public void attestationsExigees(String types) throws Exception {
        String c = mvc.perform(get(appro()).with(RSSI)).andReturn().getResponse().getContentAsString();
        assertThat(JsonPath.<List<String>>read(c, "$.attestations[*].type")).containsExactlyInAnyOrder(types.split(","));
    }

    @Quand("la production monétique constitue le dossier de changement")
    public void constituerDossier() throws Exception {
        String dossier = "{\"documentId\":\"" + UUID.randomUUID() + "\",\"empreinte\":\"" + "d".repeat(64) + "\",\"nom\":\"dossier-changement.pdf\"}";
        actions = mvc.perform(post(appro() + "/dossier").with(PRODUCTION).contentType(MediaType.APPLICATION_JSON).content(dossier));
        actions.andExpect(status().isOk()).andExpect(jsonPath("$.etat").value("DOSSIER_CONSTITUE"));
    }

    @Et("la production monétique a constitué le dossier de changement")
    public void dossierConstitue() throws Exception {
        constituerDossier();
    }

    @Quand("le participant dépose les attestations exigées au portail")
    public void deposerExigees() throws Exception {
        String c = mvc.perform(get(portail() + "/approbation-securite").with(PARTICIPANT)).andReturn().getResponse().getContentAsString();
        for (String type : JsonPath.<List<String>>read(c, "$.attestations[*].type")) {
            deposer(type);
        }
    }

    @Et("le participant a déposé l'attestation {string}")
    public void deposer(String type) throws Exception {
        MockMultipartFile f = new MockMultipartFile("fichier", type.toLowerCase() + ".pdf", "application/pdf", ("%PDF-1.7 " + type).getBytes(StandardCharsets.US_ASCII));
        mvc.perform(multipart(portail() + "/attestations/" + type).file(f).with(PARTICIPANT)).andExpect(status().isOk());
    }

    @Quand("la sécurité des SI vérifie conformes toutes les attestations")
    public void verifierToutes() throws Exception {
        String c = mvc.perform(get(appro()).with(RSSI)).andReturn().getResponse().getContentAsString();
        for (String type : JsonPath.<List<String>>read(c, "$.attestations[*].type")) {
            mvc
                .perform(post(appro() + "/attestations/" + type + "/verification").with(RSSI).contentType(MediaType.APPLICATION_JSON).content("{\"conforme\":true,\"motif\":\"\"}"))
                .andExpect(status().isOk());
        }
    }

    @Quand("la sécurité des SI rejette l'attestation {string} avec le motif {string}")
    public void rejeter(String type, String motif) throws Exception {
        actions = mvc.perform(
            post(appro() + "/attestations/" + type + "/verification")
                .with(RSSI)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"conforme\":false,\"motif\":\"" + motif + "\"}")
        );
    }

    @Alors("l'attestation {string} est de nouveau attendue avec le motif {string}")
    public void attestationAttendue(String type, String motif) throws Exception {
        actions.andExpect(status().isOk());
        String c = mvc.perform(get(portail() + "/approbation-securite").with(PARTICIPANT)).andReturn().getResponse().getContentAsString();
        assertThat(JsonPath.<List<Object>>read(c, "$.attestations[?(@.type == '" + type + "')].preuve")).containsOnlyNulls();
        assertThat(JsonPath.<List<String>>read(c, "$.attestations[?(@.type == '" + type + "')].motifRejet")).containsExactly(motif);
        assertThat(JsonPath.<List<String>>read(c, "$.actionsPermises")).containsExactly("DEPOSER_ATTESTATION");
    }

    @Quand("la sécurité des SI archive les attestations")
    public void archiver() throws Exception {
        actions = mvc.perform(post(appro() + "/archivage").with(RSSI));
    }

    @Quand("la production monétique référence la fiche de changement {string}")
    public void referencer(String reference) throws Exception {
        actions = mvc.perform(put(appro() + "/fiche").with(PRODUCTION).contentType(MediaType.APPLICATION_JSON).content("{\"reference\":\"" + reference + "\"}"));
    }

    @Quand("la production monétique relit la fiche de changement")
    public void relire() throws Exception {
        actions = mvc.perform(post(appro() + "/fiche/lecture").with(PRODUCTION));
    }

    @Quand("la conformité signe son visa favorable avec son assertion FIDO2")
    public void visaConformite() throws Exception {
        actions = mvc.perform(
            post(appro() + "/visas/CONFORMITE").with(CONFORMITE).header("X-Reauth-Assertion", "a").contentType(MediaType.APPLICATION_JSON).content("{\"favorable\":true,\"motif\":\"\"}")
        );
    }

    @Quand("la Direction Générale signe son visa favorable avec son assertion FIDO2")
    public void visaDg() throws Exception {
        actions = mvc.perform(
            post(appro() + "/visas/DIRECTION_GENERALE").with(DG).header("X-Reauth-Assertion", "a").contentType(MediaType.APPLICATION_JSON).content("{\"favorable\":true,\"motif\":\"\"}")
        );
    }

    @Alors("l'approbation de la sécurité est à l'état {string}")
    public void approbationEtat(String etat) throws Exception {
        actions.andExpect(status().isOk());
        String c = mvc.perform(get(appro()).with(RSSI)).andReturn().getResponse().getContentAsString();
        assertThat(JsonPath.<String>read(c, "$.etat")).isEqualTo(etat);
    }
}
