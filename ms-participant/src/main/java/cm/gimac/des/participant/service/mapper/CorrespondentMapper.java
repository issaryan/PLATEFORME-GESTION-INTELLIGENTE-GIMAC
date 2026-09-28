package cm.gimac.des.participant.service.mapper;

import cm.gimac.des.participant.domain.Correspondent;
import cm.gimac.des.participant.domain.Institution;
import cm.gimac.des.participant.service.dto.CorrespondentDTO;
import cm.gimac.des.participant.service.dto.InstitutionDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link Correspondent} and its DTO {@link CorrespondentDTO}.
 */
@Mapper(componentModel = "spring")
public interface CorrespondentMapper extends EntityMapper<CorrespondentDTO, Correspondent> {
    @Mapping(target = "institution", source = "institution", qualifiedByName = "institutionParticipantCode")
    CorrespondentDTO toDto(Correspondent s);

    @Named("institutionParticipantCode")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "participantCode", source = "participantCode")
    InstitutionDTO toDtoInstitutionParticipantCode(Institution institution);
}
