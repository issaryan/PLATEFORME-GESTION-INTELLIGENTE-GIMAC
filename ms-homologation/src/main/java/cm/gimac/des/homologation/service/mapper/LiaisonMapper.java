package cm.gimac.des.homologation.service.mapper;

import cm.gimac.des.homologation.domain.Liaison;
import cm.gimac.des.homologation.domain.ReferentialVersion;
import cm.gimac.des.homologation.service.dto.LiaisonDTO;
import cm.gimac.des.homologation.service.dto.ReferentialVersionDTO;
import java.util.Objects;
import java.util.UUID;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link Liaison} and its DTO {@link LiaisonDTO}.
 */
@Mapper(componentModel = "spring")
public interface LiaisonMapper extends EntityMapper<LiaisonDTO, Liaison> {
    @Mapping(target = "version", source = "version", qualifiedByName = "referentialVersionId")
    LiaisonDTO toDto(Liaison s);

    @Named("referentialVersionId")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    ReferentialVersionDTO toDtoReferentialVersionId(ReferentialVersion referentialVersion);

    default String map(UUID value) {
        return Objects.toString(value, null);
    }
}
