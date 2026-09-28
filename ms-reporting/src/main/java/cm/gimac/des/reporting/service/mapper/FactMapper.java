package cm.gimac.des.reporting.service.mapper;

import cm.gimac.des.reporting.domain.Fact;
import cm.gimac.des.reporting.domain.ImportedStatement;
import cm.gimac.des.reporting.service.dto.FactDTO;
import cm.gimac.des.reporting.service.dto.ImportedStatementDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link Fact} and its DTO {@link FactDTO}.
 */
@Mapper(componentModel = "spring")
public interface FactMapper extends EntityMapper<FactDTO, Fact> {
    @Mapping(target = "statement", source = "statement", qualifiedByName = "importedStatementDigest")
    FactDTO toDto(Fact s);

    @Named("importedStatementDigest")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "digest", source = "digest")
    ImportedStatementDTO toDtoImportedStatementDigest(ImportedStatement importedStatement);
}
