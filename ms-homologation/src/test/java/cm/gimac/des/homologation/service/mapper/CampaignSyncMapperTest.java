package cm.gimac.des.homologation.service.mapper;

import static cm.gimac.des.homologation.domain.CampaignSyncAsserts.*;
import static cm.gimac.des.homologation.domain.CampaignSyncTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CampaignSyncMapperTest {

    private CampaignSyncMapper campaignSyncMapper;

    @BeforeEach
    void setUp() {
        campaignSyncMapper = new CampaignSyncMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getCampaignSyncSample1();
        var actual = campaignSyncMapper.toEntity(campaignSyncMapper.toDto(expected));
        assertCampaignSyncAllPropertiesEquals(expected, actual);
    }
}
