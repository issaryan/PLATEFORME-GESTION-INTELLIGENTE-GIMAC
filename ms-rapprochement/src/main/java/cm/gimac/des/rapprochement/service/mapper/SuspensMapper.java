package cm.gimac.des.rapprochement.service.mapper;

import cm.gimac.des.rapprochement.domain.Reconciliation;
import cm.gimac.des.rapprochement.domain.Suspens;
import cm.gimac.des.rapprochement.service.dto.ReconciliationDTO;
import cm.gimac.des.rapprochement.service.dto.SuspensDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link Suspens} and its DTO {@link SuspensDTO}.
 */
@Mapper(componentModel = "spring")
public interface SuspensMapper extends EntityMapper<SuspensDTO, Suspens> {
    @Mapping(target = "reconciliation", source = "reconciliation", qualifiedByName = "reconciliationReference")
    SuspensDTO toDto(Suspens s);

    @Named("reconciliationReference")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "reference", source = "reference")
    ReconciliationDTO toDtoReconciliationReference(Reconciliation reconciliation);
}
