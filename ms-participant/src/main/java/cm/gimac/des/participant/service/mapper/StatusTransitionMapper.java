package cm.gimac.des.participant.service.mapper;

import cm.gimac.des.participant.domain.Institution;
import cm.gimac.des.participant.domain.StatusTransition;
import cm.gimac.des.participant.service.dto.InstitutionDTO;
import cm.gimac.des.participant.service.dto.StatusTransitionDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link StatusTransition} and its DTO {@link StatusTransitionDTO}.
 */
@Mapper(componentModel = "spring")
public interface StatusTransitionMapper extends EntityMapper<StatusTransitionDTO, StatusTransition> {
    @Mapping(target = "institution", source = "institution", qualifiedByName = "institutionParticipantCode")
    StatusTransitionDTO toDto(StatusTransition s);

    @Named("institutionParticipantCode")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "participantCode", source = "participantCode")
    InstitutionDTO toDtoInstitutionParticipantCode(Institution institution);
}
