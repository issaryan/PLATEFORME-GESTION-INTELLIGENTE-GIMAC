package cm.gimac.des.incident.service.mapper;

import static cm.gimac.des.incident.domain.TransitionAsserts.*;
import static cm.gimac.des.incident.domain.TransitionTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TransitionMapperTest {

    private TransitionMapper transitionMapper;

    @BeforeEach
    void setUp() {
        transitionMapper = new TransitionMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getTransitionSample1();
        var actual = transitionMapper.toEntity(transitionMapper.toDto(expected));
        assertTransitionAllPropertiesEquals(expected, actual);
    }
}
