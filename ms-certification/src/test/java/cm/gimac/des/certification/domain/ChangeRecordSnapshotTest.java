package cm.gimac.des.certification.domain;

import static cm.gimac.des.certification.domain.ChangeRecordSnapshotTestSamples.*;
import static cm.gimac.des.certification.domain.GoLiveDossierTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.certification.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class ChangeRecordSnapshotTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(ChangeRecordSnapshot.class);
        ChangeRecordSnapshot changeRecordSnapshot1 = getChangeRecordSnapshotSample1();
        ChangeRecordSnapshot changeRecordSnapshot2 = new ChangeRecordSnapshot();
        assertThat(changeRecordSnapshot1).isNotEqualTo(changeRecordSnapshot2);

        changeRecordSnapshot2.setId(changeRecordSnapshot1.getId());
        assertThat(changeRecordSnapshot1).isEqualTo(changeRecordSnapshot2);

        changeRecordSnapshot2 = getChangeRecordSnapshotSample2();
        assertThat(changeRecordSnapshot1).isNotEqualTo(changeRecordSnapshot2);
    }

    @Test
    void dossierTest() {
        ChangeRecordSnapshot changeRecordSnapshot = getChangeRecordSnapshotRandomSampleGenerator();
        GoLiveDossier goLiveDossierBack = getGoLiveDossierRandomSampleGenerator();

        changeRecordSnapshot.setDossier(goLiveDossierBack);
        assertThat(changeRecordSnapshot.getDossier()).isEqualTo(goLiveDossierBack);

        changeRecordSnapshot.dossier(null);
        assertThat(changeRecordSnapshot.getDossier()).isNull();
    }
}
