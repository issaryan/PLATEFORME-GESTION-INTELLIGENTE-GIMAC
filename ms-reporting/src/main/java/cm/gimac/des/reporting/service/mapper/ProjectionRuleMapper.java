package cm.gimac.des.reporting.service.mapper;

import cm.gimac.des.reporting.domain.ProjectionRule;
import cm.gimac.des.reporting.service.dto.ProjectionRuleDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link ProjectionRule} and its DTO {@link ProjectionRuleDTO}.
 */
@Mapper(componentModel = "spring")
public interface ProjectionRuleMapper extends EntityMapper<ProjectionRuleDTO, ProjectionRule> {}
