package cm.gimac.des.homologation.service.mapper;

import cm.gimac.des.homologation.domain.Liaison;
import cm.gimac.des.homologation.domain.Operation;
import cm.gimac.des.homologation.domain.ReferenceOperation;
import cm.gimac.des.homologation.service.dto.LiaisonDTO;
import cm.gimac.des.homologation.service.dto.OperationDTO;
import cm.gimac.des.homologation.service.dto.ReferenceOperationDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link Operation} and its DTO {@link OperationDTO}.
 */
@Mapper(componentModel = "spring")
public interface OperationMapper extends EntityMapper<OperationDTO, Operation> {
    @Mapping(target = "liaison", source = "liaison", qualifiedByName = "liaisonLiaisonCode")
    @Mapping(target = "reference", source = "reference", qualifiedByName = "referenceOperationCode")
    OperationDTO toDto(Operation s);

    @Named("liaisonLiaisonCode")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "liaisonCode", source = "liaisonCode")
    LiaisonDTO toDtoLiaisonLiaisonCode(Liaison liaison);

    @Named("referenceOperationCode")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "code", source = "code")
    ReferenceOperationDTO toDtoReferenceOperationCode(ReferenceOperation referenceOperation);
}
