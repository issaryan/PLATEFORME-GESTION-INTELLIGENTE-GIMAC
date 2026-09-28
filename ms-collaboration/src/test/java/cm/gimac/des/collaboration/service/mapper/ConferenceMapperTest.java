package cm.gimac.des.collaboration.service.mapper;

import static cm.gimac.des.collaboration.domain.ConferenceAsserts.*;
import static cm.gimac.des.collaboration.domain.ConferenceTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ConferenceMapperTest {

    private ConferenceMapper conferenceMapper;

    @BeforeEach
    void setUp() {
        conferenceMapper = new ConferenceMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getConferenceSample1();
        var actual = conferenceMapper.toEntity(conferenceMapper.toDto(expected));
        assertConferenceAllPropertiesEquals(expected, actual);
    }
}
