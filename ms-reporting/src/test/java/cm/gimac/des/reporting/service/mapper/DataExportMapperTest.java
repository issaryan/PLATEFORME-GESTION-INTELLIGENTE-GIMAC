package cm.gimac.des.reporting.service.mapper;

import static cm.gimac.des.reporting.domain.DataExportAsserts.*;
import static cm.gimac.des.reporting.domain.DataExportTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DataExportMapperTest {

    private DataExportMapper dataExportMapper;

    @BeforeEach
    void setUp() {
        dataExportMapper = new DataExportMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getDataExportSample1();
        var actual = dataExportMapper.toEntity(dataExportMapper.toDto(expected));
        assertDataExportAllPropertiesEquals(expected, actual);
    }
}
