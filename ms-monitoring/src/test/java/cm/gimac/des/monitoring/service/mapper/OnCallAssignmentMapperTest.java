package cm.gimac.des.monitoring.service.mapper;

import static cm.gimac.des.monitoring.domain.OnCallAssignmentAsserts.*;
import static cm.gimac.des.monitoring.domain.OnCallAssignmentTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OnCallAssignmentMapperTest {

    private OnCallAssignmentMapper onCallAssignmentMapper;

    @BeforeEach
    void setUp() {
        onCallAssignmentMapper = new OnCallAssignmentMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getOnCallAssignmentSample1();
        var actual = onCallAssignmentMapper.toEntity(onCallAssignmentMapper.toDto(expected));
        assertOnCallAssignmentAllPropertiesEquals(expected, actual);
    }
}
