package cm.gimac.des.incident.service.mapper;

import cm.gimac.des.incident.domain.AlertObservation;
import cm.gimac.des.incident.service.dto.AlertObservationDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link AlertObservation} and its DTO {@link AlertObservationDTO}.
 */
@Mapper(componentModel = "spring")
public interface AlertObservationMapper extends EntityMapper<AlertObservationDTO, AlertObservation> {}
