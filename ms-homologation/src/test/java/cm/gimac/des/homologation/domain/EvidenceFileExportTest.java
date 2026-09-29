package cm.gimac.des.homologation.domain;

import static cm.gimac.des.homologation.domain.EvidenceFileExportTestSamples.*;
import static cm.gimac.des.homologation.domain.LiaisonTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.homologation.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class EvidenceFileExportTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(EvidenceFileExport.class);
        EvidenceFileExport evidenceFileExport1 = getEvidenceFileExportSample1();
        EvidenceFileExport evidenceFileExport2 = new EvidenceFileExport();
        assertThat(evidenceFileExport1).isNotEqualTo(evidenceFileExport2);

        evidenceFileExport2.setId(evidenceFileExport1.getId());
        assertThat(evidenceFileExport1).isEqualTo(evidenceFileExport2);

        evidenceFileExport2 = getEvidenceFileExportSample2();
        assertThat(evidenceFileExport1).isNotEqualTo(evidenceFileExport2);
    }

    @Test
    void liaisonTest() {
        EvidenceFileExport evidenceFileExport = getEvidenceFileExportRandomSampleGenerator();
        Liaison liaisonBack = getLiaisonRandomSampleGenerator();

        evidenceFileExport.setLiaison(liaisonBack);
        assertThat(evidenceFileExport.getLiaison()).isEqualTo(liaisonBack);

        evidenceFileExport.liaison(null);
        assertThat(evidenceFileExport.getLiaison()).isNull();
    }
}
