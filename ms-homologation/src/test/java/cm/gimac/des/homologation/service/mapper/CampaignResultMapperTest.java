package cm.gimac.des.homologation.service.mapper;

import static cm.gimac.des.homologation.domain.CampaignResultAsserts.*;
import static cm.gimac.des.homologation.domain.CampaignResultTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CampaignResultMapperTest {

    private CampaignResultMapper campaignResultMapper;

    @BeforeEach
    void setUp() {
        campaignResultMapper = new CampaignResultMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getCampaignResultSample1();
        var actual = campaignResultMapper.toEntity(campaignResultMapper.toDto(expected));
        assertCampaignResultAllPropertiesEquals(expected, actual);
    }
}
