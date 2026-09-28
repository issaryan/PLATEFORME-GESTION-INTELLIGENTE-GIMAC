package cm.gimac.des.notification.service.mapper;

import static cm.gimac.des.notification.domain.ChannelPreferenceAsserts.*;
import static cm.gimac.des.notification.domain.ChannelPreferenceTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ChannelPreferenceMapperTest {

    private ChannelPreferenceMapper channelPreferenceMapper;

    @BeforeEach
    void setUp() {
        channelPreferenceMapper = new ChannelPreferenceMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getChannelPreferenceSample1();
        var actual = channelPreferenceMapper.toEntity(channelPreferenceMapper.toDto(expected));
        assertChannelPreferenceAllPropertiesEquals(expected, actual);
    }
}
