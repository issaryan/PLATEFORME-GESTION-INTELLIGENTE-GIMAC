package cm.gimac.des.notification.service.mapper;

import cm.gimac.des.notification.domain.DeliveryTrace;
import cm.gimac.des.notification.domain.MessageTemplate;
import cm.gimac.des.notification.domain.Notification;
import cm.gimac.des.notification.service.dto.DeliveryTraceDTO;
import cm.gimac.des.notification.service.dto.MessageTemplateDTO;
import cm.gimac.des.notification.service.dto.NotificationDTO;
import java.util.Objects;
import java.util.UUID;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link DeliveryTrace} and its DTO {@link DeliveryTraceDTO}.
 */
@Mapper(componentModel = "spring")
public interface DeliveryTraceMapper extends EntityMapper<DeliveryTraceDTO, DeliveryTrace> {
    @Mapping(target = "notification", source = "notification", qualifiedByName = "notificationId")
    @Mapping(target = "template", source = "template", qualifiedByName = "messageTemplateId")
    DeliveryTraceDTO toDto(DeliveryTrace s);

    @Named("notificationId")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    NotificationDTO toDtoNotificationId(Notification notification);

    @Named("messageTemplateId")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    MessageTemplateDTO toDtoMessageTemplateId(MessageTemplate messageTemplate);

    default String map(UUID value) {
        return Objects.toString(value, null);
    }
}
