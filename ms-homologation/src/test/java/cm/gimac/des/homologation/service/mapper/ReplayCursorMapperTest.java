package cm.gimac.des.homologation.service.mapper;

import static cm.gimac.des.homologation.domain.ReplayCursorAsserts.*;
import static cm.gimac.des.homologation.domain.ReplayCursorTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ReplayCursorMapperTest {

    private ReplayCursorMapper replayCursorMapper;

    @BeforeEach
    void setUp() {
        replayCursorMapper = new ReplayCursorMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getReplayCursorSample1();
        var actual = replayCursorMapper.toEntity(replayCursorMapper.toDto(expected));
        assertReplayCursorAllPropertiesEquals(expected, actual);
    }
}
