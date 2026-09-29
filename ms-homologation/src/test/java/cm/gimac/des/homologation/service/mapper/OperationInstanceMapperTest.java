package cm.gimac.des.homologation.service.mapper;

import static cm.gimac.des.homologation.domain.OperationInstanceAsserts.*;
import static cm.gimac.des.homologation.domain.OperationInstanceTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OperationInstanceMapperTest {

    private OperationInstanceMapper operationInstanceMapper;

    @BeforeEach
    void setUp() {
        operationInstanceMapper = new OperationInstanceMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getOperationInstanceSample1();
        var actual = operationInstanceMapper.toEntity(operationInstanceMapper.toDto(expected));
        assertOperationInstanceAllPropertiesEquals(expected, actual);
    }
}
