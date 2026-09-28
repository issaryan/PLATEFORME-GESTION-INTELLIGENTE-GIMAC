package cm.gimac.des.certification.domain;

import static cm.gimac.des.certification.domain.CertificationAttestationTestSamples.*;
import static cm.gimac.des.certification.domain.ChangeRecordTestSamples.*;
import static cm.gimac.des.certification.domain.CommissioningFileTestSamples.*;
import static cm.gimac.des.certification.domain.EvidenceItemTestSamples.*;
import static cm.gimac.des.certification.domain.QuitusTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.certification.web.rest.TestUtil;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CommissioningFileTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(CommissioningFile.class);
        CommissioningFile commissioningFile1 = getCommissioningFileSample1();
        CommissioningFile commissioningFile2 = new CommissioningFile();
        assertThat(commissioningFile1).isNotEqualTo(commissioningFile2);

        commissioningFile2.setId(commissioningFile1.getId());
        assertThat(commissioningFile1).isEqualTo(commissioningFile2);

        commissioningFile2 = getCommissioningFileSample2();
        assertThat(commissioningFile1).isNotEqualTo(commissioningFile2);
    }

    @Test
    void changeRecordTest() {
        CommissioningFile commissioningFile = getCommissioningFileRandomSampleGenerator();
        ChangeRecord changeRecordBack = getChangeRecordRandomSampleGenerator();

        commissioningFile.setChangeRecord(changeRecordBack);
        assertThat(commissioningFile.getChangeRecord()).isEqualTo(changeRecordBack);

        commissioningFile.changeRecord(null);
        assertThat(commissioningFile.getChangeRecord()).isNull();
    }

    @Test
    void attestationTest() {
        CommissioningFile commissioningFile = getCommissioningFileRandomSampleGenerator();
        CertificationAttestation certificationAttestationBack = getCertificationAttestationRandomSampleGenerator();

        commissioningFile.setAttestation(certificationAttestationBack);
        assertThat(commissioningFile.getAttestation()).isEqualTo(certificationAttestationBack);

        commissioningFile.attestation(null);
        assertThat(commissioningFile.getAttestation()).isNull();
    }

    @Test
    void commissioningReportTest() {
        CommissioningFile commissioningFile = getCommissioningFileRandomSampleGenerator();
        EvidenceItem evidenceItemBack = getEvidenceItemRandomSampleGenerator();

        commissioningFile.setCommissioningReport(evidenceItemBack);
        assertThat(commissioningFile.getCommissioningReport()).isEqualTo(evidenceItemBack);

        commissioningFile.commissioningReport(null);
        assertThat(commissioningFile.getCommissioningReport()).isNull();
    }

    @Test
    void quitusTest() {
        CommissioningFile commissioningFile = getCommissioningFileRandomSampleGenerator();
        Quitus quitusBack = getQuitusRandomSampleGenerator();

        commissioningFile.addQuitus(quitusBack);
        assertThat(commissioningFile.getQuituses()).containsOnly(quitusBack);
        assertThat(quitusBack.getFile()).isEqualTo(commissioningFile);

        commissioningFile.removeQuitus(quitusBack);
        assertThat(commissioningFile.getQuituses()).doesNotContain(quitusBack);
        assertThat(quitusBack.getFile()).isNull();

        commissioningFile.quituses(new HashSet<>(Set.of(quitusBack)));
        assertThat(commissioningFile.getQuituses()).containsOnly(quitusBack);
        assertThat(quitusBack.getFile()).isEqualTo(commissioningFile);

        commissioningFile.setQuituses(new HashSet<>());
        assertThat(commissioningFile.getQuituses()).doesNotContain(quitusBack);
        assertThat(quitusBack.getFile()).isNull();
    }

    @Test
    void piecesTest() {
        CommissioningFile commissioningFile = getCommissioningFileRandomSampleGenerator();
        EvidenceItem evidenceItemBack = getEvidenceItemRandomSampleGenerator();

        commissioningFile.addPieces(evidenceItemBack);
        assertThat(commissioningFile.getPieces()).containsOnly(evidenceItemBack);
        assertThat(evidenceItemBack.getFile()).isEqualTo(commissioningFile);

        commissioningFile.removePieces(evidenceItemBack);
        assertThat(commissioningFile.getPieces()).doesNotContain(evidenceItemBack);
        assertThat(evidenceItemBack.getFile()).isNull();

        commissioningFile.pieces(new HashSet<>(Set.of(evidenceItemBack)));
        assertThat(commissioningFile.getPieces()).containsOnly(evidenceItemBack);
        assertThat(evidenceItemBack.getFile()).isEqualTo(commissioningFile);

        commissioningFile.setPieces(new HashSet<>());
        assertThat(commissioningFile.getPieces()).doesNotContain(evidenceItemBack);
        assertThat(evidenceItemBack.getFile()).isNull();
    }
}
