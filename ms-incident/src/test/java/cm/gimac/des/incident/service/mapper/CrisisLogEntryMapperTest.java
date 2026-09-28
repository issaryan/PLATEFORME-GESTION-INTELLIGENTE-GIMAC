package cm.gimac.des.incident.service.mapper;

import static cm.gimac.des.incident.domain.CrisisLogEntryAsserts.*;
import static cm.gimac.des.incident.domain.CrisisLogEntryTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CrisisLogEntryMapperTest {

    private CrisisLogEntryMapper crisisLogEntryMapper;

    @BeforeEach
    void setUp() {
        crisisLogEntryMapper = new CrisisLogEntryMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getCrisisLogEntrySample1();
        var actual = crisisLogEntryMapper.toEntity(crisisLogEntryMapper.toDto(expected));
        assertCrisisLogEntryAllPropertiesEquals(expected, actual);
    }
}
