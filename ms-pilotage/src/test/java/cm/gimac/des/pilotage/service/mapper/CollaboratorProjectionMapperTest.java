package cm.gimac.des.pilotage.service.mapper;

import static cm.gimac.des.pilotage.domain.CollaboratorProjectionAsserts.*;
import static cm.gimac.des.pilotage.domain.CollaboratorProjectionTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CollaboratorProjectionMapperTest {

    private CollaboratorProjectionMapper collaboratorProjectionMapper;

    @BeforeEach
    void setUp() {
        collaboratorProjectionMapper = new CollaboratorProjectionMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getCollaboratorProjectionSample1();
        var actual = collaboratorProjectionMapper.toEntity(collaboratorProjectionMapper.toDto(expected));
        assertCollaboratorProjectionAllPropertiesEquals(expected, actual);
    }
}
