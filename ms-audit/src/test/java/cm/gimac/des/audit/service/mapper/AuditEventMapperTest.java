package cm.gimac.des.audit.service.mapper;

import static cm.gimac.des.audit.domain.AuditEventAsserts.*;
import static cm.gimac.des.audit.domain.AuditEventTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AuditEventMapperTest {

    private AuditEventMapper auditEventMapper;

    @BeforeEach
    void setUp() {
        auditEventMapper = new AuditEventMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getAuditEventSample1();
        var actual = auditEventMapper.toEntity(auditEventMapper.toDto(expected));
        assertAuditEventAllPropertiesEquals(expected, actual);
    }
}
