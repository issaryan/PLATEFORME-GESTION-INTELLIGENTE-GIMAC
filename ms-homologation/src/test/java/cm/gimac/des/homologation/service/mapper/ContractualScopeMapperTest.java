package cm.gimac.des.homologation.service.mapper;

import static cm.gimac.des.homologation.domain.ContractualScopeAsserts.*;
import static cm.gimac.des.homologation.domain.ContractualScopeTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ContractualScopeMapperTest {

    private ContractualScopeMapper contractualScopeMapper;

    @BeforeEach
    void setUp() {
        contractualScopeMapper = new ContractualScopeMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getContractualScopeSample1();
        var actual = contractualScopeMapper.toEntity(contractualScopeMapper.toDto(expected));
        assertContractualScopeAllPropertiesEquals(expected, actual);
    }
}
