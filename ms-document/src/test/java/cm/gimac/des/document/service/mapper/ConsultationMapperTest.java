package cm.gimac.des.document.service.mapper;

import static cm.gimac.des.document.domain.ConsultationAsserts.*;
import static cm.gimac.des.document.domain.ConsultationTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ConsultationMapperTest {

    private ConsultationMapper consultationMapper;

    @BeforeEach
    void setUp() {
        consultationMapper = new ConsultationMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getConsultationSample1();
        var actual = consultationMapper.toEntity(consultationMapper.toDto(expected));
        assertConsultationAllPropertiesEquals(expected, actual);
    }
}
