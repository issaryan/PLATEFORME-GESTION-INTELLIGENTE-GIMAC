package cm.gimac.des.homologation.service.mapper;

import cm.gimac.des.homologation.domain.Assignment;
import cm.gimac.des.homologation.domain.OperationInstance;
import cm.gimac.des.homologation.service.dto.AssignmentDTO;
import cm.gimac.des.homologation.service.dto.OperationInstanceDTO;
import java.util.Objects;
import java.util.UUID;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link Assignment} and its DTO {@link AssignmentDTO}.
 */
@Mapper(componentModel = "spring")
public interface AssignmentMapper extends EntityMapper<AssignmentDTO, Assignment> {
    @Mapping(target = "operation", source = "operation", qualifiedByName = "operationInstanceId")
    AssignmentDTO toDto(Assignment s);

    @Named("operationInstanceId")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    OperationInstanceDTO toDtoOperationInstanceId(OperationInstance operationInstance);

    default String map(UUID value) {
        return Objects.toString(value, null);
    }
}
