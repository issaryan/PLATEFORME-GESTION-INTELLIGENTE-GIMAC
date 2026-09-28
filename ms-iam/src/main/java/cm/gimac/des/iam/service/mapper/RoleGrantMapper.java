package cm.gimac.des.iam.service.mapper;

import cm.gimac.des.iam.domain.Collaborator;
import cm.gimac.des.iam.domain.Role;
import cm.gimac.des.iam.domain.RoleGrant;
import cm.gimac.des.iam.service.dto.CollaboratorDTO;
import cm.gimac.des.iam.service.dto.RoleDTO;
import cm.gimac.des.iam.service.dto.RoleGrantDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link RoleGrant} and its DTO {@link RoleGrantDTO}.
 */
@Mapper(componentModel = "spring")
public interface RoleGrantMapper extends EntityMapper<RoleGrantDTO, RoleGrant> {
    @Mapping(target = "collaborator", source = "collaborator", qualifiedByName = "collaboratorFederatedId")
    @Mapping(target = "role", source = "role", qualifiedByName = "roleCode")
    RoleGrantDTO toDto(RoleGrant s);

    @Named("collaboratorFederatedId")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "federatedId", source = "federatedId")
    CollaboratorDTO toDtoCollaboratorFederatedId(Collaborator collaborator);

    @Named("roleCode")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "code", source = "code")
    RoleDTO toDtoRoleCode(Role role);
}
