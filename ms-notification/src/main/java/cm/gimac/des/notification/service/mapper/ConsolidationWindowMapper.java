package cm.gimac.des.notification.service.mapper;

import cm.gimac.des.notification.domain.ConsolidationWindow;
import cm.gimac.des.notification.domain.Notification;
import cm.gimac.des.notification.service.dto.ConsolidationWindowDTO;
import cm.gimac.des.notification.service.dto.NotificationDTO;
import java.util.Objects;
import java.util.UUID;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link ConsolidationWindow} and its DTO {@link ConsolidationWindowDTO}.
 */
@Mapper(componentModel = "spring")
public interface ConsolidationWindowMapper extends EntityMapper<ConsolidationWindowDTO, ConsolidationWindow> {
    @Mapping(target = "summaryNotification", source = "summaryNotification", qualifiedByName = "notificationId")
    ConsolidationWindowDTO toDto(ConsolidationWindow s);

    @Named("notificationId")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    NotificationDTO toDtoNotificationId(Notification notification);

    default String map(UUID value) {
        return Objects.toString(value, null);
    }
}
