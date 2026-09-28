package cm.gimac.des.reporting.service.mapper;

import cm.gimac.des.reporting.domain.ImportedStatement;
import cm.gimac.des.reporting.domain.IndicatorDefinition;
import cm.gimac.des.reporting.domain.IndicatorValue;
import cm.gimac.des.reporting.service.dto.ImportedStatementDTO;
import cm.gimac.des.reporting.service.dto.IndicatorDefinitionDTO;
import cm.gimac.des.reporting.service.dto.IndicatorValueDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link IndicatorValue} and its DTO {@link IndicatorValueDTO}.
 */
@Mapper(componentModel = "spring")
public interface IndicatorValueMapper extends EntityMapper<IndicatorValueDTO, IndicatorValue> {
    @Mapping(target = "definition", source = "definition", qualifiedByName = "indicatorDefinitionCode")
    @Mapping(target = "statement", source = "statement", qualifiedByName = "importedStatementDigest")
    IndicatorValueDTO toDto(IndicatorValue s);

    @Named("indicatorDefinitionCode")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "code", source = "code")
    IndicatorDefinitionDTO toDtoIndicatorDefinitionCode(IndicatorDefinition indicatorDefinition);

    @Named("importedStatementDigest")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "digest", source = "digest")
    ImportedStatementDTO toDtoImportedStatementDigest(ImportedStatement importedStatement);
}
