package cm.gimac.des.pilotage.service.mapper;

import cm.gimac.des.pilotage.domain.CollaboratorProjection;
import cm.gimac.des.pilotage.service.dto.CollaboratorProjectionDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link CollaboratorProjection} and its DTO {@link CollaboratorProjectionDTO}.
 */
@Mapper(componentModel = "spring")
public interface CollaboratorProjectionMapper extends EntityMapper<CollaboratorProjectionDTO, CollaboratorProjection> {}
