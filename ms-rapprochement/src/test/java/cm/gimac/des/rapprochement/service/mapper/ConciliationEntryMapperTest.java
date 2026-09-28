package cm.gimac.des.rapprochement.service.mapper;

import static cm.gimac.des.rapprochement.domain.ConciliationEntryAsserts.*;
import static cm.gimac.des.rapprochement.domain.ConciliationEntryTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ConciliationEntryMapperTest {

    private ConciliationEntryMapper conciliationEntryMapper;

    @BeforeEach
    void setUp() {
        conciliationEntryMapper = new ConciliationEntryMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getConciliationEntrySample1();
        var actual = conciliationEntryMapper.toEntity(conciliationEntryMapper.toDto(expected));
        assertConciliationEntryAllPropertiesEquals(expected, actual);
    }
}
