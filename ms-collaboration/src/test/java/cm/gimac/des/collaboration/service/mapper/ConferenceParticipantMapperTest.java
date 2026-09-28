package cm.gimac.des.collaboration.service.mapper;

import static cm.gimac.des.collaboration.domain.ConferenceParticipantAsserts.*;
import static cm.gimac.des.collaboration.domain.ConferenceParticipantTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ConferenceParticipantMapperTest {

    private ConferenceParticipantMapper conferenceParticipantMapper;

    @BeforeEach
    void setUp() {
        conferenceParticipantMapper = new ConferenceParticipantMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getConferenceParticipantSample1();
        var actual = conferenceParticipantMapper.toEntity(conferenceParticipantMapper.toDto(expected));
        assertConferenceParticipantAllPropertiesEquals(expected, actual);
    }
}
