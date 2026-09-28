package cm.gimac.des.iam.service.mapper;

import cm.gimac.des.iam.domain.Permission;
import cm.gimac.des.iam.domain.Role;
import cm.gimac.des.iam.service.dto.PermissionDTO;
import cm.gimac.des.iam.service.dto.RoleDTO;
import java.util.Set;
import java.util.stream.Collectors;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link Role} and its DTO {@link RoleDTO}.
 */
@Mapper(componentModel = "spring")
public interface RoleMapper extends EntityMapper<RoleDTO, Role> {
    @Mapping(target = "permissionses", source = "permissionses", qualifiedByName = "permissionCodeSet")
    RoleDTO toDto(Role s);

    @Mapping(target = "removePermissions", ignore = true)
    Role toEntity(RoleDTO roleDTO);

    @Named("permissionCode")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "code", source = "code")
    PermissionDTO toDtoPermissionCode(Permission permission);

    @Named("permissionCodeSet")
    default Set<PermissionDTO> toDtoPermissionCodeSet(Set<Permission> permission) {
        return permission.stream().map(this::toDtoPermissionCode).collect(Collectors.toSet());
    }
}
