package cm.gimac.des.notification.service.mapper;

import cm.gimac.des.notification.domain.NotificationSubscription;
import cm.gimac.des.notification.service.dto.NotificationSubscriptionDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link NotificationSubscription} and its DTO {@link NotificationSubscriptionDTO}.
 */
@Mapper(componentModel = "spring")
public interface NotificationSubscriptionMapper extends EntityMapper<NotificationSubscriptionDTO, NotificationSubscription> {}
