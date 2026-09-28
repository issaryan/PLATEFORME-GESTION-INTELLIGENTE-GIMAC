package cm.gimac.des.rapprochement.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.rapprochement.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class StatementLineDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(StatementLineDTO.class);
        StatementLineDTO statementLineDTO1 = new StatementLineDTO();
        statementLineDTO1.setId(UUID.randomUUID());
        StatementLineDTO statementLineDTO2 = new StatementLineDTO();
        assertThat(statementLineDTO1).isNotEqualTo(statementLineDTO2);
        statementLineDTO2.setId(statementLineDTO1.getId());
        assertThat(statementLineDTO1).isEqualTo(statementLineDTO2);
        statementLineDTO2.setId(UUID.randomUUID());
        assertThat(statementLineDTO1).isNotEqualTo(statementLineDTO2);
        statementLineDTO1.setId(null);
        assertThat(statementLineDTO1).isNotEqualTo(statementLineDTO2);
    }
}
