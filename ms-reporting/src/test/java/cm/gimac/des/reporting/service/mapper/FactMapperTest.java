package cm.gimac.des.reporting.service.mapper;

import static cm.gimac.des.reporting.domain.FactAsserts.*;
import static cm.gimac.des.reporting.domain.FactTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FactMapperTest {

    private FactMapper factMapper;

    @BeforeEach
    void setUp() {
        factMapper = new FactMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getFactSample1();
        var actual = factMapper.toEntity(factMapper.toDto(expected));
        assertFactAllPropertiesEquals(expected, actual);
    }
}
