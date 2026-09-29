package cm.gimac.des.homologation.service.mapper;

import cm.gimac.des.homologation.domain.EvidenceReference;
import cm.gimac.des.homologation.domain.OperationInstance;
import cm.gimac.des.homologation.service.dto.EvidenceReferenceDTO;
import cm.gimac.des.homologation.service.dto.OperationInstanceDTO;
import java.util.Objects;
import java.util.UUID;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link EvidenceReference} and its DTO {@link EvidenceReferenceDTO}.
 */
@Mapper(componentModel = "spring")
public interface EvidenceReferenceMapper extends EntityMapper<EvidenceReferenceDTO, EvidenceReference> {
    @Mapping(target = "operation", source = "operation", qualifiedByName = "operationInstanceId")
    EvidenceReferenceDTO toDto(EvidenceReference s);

    @Named("operationInstanceId")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    OperationInstanceDTO toDtoOperationInstanceId(OperationInstance operationInstance);

    default String map(UUID value) {
        return Objects.toString(value, null);
    }
}
