package cm.gimac.des.audit.service.mapper;

import cm.gimac.des.audit.domain.AuditEvent;
import cm.gimac.des.audit.service.dto.AuditEventDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link AuditEvent} and its DTO {@link AuditEventDTO}.
 */
@Mapper(componentModel = "spring")
public interface AuditEventMapper extends EntityMapper<AuditEventDTO, AuditEvent> {}
