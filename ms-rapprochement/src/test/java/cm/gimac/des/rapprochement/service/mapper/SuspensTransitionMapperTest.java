package cm.gimac.des.rapprochement.service.mapper;

import static cm.gimac.des.rapprochement.domain.SuspensTransitionAsserts.*;
import static cm.gimac.des.rapprochement.domain.SuspensTransitionTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SuspensTransitionMapperTest {

    private SuspensTransitionMapper suspensTransitionMapper;

    @BeforeEach
    void setUp() {
        suspensTransitionMapper = new SuspensTransitionMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getSuspensTransitionSample1();
        var actual = suspensTransitionMapper.toEntity(suspensTransitionMapper.toDto(expected));
        assertSuspensTransitionAllPropertiesEquals(expected, actual);
    }
}
