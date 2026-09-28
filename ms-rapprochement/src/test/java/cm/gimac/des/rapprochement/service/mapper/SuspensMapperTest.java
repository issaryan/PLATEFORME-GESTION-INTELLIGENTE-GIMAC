package cm.gimac.des.rapprochement.service.mapper;

import static cm.gimac.des.rapprochement.domain.SuspensAsserts.*;
import static cm.gimac.des.rapprochement.domain.SuspensTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SuspensMapperTest {

    private SuspensMapper suspensMapper;

    @BeforeEach
    void setUp() {
        suspensMapper = new SuspensMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getSuspensSample1();
        var actual = suspensMapper.toEntity(suspensMapper.toDto(expected));
        assertSuspensAllPropertiesEquals(expected, actual);
    }
}
