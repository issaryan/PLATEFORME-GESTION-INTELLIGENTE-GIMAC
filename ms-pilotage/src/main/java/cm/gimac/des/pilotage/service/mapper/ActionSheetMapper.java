package cm.gimac.des.pilotage.service.mapper;

import cm.gimac.des.pilotage.domain.ActionSheet;
import cm.gimac.des.pilotage.domain.Risk;
import cm.gimac.des.pilotage.service.dto.ActionSheetDTO;
import cm.gimac.des.pilotage.service.dto.RiskDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link ActionSheet} and its DTO {@link ActionSheetDTO}.
 */
@Mapper(componentModel = "spring")
public interface ActionSheetMapper extends EntityMapper<ActionSheetDTO, ActionSheet> {
    @Mapping(target = "risk", source = "risk", qualifiedByName = "riskScope")
    ActionSheetDTO toDto(ActionSheet s);

    @Named("riskScope")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "scope", source = "scope")
    RiskDTO toDtoRiskScope(Risk risk);
}
