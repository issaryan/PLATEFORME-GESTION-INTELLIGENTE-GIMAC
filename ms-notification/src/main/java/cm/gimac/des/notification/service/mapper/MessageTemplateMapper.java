package cm.gimac.des.notification.service.mapper;

import cm.gimac.des.notification.domain.MessageTemplate;
import cm.gimac.des.notification.service.dto.MessageTemplateDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link MessageTemplate} and its DTO {@link MessageTemplateDTO}.
 */
@Mapper(componentModel = "spring")
public interface MessageTemplateMapper extends EntityMapper<MessageTemplateDTO, MessageTemplate> {}
