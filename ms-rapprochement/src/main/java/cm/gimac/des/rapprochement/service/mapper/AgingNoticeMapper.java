package cm.gimac.des.rapprochement.service.mapper;

import cm.gimac.des.rapprochement.domain.AgingNotice;
import cm.gimac.des.rapprochement.domain.AgingRule;
import cm.gimac.des.rapprochement.domain.Suspens;
import cm.gimac.des.rapprochement.service.dto.AgingNoticeDTO;
import cm.gimac.des.rapprochement.service.dto.AgingRuleDTO;
import cm.gimac.des.rapprochement.service.dto.SuspensDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link AgingNotice} and its DTO {@link AgingNoticeDTO}.
 */
@Mapper(componentModel = "spring")
public interface AgingNoticeMapper extends EntityMapper<AgingNoticeDTO, AgingNotice> {
    @Mapping(target = "suspens", source = "suspens", qualifiedByName = "suspensReference")
    @Mapping(target = "rule", source = "rule", qualifiedByName = "agingRuleThresholdDays")
    AgingNoticeDTO toDto(AgingNotice s);

    @Named("suspensReference")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "reference", source = "reference")
    SuspensDTO toDtoSuspensReference(Suspens suspens);

    @Named("agingRuleThresholdDays")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "thresholdDays", source = "thresholdDays")
    AgingRuleDTO toDtoAgingRuleThresholdDays(AgingRule agingRule);
}
