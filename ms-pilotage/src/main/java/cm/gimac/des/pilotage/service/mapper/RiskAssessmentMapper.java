package cm.gimac.des.pilotage.service.mapper;

import cm.gimac.des.pilotage.domain.Risk;
import cm.gimac.des.pilotage.domain.RiskAssessment;
import cm.gimac.des.pilotage.service.dto.RiskAssessmentDTO;
import cm.gimac.des.pilotage.service.dto.RiskDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link RiskAssessment} and its DTO {@link RiskAssessmentDTO}.
 */
@Mapper(componentModel = "spring")
public interface RiskAssessmentMapper extends EntityMapper<RiskAssessmentDTO, RiskAssessment> {
    @Mapping(target = "risk", source = "risk", qualifiedByName = "riskScope")
    RiskAssessmentDTO toDto(RiskAssessment s);

    @Named("riskScope")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "scope", source = "scope")
    RiskDTO toDtoRiskScope(Risk risk);
}
