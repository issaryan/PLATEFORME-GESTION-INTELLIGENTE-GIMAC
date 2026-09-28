package cm.gimac.des.collaboration.service.mapper;

import static cm.gimac.des.collaboration.domain.RecordingConsentAsserts.*;
import static cm.gimac.des.collaboration.domain.RecordingConsentTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RecordingConsentMapperTest {

    private RecordingConsentMapper recordingConsentMapper;

    @BeforeEach
    void setUp() {
        recordingConsentMapper = new RecordingConsentMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getRecordingConsentSample1();
        var actual = recordingConsentMapper.toEntity(recordingConsentMapper.toDto(expected));
        assertRecordingConsentAllPropertiesEquals(expected, actual);
    }
}
