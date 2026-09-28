package cm.gimac.des.rapprochement.service.mapper;

import static cm.gimac.des.rapprochement.domain.DepositContractAsserts.*;
import static cm.gimac.des.rapprochement.domain.DepositContractTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DepositContractMapperTest {

    private DepositContractMapper depositContractMapper;

    @BeforeEach
    void setUp() {
        depositContractMapper = new DepositContractMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getDepositContractSample1();
        var actual = depositContractMapper.toEntity(depositContractMapper.toDto(expected));
        assertDepositContractAllPropertiesEquals(expected, actual);
    }
}
