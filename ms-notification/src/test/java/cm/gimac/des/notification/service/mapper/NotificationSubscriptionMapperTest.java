package cm.gimac.des.notification.service.mapper;

import static cm.gimac.des.notification.domain.NotificationSubscriptionAsserts.*;
import static cm.gimac.des.notification.domain.NotificationSubscriptionTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NotificationSubscriptionMapperTest {

    private NotificationSubscriptionMapper notificationSubscriptionMapper;

    @BeforeEach
    void setUp() {
        notificationSubscriptionMapper = new NotificationSubscriptionMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getNotificationSubscriptionSample1();
        var actual = notificationSubscriptionMapper.toEntity(notificationSubscriptionMapper.toDto(expected));
        assertNotificationSubscriptionAllPropertiesEquals(expected, actual);
    }
}
