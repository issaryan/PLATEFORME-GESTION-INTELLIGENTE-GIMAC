package cm.gimac.des.iam.service.mapper;

import cm.gimac.des.iam.domain.Collaborator;
import cm.gimac.des.iam.domain.Delegation;
import cm.gimac.des.iam.service.dto.CollaboratorDTO;
import cm.gimac.des.iam.service.dto.DelegationDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link Delegation} and its DTO {@link DelegationDTO}.
 */
@Mapper(componentModel = "spring")
public interface DelegationMapper extends EntityMapper<DelegationDTO, Delegation> {
    @Mapping(target = "collaborator", source = "collaborator", qualifiedByName = "collaboratorFederatedId")
    DelegationDTO toDto(Delegation s);

    @Named("collaboratorFederatedId")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "federatedId", source = "federatedId")
    CollaboratorDTO toDtoCollaboratorFederatedId(Collaborator collaborator);
}
