package cm.gimac.des.rapprochement.service.mapper;

import cm.gimac.des.rapprochement.domain.ImportedStatement;
import cm.gimac.des.rapprochement.domain.StatementLine;
import cm.gimac.des.rapprochement.service.dto.ImportedStatementDTO;
import cm.gimac.des.rapprochement.service.dto.StatementLineDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link StatementLine} and its DTO {@link StatementLineDTO}.
 */
@Mapper(componentModel = "spring")
public interface StatementLineMapper extends EntityMapper<StatementLineDTO, StatementLine> {
    @Mapping(target = "statement", source = "statement", qualifiedByName = "importedStatementReference")
    StatementLineDTO toDto(StatementLine s);

    @Named("importedStatementReference")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "reference", source = "reference")
    ImportedStatementDTO toDtoImportedStatementReference(ImportedStatement importedStatement);
}
