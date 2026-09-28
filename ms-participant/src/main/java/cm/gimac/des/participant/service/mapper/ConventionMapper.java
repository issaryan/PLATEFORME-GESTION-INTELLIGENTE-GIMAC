package cm.gimac.des.participant.service.mapper;

import cm.gimac.des.participant.domain.Convention;
import cm.gimac.des.participant.domain.Institution;
import cm.gimac.des.participant.service.dto.ConventionDTO;
import cm.gimac.des.participant.service.dto.InstitutionDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link Convention} and its DTO {@link ConventionDTO}.
 */
@Mapper(componentModel = "spring")
public interface ConventionMapper extends EntityMapper<ConventionDTO, Convention> {
    @Mapping(target = "institution", source = "institution", qualifiedByName = "institutionParticipantCode")
    ConventionDTO toDto(Convention s);

    @Named("institutionParticipantCode")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "participantCode", source = "participantCode")
    InstitutionDTO toDtoInstitutionParticipantCode(Institution institution);
}
