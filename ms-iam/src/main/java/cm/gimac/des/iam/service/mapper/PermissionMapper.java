package cm.gimac.des.iam.service.mapper;

import cm.gimac.des.iam.domain.Permission;
import cm.gimac.des.iam.domain.Role;
import cm.gimac.des.iam.service.dto.PermissionDTO;
import cm.gimac.des.iam.service.dto.RoleDTO;
import java.util.Set;
import java.util.stream.Collectors;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link Permission} and its DTO {@link PermissionDTO}.
 */
@Mapper(componentModel = "spring")
public interface PermissionMapper extends EntityMapper<PermissionDTO, Permission> {
    @Mapping(target = "roleses", source = "roleses", qualifiedByName = "roleCodeSet")
    PermissionDTO toDto(Permission s);

    @Mapping(target = "roleses", ignore = true)
    @Mapping(target = "removeRoles", ignore = true)
    Permission toEntity(PermissionDTO permissionDTO);

    @Named("roleCode")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "code", source = "code")
    RoleDTO toDtoRoleCode(Role role);

    @Named("roleCodeSet")
    default Set<RoleDTO> toDtoRoleCodeSet(Set<Role> role) {
        return role.stream().map(this::toDtoRoleCode).collect(Collectors.toSet());
    }
}
