package cm.gimac.des.certification.service.mapper;

import static cm.gimac.des.certification.domain.CommissioningFileAsserts.*;
import static cm.gimac.des.certification.domain.CommissioningFileTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CommissioningFileMapperTest {

    private CommissioningFileMapper commissioningFileMapper;

    @BeforeEach
    void setUp() {
        commissioningFileMapper = new CommissioningFileMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getCommissioningFileSample1();
        var actual = commissioningFileMapper.toEntity(commissioningFileMapper.toDto(expected));
        assertCommissioningFileAllPropertiesEquals(expected, actual);
    }
}
