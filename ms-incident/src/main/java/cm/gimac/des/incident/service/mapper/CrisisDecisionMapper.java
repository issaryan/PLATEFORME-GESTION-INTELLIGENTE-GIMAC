package cm.gimac.des.incident.service.mapper;

import cm.gimac.des.incident.domain.CrisisCell;
import cm.gimac.des.incident.domain.CrisisDecision;
import cm.gimac.des.incident.service.dto.CrisisCellDTO;
import cm.gimac.des.incident.service.dto.CrisisDecisionDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link CrisisDecision} and its DTO {@link CrisisDecisionDTO}.
 */
@Mapper(componentModel = "spring")
public interface CrisisDecisionMapper extends EntityMapper<CrisisDecisionDTO, CrisisDecision> {
    @Mapping(target = "cell", source = "cell", qualifiedByName = "crisisCellReference")
    CrisisDecisionDTO toDto(CrisisDecision s);

    @Named("crisisCellReference")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "reference", source = "reference")
    CrisisCellDTO toDtoCrisisCellReference(CrisisCell crisisCell);
}
