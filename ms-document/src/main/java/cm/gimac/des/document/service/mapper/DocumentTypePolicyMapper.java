package cm.gimac.des.document.service.mapper;

import cm.gimac.des.document.domain.DocumentTypePolicy;
import cm.gimac.des.document.service.dto.DocumentTypePolicyDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link DocumentTypePolicy} and its DTO {@link DocumentTypePolicyDTO}.
 */
@Mapper(componentModel = "spring")
public interface DocumentTypePolicyMapper extends EntityMapper<DocumentTypePolicyDTO, DocumentTypePolicy> {}
