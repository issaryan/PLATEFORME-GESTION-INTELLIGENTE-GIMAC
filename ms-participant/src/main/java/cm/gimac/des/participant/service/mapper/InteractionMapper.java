package cm.gimac.des.participant.service.mapper;

import cm.gimac.des.participant.domain.Institution;
import cm.gimac.des.participant.domain.Interaction;
import cm.gimac.des.participant.service.dto.InstitutionDTO;
import cm.gimac.des.participant.service.dto.InteractionDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link Interaction} and its DTO {@link InteractionDTO}.
 */
@Mapper(componentModel = "spring")
public interface InteractionMapper extends EntityMapper<InteractionDTO, Interaction> {
    @Mapping(target = "institution", source = "institution", qualifiedByName = "institutionParticipantCode")
    InteractionDTO toDto(Interaction s);

    @Named("institutionParticipantCode")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "participantCode", source = "participantCode")
    InstitutionDTO toDtoInstitutionParticipantCode(Institution institution);
}
