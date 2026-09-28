package cm.gimac.des.homologation.service.mapper;

import cm.gimac.des.homologation.domain.ReferenceCatalog;
import cm.gimac.des.homologation.service.dto.ReferenceCatalogDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link ReferenceCatalog} and its DTO {@link ReferenceCatalogDTO}.
 */
@Mapper(componentModel = "spring")
public interface ReferenceCatalogMapper extends EntityMapper<ReferenceCatalogDTO, ReferenceCatalog> {}
