package cm.gimac.des.audit.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.audit.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AuditEventDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(AuditEventDTO.class);
        AuditEventDTO auditEventDTO1 = new AuditEventDTO();
        auditEventDTO1.setId(UUID.randomUUID());
        AuditEventDTO auditEventDTO2 = new AuditEventDTO();
        assertThat(auditEventDTO1).isNotEqualTo(auditEventDTO2);
        auditEventDTO2.setId(auditEventDTO1.getId());
        assertThat(auditEventDTO1).isEqualTo(auditEventDTO2);
        auditEventDTO2.setId(UUID.randomUUID());
        assertThat(auditEventDTO1).isNotEqualTo(auditEventDTO2);
        auditEventDTO1.setId(null);
        assertThat(auditEventDTO1).isNotEqualTo(auditEventDTO2);
    }
}
