package cm.gimac.des.certification.service.mapper;

import cm.gimac.des.certification.domain.CertificationAttestation;
import cm.gimac.des.certification.domain.ChangeRecord;
import cm.gimac.des.certification.domain.CommissioningFile;
import cm.gimac.des.certification.domain.EvidenceItem;
import cm.gimac.des.certification.service.dto.CertificationAttestationDTO;
import cm.gimac.des.certification.service.dto.ChangeRecordDTO;
import cm.gimac.des.certification.service.dto.CommissioningFileDTO;
import cm.gimac.des.certification.service.dto.EvidenceItemDTO;
import java.util.Objects;
import java.util.UUID;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link CommissioningFile} and its DTO {@link CommissioningFileDTO}.
 */
@Mapper(componentModel = "spring")
public interface CommissioningFileMapper extends EntityMapper<CommissioningFileDTO, CommissioningFile> {
    @Mapping(target = "changeRecord", source = "changeRecord", qualifiedByName = "changeRecordExternalRef")
    @Mapping(target = "attestation", source = "attestation", qualifiedByName = "certificationAttestationAttestationNumber")
    @Mapping(target = "commissioningReport", source = "commissioningReport", qualifiedByName = "evidenceItemId")
    CommissioningFileDTO toDto(CommissioningFile s);

    @Named("changeRecordExternalRef")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "externalRef", source = "externalRef")
    ChangeRecordDTO toDtoChangeRecordExternalRef(ChangeRecord changeRecord);

    @Named("certificationAttestationAttestationNumber")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "attestationNumber", source = "attestationNumber")
    CertificationAttestationDTO toDtoCertificationAttestationAttestationNumber(CertificationAttestation certificationAttestation);

    @Named("evidenceItemId")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    EvidenceItemDTO toDtoEvidenceItemId(EvidenceItem evidenceItem);

    default String map(UUID value) {
        return Objects.toString(value, null);
    }
}
