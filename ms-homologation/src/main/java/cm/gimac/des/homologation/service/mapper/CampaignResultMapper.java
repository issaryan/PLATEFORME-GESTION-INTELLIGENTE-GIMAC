package cm.gimac.des.homologation.service.mapper;

import cm.gimac.des.homologation.domain.CampaignResult;
import cm.gimac.des.homologation.domain.Liaison;
import cm.gimac.des.homologation.domain.OperationInstance;
import cm.gimac.des.homologation.service.dto.CampaignResultDTO;
import cm.gimac.des.homologation.service.dto.LiaisonDTO;
import cm.gimac.des.homologation.service.dto.OperationInstanceDTO;
import java.util.Objects;
import java.util.UUID;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link CampaignResult} and its DTO {@link CampaignResultDTO}.
 */
@Mapper(componentModel = "spring")
public interface CampaignResultMapper extends EntityMapper<CampaignResultDTO, CampaignResult> {
    @Mapping(target = "liaison", source = "liaison", qualifiedByName = "liaisonId")
    @Mapping(target = "operation", source = "operation", qualifiedByName = "operationInstanceId")
    CampaignResultDTO toDto(CampaignResult s);

    @Named("liaisonId")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    LiaisonDTO toDtoLiaisonId(Liaison liaison);

    @Named("operationInstanceId")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    OperationInstanceDTO toDtoOperationInstanceId(OperationInstance operationInstance);

    default String map(UUID value) {
        return Objects.toString(value, null);
    }
}
