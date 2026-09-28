package cm.gimac.des.homologation.service.mapper;

import cm.gimac.des.homologation.domain.CampaignSync;
import cm.gimac.des.homologation.domain.Liaison;
import cm.gimac.des.homologation.service.dto.CampaignSyncDTO;
import cm.gimac.des.homologation.service.dto.LiaisonDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link CampaignSync} and its DTO {@link CampaignSyncDTO}.
 */
@Mapper(componentModel = "spring")
public interface CampaignSyncMapper extends EntityMapper<CampaignSyncDTO, CampaignSync> {
    @Mapping(target = "liaison", source = "liaison", qualifiedByName = "liaisonLiaisonCode")
    CampaignSyncDTO toDto(CampaignSync s);

    @Named("liaisonLiaisonCode")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "liaisonCode", source = "liaisonCode")
    LiaisonDTO toDtoLiaisonLiaisonCode(Liaison liaison);
}
