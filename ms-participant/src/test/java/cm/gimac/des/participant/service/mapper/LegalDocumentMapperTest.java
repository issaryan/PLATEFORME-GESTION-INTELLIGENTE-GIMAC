package cm.gimac.des.participant.service.mapper;

import static cm.gimac.des.participant.domain.LegalDocumentAsserts.*;
import static cm.gimac.des.participant.domain.LegalDocumentTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LegalDocumentMapperTest {

    private LegalDocumentMapper legalDocumentMapper;

    @BeforeEach
    void setUp() {
        legalDocumentMapper = new LegalDocumentMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getLegalDocumentSample1();
        var actual = legalDocumentMapper.toEntity(legalDocumentMapper.toDto(expected));
        assertLegalDocumentAllPropertiesEquals(expected, actual);
    }
}
