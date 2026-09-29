package cm.gimac.des.homologation.service.mapper;

import cm.gimac.des.homologation.domain.ReferenceOperation;
import cm.gimac.des.homologation.domain.ReferentialVersion;
import cm.gimac.des.homologation.service.dto.ReferenceOperationDTO;
import cm.gimac.des.homologation.service.dto.ReferentialVersionDTO;
import java.util.Objects;
import java.util.UUID;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link ReferenceOperation} and its DTO {@link ReferenceOperationDTO}.
 */
@Mapper(componentModel = "spring")
public interface ReferenceOperationMapper extends EntityMapper<ReferenceOperationDTO, ReferenceOperation> {
    @Mapping(target = "version", source = "version", qualifiedByName = "referentialVersionId")
    ReferenceOperationDTO toDto(ReferenceOperation s);

    @Named("referentialVersionId")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    ReferentialVersionDTO toDtoReferentialVersionId(ReferentialVersion referentialVersion);

    default String map(UUID value) {
        return Objects.toString(value, null);
    }
}
