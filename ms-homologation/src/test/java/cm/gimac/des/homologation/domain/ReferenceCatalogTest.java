package cm.gimac.des.homologation.domain;

import static cm.gimac.des.homologation.domain.ReferenceCatalogTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.homologation.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class ReferenceCatalogTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(ReferenceCatalog.class);
        ReferenceCatalog referenceCatalog1 = getReferenceCatalogSample1();
        ReferenceCatalog referenceCatalog2 = new ReferenceCatalog();
        assertThat(referenceCatalog1).isNotEqualTo(referenceCatalog2);

        referenceCatalog2.setId(referenceCatalog1.getId());
        assertThat(referenceCatalog1).isEqualTo(referenceCatalog2);

        referenceCatalog2 = getReferenceCatalogSample2();
        assertThat(referenceCatalog1).isNotEqualTo(referenceCatalog2);
    }
}
