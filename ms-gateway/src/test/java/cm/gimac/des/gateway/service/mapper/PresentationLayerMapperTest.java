package cm.gimac.des.gateway.service.mapper;

import static cm.gimac.des.gateway.domain.PresentationLayerAsserts.*;
import static cm.gimac.des.gateway.domain.PresentationLayerTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PresentationLayerMapperTest {

    private PresentationLayerMapper presentationLayerMapper;

    @BeforeEach
    void setUp() {
        presentationLayerMapper = new PresentationLayerMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getPresentationLayerSample1();
        var actual = presentationLayerMapper.toEntity(presentationLayerMapper.toDto(expected));
        assertPresentationLayerAllPropertiesEquals(expected, actual);
    }
}
