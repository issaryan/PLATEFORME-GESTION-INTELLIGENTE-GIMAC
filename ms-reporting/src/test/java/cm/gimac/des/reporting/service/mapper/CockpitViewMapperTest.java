package cm.gimac.des.reporting.service.mapper;

import static cm.gimac.des.reporting.domain.CockpitViewAsserts.*;
import static cm.gimac.des.reporting.domain.CockpitViewTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CockpitViewMapperTest {

    private CockpitViewMapper cockpitViewMapper;

    @BeforeEach
    void setUp() {
        cockpitViewMapper = new CockpitViewMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getCockpitViewSample1();
        var actual = cockpitViewMapper.toEntity(cockpitViewMapper.toDto(expected));
        assertCockpitViewAllPropertiesEquals(expected, actual);
    }
}
