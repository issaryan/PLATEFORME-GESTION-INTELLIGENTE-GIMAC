package cm.gimac.des.homologation.service.mapper;

import static cm.gimac.des.homologation.domain.OperationAsserts.*;
import static cm.gimac.des.homologation.domain.OperationTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OperationMapperTest {

    private OperationMapper operationMapper;

    @BeforeEach
    void setUp() {
        operationMapper = new OperationMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getOperationSample1();
        var actual = operationMapper.toEntity(operationMapper.toDto(expected));
        assertOperationAllPropertiesEquals(expected, actual);
    }
}
