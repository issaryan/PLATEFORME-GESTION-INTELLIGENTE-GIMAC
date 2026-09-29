package cm.gimac.des.homologation.service.mapper;

import cm.gimac.des.homologation.domain.Coverage;
import cm.gimac.des.homologation.domain.Liaison;
import cm.gimac.des.homologation.service.dto.CoverageDTO;
import cm.gimac.des.homologation.service.dto.LiaisonDTO;
import java.util.Objects;
import java.util.UUID;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link Coverage} and its DTO {@link CoverageDTO}.
 */
@Mapper(componentModel = "spring")
public interface CoverageMapper extends EntityMapper<CoverageDTO, Coverage> {
    @Mapping(target = "liaison", source = "liaison", qualifiedByName = "liaisonId")
    CoverageDTO toDto(Coverage s);

    @Named("liaisonId")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    LiaisonDTO toDtoLiaisonId(Liaison liaison);

    default String map(UUID value) {
        return Objects.toString(value, null);
    }
}
