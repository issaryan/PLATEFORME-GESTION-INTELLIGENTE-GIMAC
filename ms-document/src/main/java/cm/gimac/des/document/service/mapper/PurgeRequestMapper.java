package cm.gimac.des.document.service.mapper;

import cm.gimac.des.document.domain.Document;
import cm.gimac.des.document.domain.PurgeRequest;
import cm.gimac.des.document.service.dto.DocumentDTO;
import cm.gimac.des.document.service.dto.PurgeRequestDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link PurgeRequest} and its DTO {@link PurgeRequestDTO}.
 */
@Mapper(componentModel = "spring")
public interface PurgeRequestMapper extends EntityMapper<PurgeRequestDTO, PurgeRequest> {
    @Mapping(target = "document", source = "document", qualifiedByName = "documentTitle")
    PurgeRequestDTO toDto(PurgeRequest s);

    @Named("documentTitle")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "title", source = "title")
    DocumentDTO toDtoDocumentTitle(Document document);
}
