package cm.gimac.des.participant.service.mapper;

import static cm.gimac.des.participant.domain.EscalationContactAsserts.*;
import static cm.gimac.des.participant.domain.EscalationContactTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EscalationContactMapperTest {

    private EscalationContactMapper escalationContactMapper;

    @BeforeEach
    void setUp() {
        escalationContactMapper = new EscalationContactMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getEscalationContactSample1();
        var actual = escalationContactMapper.toEntity(escalationContactMapper.toDto(expected));
        assertEscalationContactAllPropertiesEquals(expected, actual);
    }
}
