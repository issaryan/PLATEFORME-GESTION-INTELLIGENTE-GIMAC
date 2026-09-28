package cm.gimac.des.incident.service.mapper;

import static cm.gimac.des.incident.domain.ServiceCommitmentAsserts.*;
import static cm.gimac.des.incident.domain.ServiceCommitmentTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ServiceCommitmentMapperTest {

    private ServiceCommitmentMapper serviceCommitmentMapper;

    @BeforeEach
    void setUp() {
        serviceCommitmentMapper = new ServiceCommitmentMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getServiceCommitmentSample1();
        var actual = serviceCommitmentMapper.toEntity(serviceCommitmentMapper.toDto(expected));
        assertServiceCommitmentAllPropertiesEquals(expected, actual);
    }
}
