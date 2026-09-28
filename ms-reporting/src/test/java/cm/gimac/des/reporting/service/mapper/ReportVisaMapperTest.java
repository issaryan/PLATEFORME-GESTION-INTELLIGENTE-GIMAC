package cm.gimac.des.reporting.service.mapper;

import static cm.gimac.des.reporting.domain.ReportVisaAsserts.*;
import static cm.gimac.des.reporting.domain.ReportVisaTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ReportVisaMapperTest {

    private ReportVisaMapper reportVisaMapper;

    @BeforeEach
    void setUp() {
        reportVisaMapper = new ReportVisaMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getReportVisaSample1();
        var actual = reportVisaMapper.toEntity(reportVisaMapper.toDto(expected));
        assertReportVisaAllPropertiesEquals(expected, actual);
    }
}
