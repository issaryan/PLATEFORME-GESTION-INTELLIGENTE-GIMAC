package cm.gimac.des.document.domain;

import static cm.gimac.des.document.domain.DocumentTestSamples.*;
import static cm.gimac.des.document.domain.PurgeRequestTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.document.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class PurgeRequestTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(PurgeRequest.class);
        PurgeRequest purgeRequest1 = getPurgeRequestSample1();
        PurgeRequest purgeRequest2 = new PurgeRequest();
        assertThat(purgeRequest1).isNotEqualTo(purgeRequest2);

        purgeRequest2.setId(purgeRequest1.getId());
        assertThat(purgeRequest1).isEqualTo(purgeRequest2);

        purgeRequest2 = getPurgeRequestSample2();
        assertThat(purgeRequest1).isNotEqualTo(purgeRequest2);
    }

    @Test
    void documentTest() {
        PurgeRequest purgeRequest = getPurgeRequestRandomSampleGenerator();
        Document documentBack = getDocumentRandomSampleGenerator();

        purgeRequest.setDocument(documentBack);
        assertThat(purgeRequest.getDocument()).isEqualTo(documentBack);

        purgeRequest.document(null);
        assertThat(purgeRequest.getDocument()).isNull();
    }
}
