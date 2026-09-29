package cm.gimac.des.participant.service.mapper;

import static cm.gimac.des.participant.domain.AdhesionPieceAsserts.*;
import static cm.gimac.des.participant.domain.AdhesionPieceTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AdhesionPieceMapperTest {

    private AdhesionPieceMapper adhesionPieceMapper;

    @BeforeEach
    void setUp() {
        adhesionPieceMapper = new AdhesionPieceMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getAdhesionPieceSample1();
        var actual = adhesionPieceMapper.toEntity(adhesionPieceMapper.toDto(expected));
        assertAdhesionPieceAllPropertiesEquals(expected, actual);
    }
}
