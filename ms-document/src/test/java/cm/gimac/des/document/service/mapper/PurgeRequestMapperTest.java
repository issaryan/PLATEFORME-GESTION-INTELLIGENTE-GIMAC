package cm.gimac.des.document.service.mapper;

import static cm.gimac.des.document.domain.PurgeRequestAsserts.*;
import static cm.gimac.des.document.domain.PurgeRequestTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PurgeRequestMapperTest {

    private PurgeRequestMapper purgeRequestMapper;

    @BeforeEach
    void setUp() {
        purgeRequestMapper = new PurgeRequestMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getPurgeRequestSample1();
        var actual = purgeRequestMapper.toEntity(purgeRequestMapper.toDto(expected));
        assertPurgeRequestAllPropertiesEquals(expected, actual);
    }
}
