package cm.gimac.des.pilotage.service.mapper;

import static cm.gimac.des.pilotage.domain.MilestoneApprovalAsserts.*;
import static cm.gimac.des.pilotage.domain.MilestoneApprovalTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MilestoneApprovalMapperTest {

    private MilestoneApprovalMapper milestoneApprovalMapper;

    @BeforeEach
    void setUp() {
        milestoneApprovalMapper = new MilestoneApprovalMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getMilestoneApprovalSample1();
        var actual = milestoneApprovalMapper.toEntity(milestoneApprovalMapper.toDto(expected));
        assertMilestoneApprovalAllPropertiesEquals(expected, actual);
    }
}
