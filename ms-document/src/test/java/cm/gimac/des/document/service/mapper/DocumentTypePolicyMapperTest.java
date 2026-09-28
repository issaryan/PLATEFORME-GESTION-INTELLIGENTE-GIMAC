package cm.gimac.des.document.service.mapper;

import static cm.gimac.des.document.domain.DocumentTypePolicyAsserts.*;
import static cm.gimac.des.document.domain.DocumentTypePolicyTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DocumentTypePolicyMapperTest {

    private DocumentTypePolicyMapper documentTypePolicyMapper;

    @BeforeEach
    void setUp() {
        documentTypePolicyMapper = new DocumentTypePolicyMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getDocumentTypePolicySample1();
        var actual = documentTypePolicyMapper.toEntity(documentTypePolicyMapper.toDto(expected));
        assertDocumentTypePolicyAllPropertiesEquals(expected, actual);
    }
}
