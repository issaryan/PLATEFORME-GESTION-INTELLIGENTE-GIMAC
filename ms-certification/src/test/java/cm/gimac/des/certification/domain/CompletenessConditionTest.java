package cm.gimac.des.certification.domain;

import static cm.gimac.des.certification.domain.CompletenessConditionTestSamples.*;
import static cm.gimac.des.certification.domain.GoLiveDossierTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.certification.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class CompletenessConditionTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(CompletenessCondition.class);
        CompletenessCondition completenessCondition1 = getCompletenessConditionSample1();
        CompletenessCondition completenessCondition2 = new CompletenessCondition();
        assertThat(completenessCondition1).isNotEqualTo(completenessCondition2);

        completenessCondition2.setId(completenessCondition1.getId());
        assertThat(completenessCondition1).isEqualTo(completenessCondition2);

        completenessCondition2 = getCompletenessConditionSample2();
        assertThat(completenessCondition1).isNotEqualTo(completenessCondition2);
    }

    @Test
    void dossierTest() {
        CompletenessCondition completenessCondition = getCompletenessConditionRandomSampleGenerator();
        GoLiveDossier goLiveDossierBack = getGoLiveDossierRandomSampleGenerator();

        completenessCondition.setDossier(goLiveDossierBack);
        assertThat(completenessCondition.getDossier()).isEqualTo(goLiveDossierBack);

        completenessCondition.dossier(null);
        assertThat(completenessCondition.getDossier()).isNull();
    }
}
