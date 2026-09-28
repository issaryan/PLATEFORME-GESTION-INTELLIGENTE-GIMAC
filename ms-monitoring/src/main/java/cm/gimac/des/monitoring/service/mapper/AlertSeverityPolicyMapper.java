package cm.gimac.des.monitoring.service.mapper;

import cm.gimac.des.monitoring.domain.AlertSeverityPolicy;
import cm.gimac.des.monitoring.service.dto.AlertSeverityPolicyDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link AlertSeverityPolicy} and its DTO {@link AlertSeverityPolicyDTO}.
 */
@Mapper(componentModel = "spring")
public interface AlertSeverityPolicyMapper extends EntityMapper<AlertSeverityPolicyDTO, AlertSeverityPolicy> {}
