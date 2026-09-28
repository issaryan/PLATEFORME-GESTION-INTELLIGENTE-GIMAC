package cm.gimac.des.homologation.service.mapper;

import static cm.gimac.des.homologation.domain.DeduplicationEntryAsserts.*;
import static cm.gimac.des.homologation.domain.DeduplicationEntryTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DeduplicationEntryMapperTest {

    private DeduplicationEntryMapper deduplicationEntryMapper;

    @BeforeEach
    void setUp() {
        deduplicationEntryMapper = new DeduplicationEntryMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getDeduplicationEntrySample1();
        var actual = deduplicationEntryMapper.toEntity(deduplicationEntryMapper.toDto(expected));
        assertDeduplicationEntryAllPropertiesEquals(expected, actual);
    }
}
