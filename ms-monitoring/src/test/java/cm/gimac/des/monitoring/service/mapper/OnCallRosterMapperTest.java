package cm.gimac.des.monitoring.service.mapper;

import static cm.gimac.des.monitoring.domain.OnCallRosterAsserts.*;
import static cm.gimac.des.monitoring.domain.OnCallRosterTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OnCallRosterMapperTest {

    private OnCallRosterMapper onCallRosterMapper;

    @BeforeEach
    void setUp() {
        onCallRosterMapper = new OnCallRosterMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getOnCallRosterSample1();
        var actual = onCallRosterMapper.toEntity(onCallRosterMapper.toDto(expected));
        assertOnCallRosterAllPropertiesEquals(expected, actual);
    }
}
