package cm.gimac.des.collaboration.service.mapper;

import static cm.gimac.des.collaboration.domain.VerifiedTerminalAsserts.*;
import static cm.gimac.des.collaboration.domain.VerifiedTerminalTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class VerifiedTerminalMapperTest {

    private VerifiedTerminalMapper verifiedTerminalMapper;

    @BeforeEach
    void setUp() {
        verifiedTerminalMapper = new VerifiedTerminalMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getVerifiedTerminalSample1();
        var actual = verifiedTerminalMapper.toEntity(verifiedTerminalMapper.toDto(expected));
        assertVerifiedTerminalAllPropertiesEquals(expected, actual);
    }
}
