package cm.gimac.des.rapprochement.service.mapper;

import cm.gimac.des.rapprochement.domain.AgingRule;
import cm.gimac.des.rapprochement.service.dto.AgingRuleDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link AgingRule} and its DTO {@link AgingRuleDTO}.
 */
@Mapper(componentModel = "spring")
public interface AgingRuleMapper extends EntityMapper<AgingRuleDTO, AgingRule> {}
