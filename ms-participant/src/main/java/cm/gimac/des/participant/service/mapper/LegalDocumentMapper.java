package cm.gimac.des.participant.service.mapper;

import cm.gimac.des.participant.domain.Institution;
import cm.gimac.des.participant.domain.LegalDocument;
import cm.gimac.des.participant.service.dto.InstitutionDTO;
import cm.gimac.des.participant.service.dto.LegalDocumentDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link LegalDocument} and its DTO {@link LegalDocumentDTO}.
 */
@Mapper(componentModel = "spring")
public interface LegalDocumentMapper extends EntityMapper<LegalDocumentDTO, LegalDocument> {
    @Mapping(target = "previousDocument", source = "previousDocument", qualifiedByName = "legalDocumentReference")
    @Mapping(target = "institution", source = "institution", qualifiedByName = "institutionParticipantCode")
    LegalDocumentDTO toDto(LegalDocument s);

    @Named("legalDocumentReference")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "reference", source = "reference")
    LegalDocumentDTO toDtoLegalDocumentReference(LegalDocument legalDocument);

    @Named("institutionParticipantCode")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "participantCode", source = "participantCode")
    InstitutionDTO toDtoInstitutionParticipantCode(Institution institution);
}
