package cm.gimac.des.rapprochement.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.rapprochement.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ImportedStatementDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(ImportedStatementDTO.class);
        ImportedStatementDTO importedStatementDTO1 = new ImportedStatementDTO();
        importedStatementDTO1.setId(UUID.randomUUID());
        ImportedStatementDTO importedStatementDTO2 = new ImportedStatementDTO();
        assertThat(importedStatementDTO1).isNotEqualTo(importedStatementDTO2);
        importedStatementDTO2.setId(importedStatementDTO1.getId());
        assertThat(importedStatementDTO1).isEqualTo(importedStatementDTO2);
        importedStatementDTO2.setId(UUID.randomUUID());
        assertThat(importedStatementDTO1).isNotEqualTo(importedStatementDTO2);
        importedStatementDTO1.setId(null);
        assertThat(importedStatementDTO1).isNotEqualTo(importedStatementDTO2);
    }
}
