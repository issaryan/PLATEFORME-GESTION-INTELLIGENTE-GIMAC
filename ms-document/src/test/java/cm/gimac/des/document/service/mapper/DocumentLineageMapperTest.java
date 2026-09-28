package cm.gimac.des.document.service.mapper;

import static cm.gimac.des.document.domain.DocumentLineageAsserts.*;
import static cm.gimac.des.document.domain.DocumentLineageTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DocumentLineageMapperTest {

    private DocumentLineageMapper documentLineageMapper;

    @BeforeEach
    void setUp() {
        documentLineageMapper = new DocumentLineageMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getDocumentLineageSample1();
        var actual = documentLineageMapper.toEntity(documentLineageMapper.toDto(expected));
        assertDocumentLineageAllPropertiesEquals(expected, actual);
    }
}
