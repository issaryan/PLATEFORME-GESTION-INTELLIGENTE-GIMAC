package cm.gimac.des.homologation.service.mapper;

import cm.gimac.des.homologation.domain.Liaison;
import cm.gimac.des.homologation.domain.Operation;
import cm.gimac.des.homologation.domain.TestScenario;
import cm.gimac.des.homologation.service.dto.LiaisonDTO;
import cm.gimac.des.homologation.service.dto.OperationDTO;
import cm.gimac.des.homologation.service.dto.TestScenarioDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link TestScenario} and its DTO {@link TestScenarioDTO}.
 */
@Mapper(componentModel = "spring")
public interface TestScenarioMapper extends EntityMapper<TestScenarioDTO, TestScenario> {
    @Mapping(target = "liaison", source = "liaison", qualifiedByName = "liaisonLiaisonCode")
    @Mapping(target = "operation", source = "operation", qualifiedByName = "operationOperationCode")
    TestScenarioDTO toDto(TestScenario s);

    @Named("liaisonLiaisonCode")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "liaisonCode", source = "liaisonCode")
    LiaisonDTO toDtoLiaisonLiaisonCode(Liaison liaison);

    @Named("operationOperationCode")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "operationCode", source = "operationCode")
    OperationDTO toDtoOperationOperationCode(Operation operation);
}
