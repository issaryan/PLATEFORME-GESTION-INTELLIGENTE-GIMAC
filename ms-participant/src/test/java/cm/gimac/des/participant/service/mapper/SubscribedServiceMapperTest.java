package cm.gimac.des.participant.service.mapper;

import static cm.gimac.des.participant.domain.SubscribedServiceAsserts.*;
import static cm.gimac.des.participant.domain.SubscribedServiceTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SubscribedServiceMapperTest {

    private SubscribedServiceMapper subscribedServiceMapper;

    @BeforeEach
    void setUp() {
        subscribedServiceMapper = new SubscribedServiceMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getSubscribedServiceSample1();
        var actual = subscribedServiceMapper.toEntity(subscribedServiceMapper.toDto(expected));
        assertSubscribedServiceAllPropertiesEquals(expected, actual);
    }
}
