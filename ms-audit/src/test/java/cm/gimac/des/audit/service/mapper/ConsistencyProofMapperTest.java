package cm.gimac.des.audit.service.mapper;

import static cm.gimac.des.audit.domain.ConsistencyProofAsserts.*;
import static cm.gimac.des.audit.domain.ConsistencyProofTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ConsistencyProofMapperTest {

    private ConsistencyProofMapper consistencyProofMapper;

    @BeforeEach
    void setUp() {
        consistencyProofMapper = new ConsistencyProofMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getConsistencyProofSample1();
        var actual = consistencyProofMapper.toEntity(consistencyProofMapper.toDto(expected));
        assertConsistencyProofAllPropertiesEquals(expected, actual);
    }
}
