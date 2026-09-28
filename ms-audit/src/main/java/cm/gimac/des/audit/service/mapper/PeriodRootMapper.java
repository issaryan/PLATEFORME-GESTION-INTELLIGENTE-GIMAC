package cm.gimac.des.audit.service.mapper;

import cm.gimac.des.audit.domain.ConsistencyProof;
import cm.gimac.des.audit.domain.PeriodRoot;
import cm.gimac.des.audit.service.dto.ConsistencyProofDTO;
import cm.gimac.des.audit.service.dto.PeriodRootDTO;
import java.util.Objects;
import java.util.UUID;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link PeriodRoot} and its DTO {@link PeriodRootDTO}.
 */
@Mapper(componentModel = "spring")
public interface PeriodRootMapper extends EntityMapper<PeriodRootDTO, PeriodRoot> {
    @Mapping(target = "previousRoot", source = "previousRoot", qualifiedByName = "periodRootPeriod")
    @Mapping(target = "consistencyWithPrevious", source = "consistencyWithPrevious", qualifiedByName = "consistencyProofId")
    PeriodRootDTO toDto(PeriodRoot s);

    @Named("periodRootPeriod")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "period", source = "period")
    PeriodRootDTO toDtoPeriodRootPeriod(PeriodRoot periodRoot);

    @Named("consistencyProofId")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    ConsistencyProofDTO toDtoConsistencyProofId(ConsistencyProof consistencyProof);

    default String map(UUID value) {
        return Objects.toString(value, null);
    }
}
