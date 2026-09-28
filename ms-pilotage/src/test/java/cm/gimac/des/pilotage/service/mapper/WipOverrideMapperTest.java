package cm.gimac.des.pilotage.service.mapper;

import static cm.gimac.des.pilotage.domain.WipOverrideAsserts.*;
import static cm.gimac.des.pilotage.domain.WipOverrideTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WipOverrideMapperTest {

    private WipOverrideMapper wipOverrideMapper;

    @BeforeEach
    void setUp() {
        wipOverrideMapper = new WipOverrideMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getWipOverrideSample1();
        var actual = wipOverrideMapper.toEntity(wipOverrideMapper.toDto(expected));
        assertWipOverrideAllPropertiesEquals(expected, actual);
    }
}
