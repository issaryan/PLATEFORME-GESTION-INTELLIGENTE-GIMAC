package cm.gimac.des.notification.service.mapper;

import static cm.gimac.des.notification.domain.NotificationRecipientAsserts.*;
import static cm.gimac.des.notification.domain.NotificationRecipientTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NotificationRecipientMapperTest {

    private NotificationRecipientMapper notificationRecipientMapper;

    @BeforeEach
    void setUp() {
        notificationRecipientMapper = new NotificationRecipientMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getNotificationRecipientSample1();
        var actual = notificationRecipientMapper.toEntity(notificationRecipientMapper.toDto(expected));
        assertNotificationRecipientAllPropertiesEquals(expected, actual);
    }
}
