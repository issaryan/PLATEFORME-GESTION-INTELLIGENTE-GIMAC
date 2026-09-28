package cm.gimac.des.collaboration.service.mapper;

import static cm.gimac.des.collaboration.domain.RecordingSegmentAsserts.*;
import static cm.gimac.des.collaboration.domain.RecordingSegmentTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RecordingSegmentMapperTest {

    private RecordingSegmentMapper recordingSegmentMapper;

    @BeforeEach
    void setUp() {
        recordingSegmentMapper = new RecordingSegmentMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getRecordingSegmentSample1();
        var actual = recordingSegmentMapper.toEntity(recordingSegmentMapper.toDto(expected));
        assertRecordingSegmentAllPropertiesEquals(expected, actual);
    }
}
