package cm.gimac.des.certification.service.mapper;

import cm.gimac.des.certification.domain.CompletenessCondition;
import cm.gimac.des.certification.domain.GoLiveDossier;
import cm.gimac.des.certification.service.dto.CompletenessConditionDTO;
import cm.gimac.des.certification.service.dto.GoLiveDossierDTO;
import java.util.Objects;
import java.util.UUID;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link CompletenessCondition} and its DTO {@link CompletenessConditionDTO}.
 */
@Mapper(componentModel = "spring")
public interface CompletenessConditionMapper extends EntityMapper<CompletenessConditionDTO, CompletenessCondition> {
    @Mapping(target = "dossier", source = "dossier", qualifiedByName = "goLiveDossierId")
    CompletenessConditionDTO toDto(CompletenessCondition s);

    @Named("goLiveDossierId")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    GoLiveDossierDTO toDtoGoLiveDossierId(GoLiveDossier goLiveDossier);

    default String map(UUID value) {
        return Objects.toString(value, null);
    }
}
