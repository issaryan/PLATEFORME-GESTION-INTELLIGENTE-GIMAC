package cm.gimac.des.rapprochement.service.mapper;

import static cm.gimac.des.rapprochement.domain.ReconciliationAsserts.*;
import static cm.gimac.des.rapprochement.domain.ReconciliationTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ReconciliationMapperTest {

    private ReconciliationMapper reconciliationMapper;

    @BeforeEach
    void setUp() {
        reconciliationMapper = new ReconciliationMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getReconciliationSample1();
        var actual = reconciliationMapper.toEntity(reconciliationMapper.toDto(expected));
        assertReconciliationAllPropertiesEquals(expected, actual);
    }
}
