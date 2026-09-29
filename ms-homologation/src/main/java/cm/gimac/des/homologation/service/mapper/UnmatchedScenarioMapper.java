package cm.gimac.des.homologation.service.mapper;

import cm.gimac.des.homologation.domain.Liaison;
import cm.gimac.des.homologation.domain.UnmatchedScenario;
import cm.gimac.des.homologation.service.dto.LiaisonDTO;
import cm.gimac.des.homologation.service.dto.UnmatchedScenarioDTO;
import java.util.Objects;
import java.util.UUID;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link UnmatchedScenario} and its DTO {@link UnmatchedScenarioDTO}.
 */
@Mapper(componentModel = "spring")
public interface UnmatchedScenarioMapper extends EntityMapper<UnmatchedScenarioDTO, UnmatchedScenario> {
    @Mapping(target = "liaison", source = "liaison", qualifiedByName = "liaisonId")
    UnmatchedScenarioDTO toDto(UnmatchedScenario s);

    @Named("liaisonId")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    LiaisonDTO toDtoLiaisonId(Liaison liaison);

    default String map(UUID value) {
        return Objects.toString(value, null);
    }
}
