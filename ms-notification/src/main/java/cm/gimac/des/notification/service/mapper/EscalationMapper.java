package cm.gimac.des.notification.service.mapper;

import cm.gimac.des.notification.domain.Escalation;
import cm.gimac.des.notification.domain.Notification;
import cm.gimac.des.notification.service.dto.EscalationDTO;
import cm.gimac.des.notification.service.dto.NotificationDTO;
import java.util.Objects;
import java.util.UUID;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link Escalation} and its DTO {@link EscalationDTO}.
 */
@Mapper(componentModel = "spring")
public interface EscalationMapper extends EntityMapper<EscalationDTO, Escalation> {
    @Mapping(target = "notification", source = "notification", qualifiedByName = "notificationId")
    EscalationDTO toDto(Escalation s);

    @Named("notificationId")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    NotificationDTO toDtoNotificationId(Notification notification);

    default String map(UUID value) {
        return Objects.toString(value, null);
    }
}
