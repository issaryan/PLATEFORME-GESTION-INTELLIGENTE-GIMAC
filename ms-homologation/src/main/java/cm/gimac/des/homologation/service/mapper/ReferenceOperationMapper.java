package cm.gimac.des.homologation.service.mapper;

import cm.gimac.des.homologation.domain.ReferenceCatalog;
import cm.gimac.des.homologation.domain.ReferenceOperation;
import cm.gimac.des.homologation.service.dto.ReferenceCatalogDTO;
import cm.gimac.des.homologation.service.dto.ReferenceOperationDTO;
import java.util.Set;
import java.util.stream.Collectors;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link ReferenceOperation} and its DTO {@link ReferenceOperationDTO}.
 */
@Mapper(componentModel = "spring")
public interface ReferenceOperationMapper extends EntityMapper<ReferenceOperationDTO, ReferenceOperation> {
    @Mapping(target = "catalog", source = "catalog", qualifiedByName = "referenceCatalogVersion")
    @Mapping(target = "predecessors", source = "predecessors", qualifiedByName = "referenceOperationCodeSet")
    @Mapping(target = "successors", source = "successors", qualifiedByName = "referenceOperationCodeSet")
    ReferenceOperationDTO toDto(ReferenceOperation s);

    @Mapping(target = "removePredecessors", ignore = true)
    @Mapping(target = "successors", ignore = true)
    @Mapping(target = "removeSuccessors", ignore = true)
    ReferenceOperation toEntity(ReferenceOperationDTO referenceOperationDTO);

    @Named("referenceCatalogVersion")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "version", source = "version")
    ReferenceCatalogDTO toDtoReferenceCatalogVersion(ReferenceCatalog referenceCatalog);

    @Named("referenceOperationCode")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "code", source = "code")
    ReferenceOperationDTO toDtoReferenceOperationCode(ReferenceOperation referenceOperation);

    @Named("referenceOperationCodeSet")
    default Set<ReferenceOperationDTO> toDtoReferenceOperationCodeSet(Set<ReferenceOperation> referenceOperation) {
        return referenceOperation.stream().map(this::toDtoReferenceOperationCode).collect(Collectors.toSet());
    }
}
