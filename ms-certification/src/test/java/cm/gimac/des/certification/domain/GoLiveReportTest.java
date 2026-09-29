package cm.gimac.des.certification.domain;

import static cm.gimac.des.certification.domain.GoLiveDossierTestSamples.*;
import static cm.gimac.des.certification.domain.GoLiveReportTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.certification.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class GoLiveReportTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(GoLiveReport.class);
        GoLiveReport goLiveReport1 = getGoLiveReportSample1();
        GoLiveReport goLiveReport2 = new GoLiveReport();
        assertThat(goLiveReport1).isNotEqualTo(goLiveReport2);

        goLiveReport2.setId(goLiveReport1.getId());
        assertThat(goLiveReport1).isEqualTo(goLiveReport2);

        goLiveReport2 = getGoLiveReportSample2();
        assertThat(goLiveReport1).isNotEqualTo(goLiveReport2);
    }

    @Test
    void dossierTest() {
        GoLiveReport goLiveReport = getGoLiveReportRandomSampleGenerator();
        GoLiveDossier goLiveDossierBack = getGoLiveDossierRandomSampleGenerator();

        goLiveReport.setDossier(goLiveDossierBack);
        assertThat(goLiveReport.getDossier()).isEqualTo(goLiveDossierBack);

        goLiveReport.dossier(null);
        assertThat(goLiveReport.getDossier()).isNull();
    }
}
