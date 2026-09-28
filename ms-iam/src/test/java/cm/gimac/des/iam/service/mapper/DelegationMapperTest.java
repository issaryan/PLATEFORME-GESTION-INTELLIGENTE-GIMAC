package cm.gimac.des.iam.service.mapper;

import static cm.gimac.des.iam.domain.DelegationAsserts.*;
import static cm.gimac.des.iam.domain.DelegationTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DelegationMapperTest {

    private DelegationMapper delegationMapper;

    @BeforeEach
    void setUp() {
        delegationMapper = new DelegationMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getDelegationSample1();
        var actual = delegationMapper.toEntity(delegationMapper.toDto(expected));
        assertDelegationAllPropertiesEquals(expected, actual);
    }
}
