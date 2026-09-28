package cm.gimac.des.monitoring.service.mapper;

import static cm.gimac.des.monitoring.domain.ReportSignatureAsserts.*;
import static cm.gimac.des.monitoring.domain.ReportSignatureTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ReportSignatureMapperTest {

    private ReportSignatureMapper reportSignatureMapper;

    @BeforeEach
    void setUp() {
        reportSignatureMapper = new ReportSignatureMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getReportSignatureSample1();
        var actual = reportSignatureMapper.toEntity(reportSignatureMapper.toDto(expected));
        assertReportSignatureAllPropertiesEquals(expected, actual);
    }
}
