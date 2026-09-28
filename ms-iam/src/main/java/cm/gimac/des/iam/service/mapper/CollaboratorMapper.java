package cm.gimac.des.iam.service.mapper;

import cm.gimac.des.iam.domain.Collaborator;
import cm.gimac.des.iam.service.dto.CollaboratorDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link Collaborator} and its DTO {@link CollaboratorDTO}.
 */
@Mapper(componentModel = "spring")
public interface CollaboratorMapper extends EntityMapper<CollaboratorDTO, Collaborator> {}
