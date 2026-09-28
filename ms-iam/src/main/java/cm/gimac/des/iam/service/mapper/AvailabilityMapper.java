package cm.gimac.des.iam.service.mapper;

import cm.gimac.des.iam.domain.Availability;
import cm.gimac.des.iam.domain.Collaborator;
import cm.gimac.des.iam.service.dto.AvailabilityDTO;
import cm.gimac.des.iam.service.dto.CollaboratorDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link Availability} and its DTO {@link AvailabilityDTO}.
 */
@Mapper(componentModel = "spring")
public interface AvailabilityMapper extends EntityMapper<AvailabilityDTO, Availability> {
    @Mapping(target = "collaborator", source = "collaborator", qualifiedByName = "collaboratorFederatedId")
    AvailabilityDTO toDto(Availability s);

    @Named("collaboratorFederatedId")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "federatedId", source = "federatedId")
    CollaboratorDTO toDtoCollaboratorFederatedId(Collaborator collaborator);
}
