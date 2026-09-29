package cm.gimac.des.homologation.service.mapper;

import cm.gimac.des.homologation.domain.ReferentialVersion;
import cm.gimac.des.homologation.service.dto.ReferentialVersionDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link ReferentialVersion} and its DTO {@link ReferentialVersionDTO}.
 */
@Mapper(componentModel = "spring")
public interface ReferentialVersionMapper extends EntityMapper<ReferentialVersionDTO, ReferentialVersion> {}
