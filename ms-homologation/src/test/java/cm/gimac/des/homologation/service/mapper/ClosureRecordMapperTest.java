package cm.gimac.des.homologation.service.mapper;

import static cm.gimac.des.homologation.domain.ClosureRecordAsserts.*;
import static cm.gimac.des.homologation.domain.ClosureRecordTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ClosureRecordMapperTest {

    private ClosureRecordMapper closureRecordMapper;

    @BeforeEach
    void setUp() {
        closureRecordMapper = new ClosureRecordMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getClosureRecordSample1();
        var actual = closureRecordMapper.toEntity(closureRecordMapper.toDto(expected));
        assertClosureRecordAllPropertiesEquals(expected, actual);
    }
}
