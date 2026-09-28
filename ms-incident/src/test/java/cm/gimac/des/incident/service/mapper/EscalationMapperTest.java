package cm.gimac.des.incident.service.mapper;

import static cm.gimac.des.incident.domain.EscalationAsserts.*;
import static cm.gimac.des.incident.domain.EscalationTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EscalationMapperTest {

    private EscalationMapper escalationMapper;

    @BeforeEach
    void setUp() {
        escalationMapper = new EscalationMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getEscalationSample1();
        var actual = escalationMapper.toEntity(escalationMapper.toDto(expected));
        assertEscalationAllPropertiesEquals(expected, actual);
    }
}
