package cm.gimac.des.reporting.service.mapper;

import cm.gimac.des.reporting.domain.ImportedStatement;
import cm.gimac.des.reporting.service.dto.ImportedStatementDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link ImportedStatement} and its DTO {@link ImportedStatementDTO}.
 */
@Mapper(componentModel = "spring")
public interface ImportedStatementMapper extends EntityMapper<ImportedStatementDTO, ImportedStatement> {
    @Mapping(target = "supersededBy", source = "supersededBy", qualifiedByName = "importedStatementDigest")
    ImportedStatementDTO toDto(ImportedStatement s);

    @Named("importedStatementDigest")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "digest", source = "digest")
    ImportedStatementDTO toDtoImportedStatementDigest(ImportedStatement importedStatement);
}
