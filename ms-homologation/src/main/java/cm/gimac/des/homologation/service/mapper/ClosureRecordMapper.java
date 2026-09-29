package cm.gimac.des.homologation.service.mapper;

import cm.gimac.des.homologation.domain.ClosureRecord;
import cm.gimac.des.homologation.domain.OperationInstance;
import cm.gimac.des.homologation.service.dto.ClosureRecordDTO;
import cm.gimac.des.homologation.service.dto.OperationInstanceDTO;
import java.util.Objects;
import java.util.UUID;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link ClosureRecord} and its DTO {@link ClosureRecordDTO}.
 */
@Mapper(componentModel = "spring")
public interface ClosureRecordMapper extends EntityMapper<ClosureRecordDTO, ClosureRecord> {
    @Mapping(target = "operation", source = "operation", qualifiedByName = "operationInstanceId")
    ClosureRecordDTO toDto(ClosureRecord s);

    @Named("operationInstanceId")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    OperationInstanceDTO toDtoOperationInstanceId(OperationInstance operationInstance);

    default String map(UUID value) {
        return Objects.toString(value, null);
    }
}
