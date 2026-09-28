package cm.gimac.des.pilotage.service.mapper;

import cm.gimac.des.pilotage.domain.AbsenceProjection;
import cm.gimac.des.pilotage.service.dto.AbsenceProjectionDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link AbsenceProjection} and its DTO {@link AbsenceProjectionDTO}.
 */
@Mapper(componentModel = "spring")
public interface AbsenceProjectionMapper extends EntityMapper<AbsenceProjectionDTO, AbsenceProjection> {}
