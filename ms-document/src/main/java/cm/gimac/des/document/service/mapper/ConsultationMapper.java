package cm.gimac.des.document.service.mapper;

import cm.gimac.des.document.domain.Consultation;
import cm.gimac.des.document.domain.Document;
import cm.gimac.des.document.service.dto.ConsultationDTO;
import cm.gimac.des.document.service.dto.DocumentDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link Consultation} and its DTO {@link ConsultationDTO}.
 */
@Mapper(componentModel = "spring")
public interface ConsultationMapper extends EntityMapper<ConsultationDTO, Consultation> {
    @Mapping(target = "document", source = "document", qualifiedByName = "documentTitle")
    ConsultationDTO toDto(Consultation s);

    @Named("documentTitle")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "title", source = "title")
    DocumentDTO toDtoDocumentTitle(Document document);
}
