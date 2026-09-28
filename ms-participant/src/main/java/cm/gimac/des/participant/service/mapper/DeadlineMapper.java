package cm.gimac.des.participant.service.mapper;

import cm.gimac.des.participant.domain.Convention;
import cm.gimac.des.participant.domain.Deadline;
import cm.gimac.des.participant.domain.Institution;
import cm.gimac.des.participant.domain.LegalDocument;
import cm.gimac.des.participant.service.dto.ConventionDTO;
import cm.gimac.des.participant.service.dto.DeadlineDTO;
import cm.gimac.des.participant.service.dto.InstitutionDTO;
import cm.gimac.des.participant.service.dto.LegalDocumentDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link Deadline} and its DTO {@link DeadlineDTO}.
 */
@Mapper(componentModel = "spring")
public interface DeadlineMapper extends EntityMapper<DeadlineDTO, Deadline> {
    @Mapping(target = "institution", source = "institution", qualifiedByName = "institutionParticipantCode")
    @Mapping(target = "legalDocument", source = "legalDocument", qualifiedByName = "legalDocumentReference")
    @Mapping(target = "convention", source = "convention", qualifiedByName = "conventionReference")
    DeadlineDTO toDto(Deadline s);

    @Named("institutionParticipantCode")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "participantCode", source = "participantCode")
    InstitutionDTO toDtoInstitutionParticipantCode(Institution institution);

    @Named("legalDocumentReference")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "reference", source = "reference")
    LegalDocumentDTO toDtoLegalDocumentReference(LegalDocument legalDocument);

    @Named("conventionReference")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "reference", source = "reference")
    ConventionDTO toDtoConventionReference(Convention convention);
}
