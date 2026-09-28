package cm.gimac.des.pilotage.service.mapper;

import static cm.gimac.des.pilotage.domain.BoardColumnAsserts.*;
import static cm.gimac.des.pilotage.domain.BoardColumnTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BoardColumnMapperTest {

    private BoardColumnMapper boardColumnMapper;

    @BeforeEach
    void setUp() {
        boardColumnMapper = new BoardColumnMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getBoardColumnSample1();
        var actual = boardColumnMapper.toEntity(boardColumnMapper.toDto(expected));
        assertBoardColumnAllPropertiesEquals(expected, actual);
    }
}
