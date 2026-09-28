package cm.gimac.des.monitoring.service.mapper;

import cm.gimac.des.monitoring.domain.OperatorUnavailability;
import cm.gimac.des.monitoring.service.dto.OperatorUnavailabilityDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link OperatorUnavailability} and its DTO {@link OperatorUnavailabilityDTO}.
 */
@Mapper(componentModel = "spring")
public interface OperatorUnavailabilityMapper extends EntityMapper<OperatorUnavailabilityDTO, OperatorUnavailability> {}
