package cm.gimac.des.pilotage.service.mapper;

import static cm.gimac.des.pilotage.domain.ActionSheetAsserts.*;
import static cm.gimac.des.pilotage.domain.ActionSheetTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ActionSheetMapperTest {

    private ActionSheetMapper actionSheetMapper;

    @BeforeEach
    void setUp() {
        actionSheetMapper = new ActionSheetMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getActionSheetSample1();
        var actual = actionSheetMapper.toEntity(actionSheetMapper.toDto(expected));
        assertActionSheetAllPropertiesEquals(expected, actual);
    }
}
