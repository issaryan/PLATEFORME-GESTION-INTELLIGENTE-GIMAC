package cm.gimac.des.collaboration.service.mapper;

import cm.gimac.des.collaboration.domain.Conference;
import cm.gimac.des.collaboration.domain.MediaKeyEpoch;
import cm.gimac.des.collaboration.service.dto.ConferenceDTO;
import cm.gimac.des.collaboration.service.dto.MediaKeyEpochDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link MediaKeyEpoch} and its DTO {@link MediaKeyEpochDTO}.
 */
@Mapper(componentModel = "spring")
public interface MediaKeyEpochMapper extends EntityMapper<MediaKeyEpochDTO, MediaKeyEpoch> {
    @Mapping(target = "conference", source = "conference", qualifiedByName = "conferenceSfuRoom")
    MediaKeyEpochDTO toDto(MediaKeyEpoch s);

    @Named("conferenceSfuRoom")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "sfuRoom", source = "sfuRoom")
    ConferenceDTO toDtoConferenceSfuRoom(Conference conference);
}
