package cm.gimac.des.pilotage.service.mapper;

import static cm.gimac.des.pilotage.domain.BoardAsserts.*;
import static cm.gimac.des.pilotage.domain.BoardTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BoardMapperTest {

    private BoardMapper boardMapper;

    @BeforeEach
    void setUp() {
        boardMapper = new BoardMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getBoardSample1();
        var actual = boardMapper.toEntity(boardMapper.toDto(expected));
        assertBoardAllPropertiesEquals(expected, actual);
    }
}
