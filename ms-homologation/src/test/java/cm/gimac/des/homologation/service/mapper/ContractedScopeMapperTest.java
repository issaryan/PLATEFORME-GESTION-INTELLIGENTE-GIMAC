package cm.gimac.des.homologation.service.mapper;

import static cm.gimac.des.homologation.domain.ContractedScopeAsserts.*;
import static cm.gimac.des.homologation.domain.ContractedScopeTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ContractedScopeMapperTest {

    private ContractedScopeMapper contractedScopeMapper;

    @BeforeEach
    void setUp() {
        contractedScopeMapper = new ContractedScopeMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getContractedScopeSample1();
        var actual = contractedScopeMapper.toEntity(contractedScopeMapper.toDto(expected));
        assertContractedScopeAllPropertiesEquals(expected, actual);
    }
}
