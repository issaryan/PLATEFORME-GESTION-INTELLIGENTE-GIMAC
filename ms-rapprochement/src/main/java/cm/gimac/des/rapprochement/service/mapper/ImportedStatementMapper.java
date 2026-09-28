package cm.gimac.des.rapprochement.service.mapper;

import cm.gimac.des.rapprochement.domain.ImportedStatement;
import cm.gimac.des.rapprochement.service.dto.ImportedStatementDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link ImportedStatement} and its DTO {@link ImportedStatementDTO}.
 */
@Mapper(componentModel = "spring")
public interface ImportedStatementMapper extends EntityMapper<ImportedStatementDTO, ImportedStatement> {}
