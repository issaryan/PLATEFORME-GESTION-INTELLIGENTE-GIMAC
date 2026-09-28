package cm.gimac.des.collaboration.service.mapper;

import cm.gimac.des.collaboration.domain.Conference;
import cm.gimac.des.collaboration.domain.ConferenceParticipant;
import cm.gimac.des.collaboration.service.dto.ConferenceDTO;
import cm.gimac.des.collaboration.service.dto.ConferenceParticipantDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link ConferenceParticipant} and its DTO {@link ConferenceParticipantDTO}.
 */
@Mapper(componentModel = "spring")
public interface ConferenceParticipantMapper extends EntityMapper<ConferenceParticipantDTO, ConferenceParticipant> {
    @Mapping(target = "conference", source = "conference", qualifiedByName = "conferenceSfuRoom")
    ConferenceParticipantDTO toDto(ConferenceParticipant s);

    @Named("conferenceSfuRoom")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "sfuRoom", source = "sfuRoom")
    ConferenceDTO toDtoConferenceSfuRoom(Conference conference);
}
