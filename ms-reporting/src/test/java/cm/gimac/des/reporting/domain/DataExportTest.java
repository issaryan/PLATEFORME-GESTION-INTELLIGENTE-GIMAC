package cm.gimac.des.reporting.domain;

import static cm.gimac.des.reporting.domain.DataExportTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.reporting.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class DataExportTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(DataExport.class);
        DataExport dataExport1 = getDataExportSample1();
        DataExport dataExport2 = new DataExport();
        assertThat(dataExport1).isNotEqualTo(dataExport2);

        dataExport2.setId(dataExport1.getId());
        assertThat(dataExport1).isEqualTo(dataExport2);

        dataExport2 = getDataExportSample2();
        assertThat(dataExport1).isNotEqualTo(dataExport2);
    }
}
