package cm.gimac.des.incident.service.mapper;

import static cm.gimac.des.incident.domain.IncidentAsserts.*;
import static cm.gimac.des.incident.domain.IncidentTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class IncidentMapperTest {

    private IncidentMapper incidentMapper;

    @BeforeEach
    void setUp() {
        incidentMapper = new IncidentMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getIncidentSample1();
        var actual = incidentMapper.toEntity(incidentMapper.toDto(expected));
        assertIncidentAllPropertiesEquals(expected, actual);
    }
}
