package cm.gimac.des.audit.domain;

import static cm.gimac.des.audit.domain.AuditEventTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.audit.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class AuditEventTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(AuditEvent.class);
        AuditEvent auditEvent1 = getAuditEventSample1();
        AuditEvent auditEvent2 = new AuditEvent();
        assertThat(auditEvent1).isNotEqualTo(auditEvent2);

        auditEvent2.setId(auditEvent1.getId());
        assertThat(auditEvent1).isEqualTo(auditEvent2);

        auditEvent2 = getAuditEventSample2();
        assertThat(auditEvent1).isNotEqualTo(auditEvent2);
    }
}
