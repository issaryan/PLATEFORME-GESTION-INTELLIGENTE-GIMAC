package cm.gimac.des.collaboration.service.mapper;

import cm.gimac.des.collaboration.domain.Conference;
import cm.gimac.des.collaboration.domain.RecordingSegment;
import cm.gimac.des.collaboration.service.dto.ConferenceDTO;
import cm.gimac.des.collaboration.service.dto.RecordingSegmentDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link RecordingSegment} and its DTO {@link RecordingSegmentDTO}.
 */
@Mapper(componentModel = "spring")
public interface RecordingSegmentMapper extends EntityMapper<RecordingSegmentDTO, RecordingSegment> {
    @Mapping(target = "conference", source = "conference", qualifiedByName = "conferenceSfuRoom")
    RecordingSegmentDTO toDto(RecordingSegment s);

    @Named("conferenceSfuRoom")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "sfuRoom", source = "sfuRoom")
    ConferenceDTO toDtoConferenceSfuRoom(Conference conference);
}
