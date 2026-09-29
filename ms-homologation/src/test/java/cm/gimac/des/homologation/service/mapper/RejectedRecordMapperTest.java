package cm.gimac.des.homologation.service.mapper;

import static cm.gimac.des.homologation.domain.RejectedRecordAsserts.*;
import static cm.gimac.des.homologation.domain.RejectedRecordTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RejectedRecordMapperTest {

    private RejectedRecordMapper rejectedRecordMapper;

    @BeforeEach
    void setUp() {
        rejectedRecordMapper = new RejectedRecordMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getRejectedRecordSample1();
        var actual = rejectedRecordMapper.toEntity(rejectedRecordMapper.toDto(expected));
        assertRejectedRecordAllPropertiesEquals(expected, actual);
    }
}
