package cm.gimac.des.notification.service.mapper;

import static cm.gimac.des.notification.domain.DeliveryTraceAsserts.*;
import static cm.gimac.des.notification.domain.DeliveryTraceTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DeliveryTraceMapperTest {

    private DeliveryTraceMapper deliveryTraceMapper;

    @BeforeEach
    void setUp() {
        deliveryTraceMapper = new DeliveryTraceMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getDeliveryTraceSample1();
        var actual = deliveryTraceMapper.toEntity(deliveryTraceMapper.toDto(expected));
        assertDeliveryTraceAllPropertiesEquals(expected, actual);
    }
}
