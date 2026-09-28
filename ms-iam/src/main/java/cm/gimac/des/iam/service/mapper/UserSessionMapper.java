package cm.gimac.des.iam.service.mapper;

import cm.gimac.des.iam.domain.Collaborator;
import cm.gimac.des.iam.domain.UserSession;
import cm.gimac.des.iam.service.dto.CollaboratorDTO;
import cm.gimac.des.iam.service.dto.UserSessionDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link UserSession} and its DTO {@link UserSessionDTO}.
 */
@Mapper(componentModel = "spring")
public interface UserSessionMapper extends EntityMapper<UserSessionDTO, UserSession> {
    @Mapping(target = "collaborator", source = "collaborator", qualifiedByName = "collaboratorFederatedId")
    UserSessionDTO toDto(UserSession s);

    @Named("collaboratorFederatedId")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "federatedId", source = "federatedId")
    CollaboratorDTO toDtoCollaboratorFederatedId(Collaborator collaborator);
}
