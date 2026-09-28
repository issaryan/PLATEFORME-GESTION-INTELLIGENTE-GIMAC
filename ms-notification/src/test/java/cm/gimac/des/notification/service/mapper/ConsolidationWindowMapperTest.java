package cm.gimac.des.notification.service.mapper;

import static cm.gimac.des.notification.domain.ConsolidationWindowAsserts.*;
import static cm.gimac.des.notification.domain.ConsolidationWindowTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ConsolidationWindowMapperTest {

    private ConsolidationWindowMapper consolidationWindowMapper;

    @BeforeEach
    void setUp() {
        consolidationWindowMapper = new ConsolidationWindowMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getConsolidationWindowSample1();
        var actual = consolidationWindowMapper.toEntity(consolidationWindowMapper.toDto(expected));
        assertConsolidationWindowAllPropertiesEquals(expected, actual);
    }
}
