package cm.gimac.des.participant.service.mapper;

import cm.gimac.des.participant.domain.Institution;
import cm.gimac.des.participant.service.dto.InstitutionDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link Institution} and its DTO {@link InstitutionDTO}.
 */
@Mapper(componentModel = "spring")
public interface InstitutionMapper extends EntityMapper<InstitutionDTO, Institution> {
    @Mapping(target = "settlementBank", source = "settlementBank", qualifiedByName = "institutionParticipantCode")
    @Mapping(target = "sponsorBank", source = "sponsorBank", qualifiedByName = "institutionParticipantCode")
    InstitutionDTO toDto(Institution s);

    @Named("institutionParticipantCode")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "participantCode", source = "participantCode")
    InstitutionDTO toDtoInstitutionParticipantCode(Institution institution);
}
