package cm.gimac.des.homologation.service.mapper;

import cm.gimac.des.homologation.domain.Liaison;
import cm.gimac.des.homologation.domain.ReferenceCatalog;
import cm.gimac.des.homologation.service.dto.LiaisonDTO;
import cm.gimac.des.homologation.service.dto.ReferenceCatalogDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link Liaison} and its DTO {@link LiaisonDTO}.
 */
@Mapper(componentModel = "spring")
public interface LiaisonMapper extends EntityMapper<LiaisonDTO, Liaison> {
    @Mapping(target = "catalog", source = "catalog", qualifiedByName = "referenceCatalogVersion")
    LiaisonDTO toDto(Liaison s);

    @Named("referenceCatalogVersion")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "version", source = "version")
    ReferenceCatalogDTO toDtoReferenceCatalogVersion(ReferenceCatalog referenceCatalog);
}
