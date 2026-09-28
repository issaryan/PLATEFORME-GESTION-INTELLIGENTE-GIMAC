package cm.gimac.des.rapprochement.service.mapper;

import cm.gimac.des.rapprochement.domain.ImportedStatement;
import cm.gimac.des.rapprochement.domain.Reconciliation;
import cm.gimac.des.rapprochement.service.dto.ImportedStatementDTO;
import cm.gimac.des.rapprochement.service.dto.ReconciliationDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link Reconciliation} and its DTO {@link ReconciliationDTO}.
 */
@Mapper(componentModel = "spring")
public interface ReconciliationMapper extends EntityMapper<ReconciliationDTO, Reconciliation> {
    @Mapping(target = "switchStatement", source = "switchStatement", qualifiedByName = "importedStatementReference")
    @Mapping(target = "participantStatement", source = "participantStatement", qualifiedByName = "importedStatementReference")
    @Mapping(target = "settlementStatement", source = "settlementStatement", qualifiedByName = "importedStatementReference")
    ReconciliationDTO toDto(Reconciliation s);

    @Named("importedStatementReference")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "reference", source = "reference")
    ImportedStatementDTO toDtoImportedStatementReference(ImportedStatement importedStatement);
}
