package cm.gimac.des.homologation.service.mapper;

import cm.gimac.des.homologation.domain.DeadlineNotification;
import cm.gimac.des.homologation.service.dto.DeadlineNotificationDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link DeadlineNotification} and its DTO {@link DeadlineNotificationDTO}.
 */
@Mapper(componentModel = "spring")
public interface DeadlineNotificationMapper extends EntityMapper<DeadlineNotificationDTO, DeadlineNotification> {}
