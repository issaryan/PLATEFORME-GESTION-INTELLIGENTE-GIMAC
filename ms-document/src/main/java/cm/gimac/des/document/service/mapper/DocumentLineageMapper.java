package cm.gimac.des.document.service.mapper;

import cm.gimac.des.document.domain.Document;
import cm.gimac.des.document.domain.DocumentLineage;
import cm.gimac.des.document.service.dto.DocumentDTO;
import cm.gimac.des.document.service.dto.DocumentLineageDTO;
import java.util.Objects;
import java.util.UUID;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link DocumentLineage} and its DTO {@link DocumentLineageDTO}.
 */
@Mapper(componentModel = "spring")
public interface DocumentLineageMapper extends EntityMapper<DocumentLineageDTO, DocumentLineage> {
    @Mapping(target = "currentDocument", source = "currentDocument", qualifiedByName = "documentId")
    DocumentLineageDTO toDto(DocumentLineage s);

    @Named("documentId")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    DocumentDTO toDtoDocumentId(Document document);

    default String map(UUID value) {
        return Objects.toString(value, null);
    }
}
