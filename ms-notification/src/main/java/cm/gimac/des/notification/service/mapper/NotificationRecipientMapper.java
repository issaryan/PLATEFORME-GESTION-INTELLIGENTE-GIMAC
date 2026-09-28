package cm.gimac.des.notification.service.mapper;

import cm.gimac.des.notification.domain.Notification;
import cm.gimac.des.notification.domain.NotificationRecipient;
import cm.gimac.des.notification.domain.NotificationSubscription;
import cm.gimac.des.notification.service.dto.NotificationDTO;
import cm.gimac.des.notification.service.dto.NotificationRecipientDTO;
import cm.gimac.des.notification.service.dto.NotificationSubscriptionDTO;
import java.util.Objects;
import java.util.UUID;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link NotificationRecipient} and its DTO {@link NotificationRecipientDTO}.
 */
@Mapper(componentModel = "spring")
public interface NotificationRecipientMapper extends EntityMapper<NotificationRecipientDTO, NotificationRecipient> {
    @Mapping(target = "notification", source = "notification", qualifiedByName = "notificationId")
    @Mapping(target = "sourceSubscription", source = "sourceSubscription", qualifiedByName = "notificationSubscriptionId")
    NotificationRecipientDTO toDto(NotificationRecipient s);

    @Named("notificationId")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    NotificationDTO toDtoNotificationId(Notification notification);

    @Named("notificationSubscriptionId")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    NotificationSubscriptionDTO toDtoNotificationSubscriptionId(NotificationSubscription notificationSubscription);

    default String map(UUID value) {
        return Objects.toString(value, null);
    }
}
