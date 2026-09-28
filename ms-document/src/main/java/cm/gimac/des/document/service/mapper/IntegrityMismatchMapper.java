package cm.gimac.des.document.service.mapper;

import cm.gimac.des.document.domain.Document;
import cm.gimac.des.document.domain.IntegrityCheck;
import cm.gimac.des.document.domain.IntegrityMismatch;
import cm.gimac.des.document.service.dto.DocumentDTO;
import cm.gimac.des.document.service.dto.IntegrityCheckDTO;
import cm.gimac.des.document.service.dto.IntegrityMismatchDTO;
import java.util.Objects;
import java.util.UUID;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link IntegrityMismatch} and its DTO {@link IntegrityMismatchDTO}.
 */
@Mapper(componentModel = "spring")
public interface IntegrityMismatchMapper extends EntityMapper<IntegrityMismatchDTO, IntegrityMismatch> {
    @Mapping(target = "integrityCheck", source = "integrityCheck", qualifiedByName = "integrityCheckId")
    @Mapping(target = "document", source = "document", qualifiedByName = "documentTitle")
    IntegrityMismatchDTO toDto(IntegrityMismatch s);

    @Named("integrityCheckId")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    IntegrityCheckDTO toDtoIntegrityCheckId(IntegrityCheck integrityCheck);

    @Named("documentTitle")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "title", source = "title")
    DocumentDTO toDtoDocumentTitle(Document document);

    default String map(UUID value) {
        return Objects.toString(value, null);
    }
}
