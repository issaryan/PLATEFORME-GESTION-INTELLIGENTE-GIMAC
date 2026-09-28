package cm.gimac.des.iam.service.mapper;

import cm.gimac.des.iam.domain.Assignment;
import cm.gimac.des.iam.domain.Collaborator;
import cm.gimac.des.iam.service.dto.AssignmentDTO;
import cm.gimac.des.iam.service.dto.CollaboratorDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link Assignment} and its DTO {@link AssignmentDTO}.
 */
@Mapper(componentModel = "spring")
public interface AssignmentMapper extends EntityMapper<AssignmentDTO, Assignment> {
    @Mapping(target = "collaborator", source = "collaborator", qualifiedByName = "collaboratorFederatedId")
    AssignmentDTO toDto(Assignment s);

    @Named("collaboratorFederatedId")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "federatedId", source = "federatedId")
    CollaboratorDTO toDtoCollaboratorFederatedId(Collaborator collaborator);
}
