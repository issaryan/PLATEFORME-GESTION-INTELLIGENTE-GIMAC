package cm.gimac.des.incident.service.mapper;

import static cm.gimac.des.incident.domain.EngagementFreezeAsserts.*;
import static cm.gimac.des.incident.domain.EngagementFreezeTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EngagementFreezeMapperTest {

    private EngagementFreezeMapper engagementFreezeMapper;

    @BeforeEach
    void setUp() {
        engagementFreezeMapper = new EngagementFreezeMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getEngagementFreezeSample1();
        var actual = engagementFreezeMapper.toEntity(engagementFreezeMapper.toDto(expected));
        assertEngagementFreezeAllPropertiesEquals(expected, actual);
    }
}
