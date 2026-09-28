package cm.gimac.des.incident.service.mapper;

import static cm.gimac.des.incident.domain.RemediationActionAsserts.*;
import static cm.gimac.des.incident.domain.RemediationActionTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RemediationActionMapperTest {

    private RemediationActionMapper remediationActionMapper;

    @BeforeEach
    void setUp() {
        remediationActionMapper = new RemediationActionMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getRemediationActionSample1();
        var actual = remediationActionMapper.toEntity(remediationActionMapper.toDto(expected));
        assertRemediationActionAllPropertiesEquals(expected, actual);
    }
}
