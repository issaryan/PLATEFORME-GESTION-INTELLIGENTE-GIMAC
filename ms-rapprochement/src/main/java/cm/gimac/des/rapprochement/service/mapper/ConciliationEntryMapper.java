package cm.gimac.des.rapprochement.service.mapper;

import cm.gimac.des.rapprochement.domain.ConciliationEntry;
import cm.gimac.des.rapprochement.domain.Suspens;
import cm.gimac.des.rapprochement.service.dto.ConciliationEntryDTO;
import cm.gimac.des.rapprochement.service.dto.SuspensDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link ConciliationEntry} and its DTO {@link ConciliationEntryDTO}.
 */
@Mapper(componentModel = "spring")
public interface ConciliationEntryMapper extends EntityMapper<ConciliationEntryDTO, ConciliationEntry> {
    @Mapping(target = "suspens", source = "suspens", qualifiedByName = "suspensReference")
    ConciliationEntryDTO toDto(ConciliationEntry s);

    @Named("suspensReference")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "reference", source = "reference")
    SuspensDTO toDtoSuspensReference(Suspens suspens);
}
