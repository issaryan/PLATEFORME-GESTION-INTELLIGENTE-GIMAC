package cm.gimac.des.homologation.service.mapper;

import cm.gimac.des.homologation.domain.Liaison;
import cm.gimac.des.homologation.domain.OperationInstance;
import cm.gimac.des.homologation.domain.ReferenceOperation;
import cm.gimac.des.homologation.service.dto.LiaisonDTO;
import cm.gimac.des.homologation.service.dto.OperationInstanceDTO;
import cm.gimac.des.homologation.service.dto.ReferenceOperationDTO;
import java.util.Objects;
import java.util.UUID;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link OperationInstance} and its DTO {@link OperationInstanceDTO}.
 */
@Mapper(componentModel = "spring")
public interface OperationInstanceMapper extends EntityMapper<OperationInstanceDTO, OperationInstance> {
    @Mapping(target = "liaison", source = "liaison", qualifiedByName = "liaisonId")
    @Mapping(target = "referenceOperation", source = "referenceOperation", qualifiedByName = "referenceOperationId")
    OperationInstanceDTO toDto(OperationInstance s);

    @Named("liaisonId")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    LiaisonDTO toDtoLiaisonId(Liaison liaison);

    @Named("referenceOperationId")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    ReferenceOperationDTO toDtoReferenceOperationId(ReferenceOperation referenceOperation);

    default String map(UUID value) {
        return Objects.toString(value, null);
    }
}
