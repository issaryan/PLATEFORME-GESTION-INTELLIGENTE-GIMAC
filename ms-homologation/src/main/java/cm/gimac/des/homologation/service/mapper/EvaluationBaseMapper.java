package cm.gimac.des.homologation.service.mapper;

import cm.gimac.des.homologation.domain.EvaluationBase;
import cm.gimac.des.homologation.domain.Liaison;
import cm.gimac.des.homologation.service.dto.EvaluationBaseDTO;
import cm.gimac.des.homologation.service.dto.LiaisonDTO;
import java.util.Objects;
import java.util.UUID;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link EvaluationBase} and its DTO {@link EvaluationBaseDTO}.
 */
@Mapper(componentModel = "spring")
public interface EvaluationBaseMapper extends EntityMapper<EvaluationBaseDTO, EvaluationBase> {
    @Mapping(target = "liaison", source = "liaison", qualifiedByName = "liaisonId")
    EvaluationBaseDTO toDto(EvaluationBase s);

    @Named("liaisonId")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    LiaisonDTO toDtoLiaisonId(Liaison liaison);

    default String map(UUID value) {
        return Objects.toString(value, null);
    }
}
