package cm.gimac.des.rapprochement.domain;

import static cm.gimac.des.rapprochement.domain.ImportedStatementTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.rapprochement.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class ImportedStatementTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(ImportedStatement.class);
        ImportedStatement importedStatement1 = getImportedStatementSample1();
        ImportedStatement importedStatement2 = new ImportedStatement();
        assertThat(importedStatement1).isNotEqualTo(importedStatement2);

        importedStatement2.setId(importedStatement1.getId());
        assertThat(importedStatement1).isEqualTo(importedStatement2);

        importedStatement2 = getImportedStatementSample2();
        assertThat(importedStatement1).isNotEqualTo(importedStatement2);
    }
}
