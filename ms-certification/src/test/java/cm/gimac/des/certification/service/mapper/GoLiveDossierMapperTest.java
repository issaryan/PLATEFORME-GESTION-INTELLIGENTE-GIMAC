package cm.gimac.des.certification.service.mapper;

import static cm.gimac.des.certification.domain.GoLiveDossierAsserts.*;
import static cm.gimac.des.certification.domain.GoLiveDossierTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GoLiveDossierMapperTest {

    private GoLiveDossierMapper goLiveDossierMapper;

    @BeforeEach
    void setUp() {
        goLiveDossierMapper = new GoLiveDossierMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getGoLiveDossierSample1();
        var actual = goLiveDossierMapper.toEntity(goLiveDossierMapper.toDto(expected));
        assertGoLiveDossierAllPropertiesEquals(expected, actual);
    }
}
