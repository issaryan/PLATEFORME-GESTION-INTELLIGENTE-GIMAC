package cm.gimac.des.homologation.domain;

import static cm.gimac.des.homologation.domain.ClosureRecordTestSamples.*;
import static cm.gimac.des.homologation.domain.OperationInstanceTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.homologation.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class ClosureRecordTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(ClosureRecord.class);
        ClosureRecord closureRecord1 = getClosureRecordSample1();
        ClosureRecord closureRecord2 = new ClosureRecord();
        assertThat(closureRecord1).isNotEqualTo(closureRecord2);

        closureRecord2.setId(closureRecord1.getId());
        assertThat(closureRecord1).isEqualTo(closureRecord2);

        closureRecord2 = getClosureRecordSample2();
        assertThat(closureRecord1).isNotEqualTo(closureRecord2);
    }

    @Test
    void operationTest() {
        ClosureRecord closureRecord = getClosureRecordRandomSampleGenerator();
        OperationInstance operationInstanceBack = getOperationInstanceRandomSampleGenerator();

        closureRecord.setOperation(operationInstanceBack);
        assertThat(closureRecord.getOperation()).isEqualTo(operationInstanceBack);

        closureRecord.operation(null);
        assertThat(closureRecord.getOperation()).isNull();
    }
}
