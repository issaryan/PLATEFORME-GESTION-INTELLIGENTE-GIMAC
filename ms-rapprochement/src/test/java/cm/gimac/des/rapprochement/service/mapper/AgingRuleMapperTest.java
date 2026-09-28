package cm.gimac.des.rapprochement.service.mapper;

import static cm.gimac.des.rapprochement.domain.AgingRuleAsserts.*;
import static cm.gimac.des.rapprochement.domain.AgingRuleTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AgingRuleMapperTest {

    private AgingRuleMapper agingRuleMapper;

    @BeforeEach
    void setUp() {
        agingRuleMapper = new AgingRuleMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getAgingRuleSample1();
        var actual = agingRuleMapper.toEntity(agingRuleMapper.toDto(expected));
        assertAgingRuleAllPropertiesEquals(expected, actual);
    }
}
