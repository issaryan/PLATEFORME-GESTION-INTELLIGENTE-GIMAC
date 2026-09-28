package cm.gimac.des.pilotage.service.mapper;

import static cm.gimac.des.pilotage.domain.MeetingAttendanceAsserts.*;
import static cm.gimac.des.pilotage.domain.MeetingAttendanceTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MeetingAttendanceMapperTest {

    private MeetingAttendanceMapper meetingAttendanceMapper;

    @BeforeEach
    void setUp() {
        meetingAttendanceMapper = new MeetingAttendanceMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getMeetingAttendanceSample1();
        var actual = meetingAttendanceMapper.toEntity(meetingAttendanceMapper.toDto(expected));
        assertMeetingAttendanceAllPropertiesEquals(expected, actual);
    }
}
