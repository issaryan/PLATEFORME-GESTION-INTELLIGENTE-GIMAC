package cm.gimac.des.participant.service.mapper;

import static cm.gimac.des.participant.domain.InteractionAsserts.*;
import static cm.gimac.des.participant.domain.InteractionTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InteractionMapperTest {

    private InteractionMapper interactionMapper;

    @BeforeEach
    void setUp() {
        interactionMapper = new InteractionMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getInteractionSample1();
        var actual = interactionMapper.toEntity(interactionMapper.toDto(expected));
        assertInteractionAllPropertiesEquals(expected, actual);
    }
}
