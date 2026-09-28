package cm.gimac.des.collaboration.service.mapper;

import cm.gimac.des.collaboration.domain.Conference;
import cm.gimac.des.collaboration.domain.RecordingConsent;
import cm.gimac.des.collaboration.service.dto.ConferenceDTO;
import cm.gimac.des.collaboration.service.dto.RecordingConsentDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link RecordingConsent} and its DTO {@link RecordingConsentDTO}.
 */
@Mapper(componentModel = "spring")
public interface RecordingConsentMapper extends EntityMapper<RecordingConsentDTO, RecordingConsent> {
    @Mapping(target = "conference", source = "conference", qualifiedByName = "conferenceSfuRoom")
    RecordingConsentDTO toDto(RecordingConsent s);

    @Named("conferenceSfuRoom")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "sfuRoom", source = "sfuRoom")
    ConferenceDTO toDtoConferenceSfuRoom(Conference conference);
}
