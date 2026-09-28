package cm.gimac.des.audit.service.mapper;

import cm.gimac.des.audit.domain.InclusionProof;
import cm.gimac.des.audit.domain.PeriodRoot;
import cm.gimac.des.audit.service.dto.InclusionProofDTO;
import cm.gimac.des.audit.service.dto.PeriodRootDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link InclusionProof} and its DTO {@link InclusionProofDTO}.
 */
@Mapper(componentModel = "spring")
public interface InclusionProofMapper extends EntityMapper<InclusionProofDTO, InclusionProof> {
    @Mapping(target = "periodRoot", source = "periodRoot", qualifiedByName = "periodRootPeriod")
    InclusionProofDTO toDto(InclusionProof s);

    @Named("periodRootPeriod")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "period", source = "period")
    PeriodRootDTO toDtoPeriodRootPeriod(PeriodRoot periodRoot);
}
