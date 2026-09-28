package cm.gimac.des.rapprochement.service.mapper;

import static cm.gimac.des.rapprochement.domain.AgingNoticeAsserts.*;
import static cm.gimac.des.rapprochement.domain.AgingNoticeTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AgingNoticeMapperTest {

    private AgingNoticeMapper agingNoticeMapper;

    @BeforeEach
    void setUp() {
        agingNoticeMapper = new AgingNoticeMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getAgingNoticeSample1();
        var actual = agingNoticeMapper.toEntity(agingNoticeMapper.toDto(expected));
        assertAgingNoticeAllPropertiesEquals(expected, actual);
    }
}
