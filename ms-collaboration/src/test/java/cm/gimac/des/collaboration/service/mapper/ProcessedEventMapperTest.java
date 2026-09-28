package cm.gimac.des.collaboration.service.mapper;

import static cm.gimac.des.collaboration.domain.ProcessedEventAsserts.*;
import static cm.gimac.des.collaboration.domain.ProcessedEventTestSamples.*;

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
