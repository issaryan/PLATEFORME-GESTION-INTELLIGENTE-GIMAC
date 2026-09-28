package cm.gimac.des.collaboration.service.mapper;

import static cm.gimac.des.collaboration.domain.MediaKeyEpochAsserts.*;
import static cm.gimac.des.collaboration.domain.MediaKeyEpochTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MediaKeyEpochMapperTest {

    private MediaKeyEpochMapper mediaKeyEpochMapper;

    @BeforeEach
    void setUp() {
        mediaKeyEpochMapper = new MediaKeyEpochMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getMediaKeyEpochSample1();
        var actual = mediaKeyEpochMapper.toEntity(mediaKeyEpochMapper.toDto(expected));
        assertMediaKeyEpochAllPropertiesEquals(expected, actual);
    }
}
