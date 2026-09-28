package cm.gimac.des.rapprochement.service.mapper;

import static cm.gimac.des.rapprochement.domain.ImportedStatementAsserts.*;
import static cm.gimac.des.rapprochement.domain.ImportedStatementTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ImportedStatementMapperTest {

    private ImportedStatementMapper importedStatementMapper;

    @BeforeEach
    void setUp() {
        importedStatementMapper = new ImportedStatementMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getImportedStatementSample1();
        var actual = importedStatementMapper.toEntity(importedStatementMapper.toDto(expected));
        assertImportedStatementAllPropertiesEquals(expected, actual);
    }
}
