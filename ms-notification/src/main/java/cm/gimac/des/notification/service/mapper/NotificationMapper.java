package cm.gimac.des.notification.service.mapper;

import cm.gimac.des.notification.domain.ConsolidationWindow;
import cm.gimac.des.notification.domain.Notification;
import cm.gimac.des.notification.service.dto.ConsolidationWindowDTO;
import cm.gimac.des.notification.service.dto.NotificationDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link Notification} and its DTO {@link NotificationDTO}.
 */
@Mapper(componentModel = "spring")
public interface NotificationMapper extends EntityMapper<NotificationDTO, Notification> {
    @Mapping(target = "consolidationWindow", source = "consolidationWindow", qualifiedByName = "consolidationWindowGroupKey")
    NotificationDTO toDto(Notification s);

    @Named("consolidationWindowGroupKey")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "groupKey", source = "groupKey")
    ConsolidationWindowDTO toDtoConsolidationWindowGroupKey(ConsolidationWindow consolidationWindow);
}
