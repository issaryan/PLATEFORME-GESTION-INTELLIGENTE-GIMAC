package cm.gimac.des.homologation.domain;

import static cm.gimac.des.homologation.domain.ReferenceCatalogTestSamples.*;
import static cm.gimac.des.homologation.domain.ReferenceOperationTestSamples.*;
import static cm.gimac.des.homologation.domain.ReferenceOperationTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.homologation.web.rest.TestUtil;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ReferenceOperationTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(ReferenceOperation.class);
        ReferenceOperation referenceOperation1 = getReferenceOperationSample1();
        ReferenceOperation referenceOperation2 = new ReferenceOperation();
        assertThat(referenceOperation1).isNotEqualTo(referenceOperation2);

        referenceOperation2.setId(referenceOperation1.getId());
        assertThat(referenceOperation1).isEqualTo(referenceOperation2);

        referenceOperation2 = getReferenceOperationSample2();
        assertThat(referenceOperation1).isNotEqualTo(referenceOperation2);
    }

    @Test
    void catalogTest() {
        ReferenceOperation referenceOperation = getReferenceOperationRandomSampleGenerator();
        ReferenceCatalog referenceCatalogBack = getReferenceCatalogRandomSampleGenerator();

        referenceOperation.setCatalog(referenceCatalogBack);
        assertThat(referenceOperation.getCatalog()).isEqualTo(referenceCatalogBack);

        referenceOperation.catalog(null);
        assertThat(referenceOperation.getCatalog()).isNull();
    }

    @Test
    void predecessorsTest() {
        ReferenceOperation referenceOperation = getReferenceOperationRandomSampleGenerator();
        ReferenceOperation referenceOperationBack = getReferenceOperationRandomSampleGenerator();

        referenceOperation.addPredecessors(referenceOperationBack);
        assertThat(referenceOperation.getPredecessors()).containsOnly(referenceOperationBack);

        referenceOperation.removePredecessors(referenceOperationBack);
        assertThat(referenceOperation.getPredecessors()).doesNotContain(referenceOperationBack);

        referenceOperation.predecessors(new HashSet<>(Set.of(referenceOperationBack)));
        assertThat(referenceOperation.getPredecessors()).containsOnly(referenceOperationBack);

        referenceOperation.setPredecessors(new HashSet<>());
        assertThat(referenceOperation.getPredecessors()).doesNotContain(referenceOperationBack);
    }

    @Test
    void successorsTest() {
        ReferenceOperation referenceOperation = getReferenceOperationRandomSampleGenerator();
        ReferenceOperation referenceOperationBack = getReferenceOperationRandomSampleGenerator();

        referenceOperation.addSuccessors(referenceOperationBack);
        assertThat(referenceOperation.getSuccessors()).containsOnly(referenceOperationBack);
        assertThat(referenceOperationBack.getPredecessors()).containsOnly(referenceOperation);

        referenceOperation.removeSuccessors(referenceOperationBack);
        assertThat(referenceOperation.getSuccessors()).doesNotContain(referenceOperationBack);
        assertThat(referenceOperationBack.getPredecessors()).doesNotContain(referenceOperation);

        referenceOperation.successors(new HashSet<>(Set.of(referenceOperationBack)));
        assertThat(referenceOperation.getSuccessors()).containsOnly(referenceOperationBack);
        assertThat(referenceOperationBack.getPredecessors()).containsOnly(referenceOperation);

        referenceOperation.setSuccessors(new HashSet<>());
        assertThat(referenceOperation.getSuccessors()).doesNotContain(referenceOperationBack);
        assertThat(referenceOperationBack.getPredecessors()).doesNotContain(referenceOperation);
    }
}
