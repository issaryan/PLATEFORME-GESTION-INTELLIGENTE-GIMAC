package cm.gimac.des.certification.service.mapper;

import static cm.gimac.des.certification.domain.QuitusAsserts.*;
import static cm.gimac.des.certification.domain.QuitusTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class QuitusMapperTest {

    private QuitusMapper quitusMapper;

    @BeforeEach
    void setUp() {
        quitusMapper = new QuitusMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getQuitusSample1();
        var actual = quitusMapper.toEntity(quitusMapper.toDto(expected));
        assertQuitusAllPropertiesEquals(expected, actual);
    }
}
