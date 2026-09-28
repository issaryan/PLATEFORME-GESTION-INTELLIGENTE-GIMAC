package cm.gimac.des.participant.service.mapper;

import cm.gimac.des.participant.domain.Institution;
import cm.gimac.des.participant.domain.SubscribedService;
import cm.gimac.des.participant.service.dto.InstitutionDTO;
import cm.gimac.des.participant.service.dto.SubscribedServiceDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link SubscribedService} and its DTO {@link SubscribedServiceDTO}.
 */
@Mapper(componentModel = "spring")
public interface SubscribedServiceMapper extends EntityMapper<SubscribedServiceDTO, SubscribedService> {
    @Mapping(target = "institution", source = "institution", qualifiedByName = "institutionParticipantCode")
    SubscribedServiceDTO toDto(SubscribedService s);

    @Named("institutionParticipantCode")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "participantCode", source = "participantCode")
    InstitutionDTO toDtoInstitutionParticipantCode(Institution institution);
}
