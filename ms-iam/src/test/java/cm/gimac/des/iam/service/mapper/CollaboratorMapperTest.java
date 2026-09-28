package cm.gimac.des.iam.service.mapper;

import static cm.gimac.des.iam.domain.CollaboratorAsserts.*;
import static cm.gimac.des.iam.domain.CollaboratorTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CollaboratorMapperTest {

    private CollaboratorMapper collaboratorMapper;

    @BeforeEach
    void setUp() {
        collaboratorMapper = new CollaboratorMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getCollaboratorSample1();
        var actual = collaboratorMapper.toEntity(collaboratorMapper.toDto(expected));
        assertCollaboratorAllPropertiesEquals(expected, actual);
    }
}
