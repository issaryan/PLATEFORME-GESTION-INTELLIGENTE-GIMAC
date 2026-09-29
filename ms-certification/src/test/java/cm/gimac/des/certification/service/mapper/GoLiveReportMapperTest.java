package cm.gimac.des.certification.service.mapper;

import static cm.gimac.des.certification.domain.GoLiveReportAsserts.*;
import static cm.gimac.des.certification.domain.GoLiveReportTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GoLiveReportMapperTest {

    private GoLiveReportMapper goLiveReportMapper;

    @BeforeEach
    void setUp() {
        goLiveReportMapper = new GoLiveReportMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getGoLiveReportSample1();
        var actual = goLiveReportMapper.toEntity(goLiveReportMapper.toDto(expected));
        assertGoLiveReportAllPropertiesEquals(expected, actual);
    }
}
