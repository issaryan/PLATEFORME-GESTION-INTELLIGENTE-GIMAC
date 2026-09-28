package cm.gimac.des.reporting.service.mapper;

import static cm.gimac.des.reporting.domain.ProcessedEventAsserts.*;
import static cm.gimac.des.reporting.domain.ProcessedEventTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProcessedEventMapperTest {

    private ProcessedEventMapper processedEventMapper;

    @BeforeEach
    void setUp() {
        processedEventMapper = new ProcessedEventMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getProcessedEventSample1();
        var actual = processedEventMapper.toEntity(processedEventMapper.toDto(expected));
        assertProcessedEventAllPropertiesEquals(expected, actual);
    }
}
