package cm.gimac.des.participant.service.mapper;

import cm.gimac.des.participant.domain.Correspondent;
import cm.gimac.des.participant.domain.EscalationContact;
import cm.gimac.des.participant.domain.Institution;
import cm.gimac.des.participant.service.dto.CorrespondentDTO;
import cm.gimac.des.participant.service.dto.EscalationContactDTO;
import cm.gimac.des.participant.service.dto.InstitutionDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link EscalationContact} and its DTO {@link EscalationContactDTO}.
 */
@Mapper(componentModel = "spring")
public interface EscalationContactMapper extends EntityMapper<EscalationContactDTO, EscalationContact> {
    @Mapping(target = "institution", source = "institution", qualifiedByName = "institutionParticipantCode")
    @Mapping(target = "correspondent", source = "correspondent", qualifiedByName = "correspondentFullName")
    @Mapping(target = "deputy", source = "deputy", qualifiedByName = "correspondentFullName")
    EscalationContactDTO toDto(EscalationContact s);

    @Named("institutionParticipantCode")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "participantCode", source = "participantCode")
    InstitutionDTO toDtoInstitutionParticipantCode(Institution institution);

    @Named("correspondentFullName")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "fullName", source = "fullName")
    CorrespondentDTO toDtoCorrespondentFullName(Correspondent correspondent);
}
