package cm.gimac.des.certification.service.mapper;

import static cm.gimac.des.certification.domain.ChangeRecordAsserts.*;
import static cm.gimac.des.certification.domain.ChangeRecordTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ChangeRecordMapperTest {

    private ChangeRecordMapper changeRecordMapper;

    @BeforeEach
    void setUp() {
        changeRecordMapper = new ChangeRecordMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getChangeRecordSample1();
        var actual = changeRecordMapper.toEntity(changeRecordMapper.toDto(expected));
        assertChangeRecordAllPropertiesEquals(expected, actual);
    }
}
