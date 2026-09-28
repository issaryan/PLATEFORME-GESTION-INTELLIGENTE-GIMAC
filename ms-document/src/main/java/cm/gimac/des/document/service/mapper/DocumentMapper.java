package cm.gimac.des.document.service.mapper;

import cm.gimac.des.document.domain.Document;
import cm.gimac.des.document.domain.DocumentLineage;
import cm.gimac.des.document.domain.DocumentTypePolicy;
import cm.gimac.des.document.service.dto.DocumentDTO;
import cm.gimac.des.document.service.dto.DocumentLineageDTO;
import cm.gimac.des.document.service.dto.DocumentTypePolicyDTO;
import java.util.Objects;
import java.util.UUID;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link Document} and its DTO {@link DocumentDTO}.
 */
@Mapper(componentModel = "spring")
public interface DocumentMapper extends EntityMapper<DocumentDTO, Document> {
    @Mapping(target = "lineage", source = "lineage", qualifiedByName = "documentLineageId")
    @Mapping(target = "policy", source = "policy", qualifiedByName = "documentTypePolicyId")
    DocumentDTO toDto(Document s);

    @Named("documentLineageId")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    DocumentLineageDTO toDtoDocumentLineageId(DocumentLineage documentLineage);

    @Named("documentTypePolicyId")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    DocumentTypePolicyDTO toDtoDocumentTypePolicyId(DocumentTypePolicy documentTypePolicy);

    default String map(UUID value) {
        return Objects.toString(value, null);
    }
}
