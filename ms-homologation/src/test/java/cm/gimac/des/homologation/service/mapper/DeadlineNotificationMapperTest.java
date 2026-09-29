package cm.gimac.des.homologation.service.mapper;

import static cm.gimac.des.homologation.domain.DeadlineNotificationAsserts.*;
import static cm.gimac.des.homologation.domain.DeadlineNotificationTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DeadlineNotificationMapperTest {

    private DeadlineNotificationMapper deadlineNotificationMapper;

    @BeforeEach
    void setUp() {
        deadlineNotificationMapper = new DeadlineNotificationMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getDeadlineNotificationSample1();
        var actual = deadlineNotificationMapper.toEntity(deadlineNotificationMapper.toDto(expected));
        assertDeadlineNotificationAllPropertiesEquals(expected, actual);
    }
}
