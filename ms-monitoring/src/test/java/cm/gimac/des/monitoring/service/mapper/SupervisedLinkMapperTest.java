package cm.gimac.des.monitoring.service.mapper;

import static cm.gimac.des.monitoring.domain.SupervisedLinkAsserts.*;
import static cm.gimac.des.monitoring.domain.SupervisedLinkTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SupervisedLinkMapperTest {

    private SupervisedLinkMapper supervisedLinkMapper;

    @BeforeEach
    void setUp() {
        supervisedLinkMapper = new SupervisedLinkMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getSupervisedLinkSample1();
        var actual = supervisedLinkMapper.toEntity(supervisedLinkMapper.toDto(expected));
        assertSupervisedLinkAllPropertiesEquals(expected, actual);
    }
}
