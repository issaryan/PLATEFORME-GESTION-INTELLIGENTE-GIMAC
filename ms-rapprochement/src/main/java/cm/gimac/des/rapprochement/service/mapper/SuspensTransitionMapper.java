package cm.gimac.des.rapprochement.service.mapper;

import cm.gimac.des.rapprochement.domain.Suspens;
import cm.gimac.des.rapprochement.domain.SuspensTransition;
import cm.gimac.des.rapprochement.service.dto.SuspensDTO;
import cm.gimac.des.rapprochement.service.dto.SuspensTransitionDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link SuspensTransition} and its DTO {@link SuspensTransitionDTO}.
 */
@Mapper(componentModel = "spring")
public interface SuspensTransitionMapper extends EntityMapper<SuspensTransitionDTO, SuspensTransition> {
    @Mapping(target = "suspens", source = "suspens", qualifiedByName = "suspensReference")
    SuspensTransitionDTO toDto(SuspensTransition s);

    @Named("suspensReference")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "reference", source = "reference")
    SuspensDTO toDtoSuspensReference(Suspens suspens);
}
