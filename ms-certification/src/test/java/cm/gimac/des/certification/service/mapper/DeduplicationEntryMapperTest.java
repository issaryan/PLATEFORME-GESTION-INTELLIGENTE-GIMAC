package cm.gimac.des.certification.service.mapper;

import static cm.gimac.des.certification.domain.DeduplicationEntryAsserts.*;
import static cm.gimac.des.certification.domain.DeduplicationEntryTestSamples.*;

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
