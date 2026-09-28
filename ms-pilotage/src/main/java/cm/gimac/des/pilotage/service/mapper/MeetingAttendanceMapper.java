package cm.gimac.des.pilotage.service.mapper;

import cm.gimac.des.pilotage.domain.Meeting;
import cm.gimac.des.pilotage.domain.MeetingAttendance;
import cm.gimac.des.pilotage.service.dto.MeetingAttendanceDTO;
import cm.gimac.des.pilotage.service.dto.MeetingDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link MeetingAttendance} and its DTO {@link MeetingAttendanceDTO}.
 */
@Mapper(componentModel = "spring")
public interface MeetingAttendanceMapper extends EntityMapper<MeetingAttendanceDTO, MeetingAttendance> {
    @Mapping(target = "meeting", source = "meeting", qualifiedByName = "meetingInstance")
    MeetingAttendanceDTO toDto(MeetingAttendance s);

    @Named("meetingInstance")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "instance", source = "instance")
    MeetingDTO toDtoMeetingInstance(Meeting meeting);
}
