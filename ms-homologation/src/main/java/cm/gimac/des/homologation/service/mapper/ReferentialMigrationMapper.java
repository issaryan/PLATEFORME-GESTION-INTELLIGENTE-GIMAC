package cm.gimac.des.homologation.service.mapper;

import cm.gimac.des.homologation.domain.ReferentialMigration;
import cm.gimac.des.homologation.service.dto.ReferentialMigrationDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link ReferentialMigration} and its DTO {@link ReferentialMigrationDTO}.
 */
@Mapper(componentModel = "spring")
public interface ReferentialMigrationMapper extends EntityMapper<ReferentialMigrationDTO, ReferentialMigration> {}
