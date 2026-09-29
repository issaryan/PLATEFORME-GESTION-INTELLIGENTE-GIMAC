package cm.gimac.des.certification.service.mapper;

import static cm.gimac.des.certification.domain.TimestampTokenAsserts.*;
import static cm.gimac.des.certification.domain.TimestampTokenTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TimestampTokenMapperTest {

    private TimestampTokenMapper timestampTokenMapper;

    @BeforeEach
    void setUp() {
        timestampTokenMapper = new TimestampTokenMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getTimestampTokenSample1();
        var actual = timestampTokenMapper.toEntity(timestampTokenMapper.toDto(expected));
        assertTimestampTokenAllPropertiesEquals(expected, actual);
    }
}
