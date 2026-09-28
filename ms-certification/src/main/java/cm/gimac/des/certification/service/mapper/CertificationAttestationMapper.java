package cm.gimac.des.certification.service.mapper;

import cm.gimac.des.certification.domain.CertificationAttestation;
import cm.gimac.des.certification.service.dto.CertificationAttestationDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link CertificationAttestation} and its DTO {@link CertificationAttestationDTO}.
 */
@Mapper(componentModel = "spring")
public interface CertificationAttestationMapper extends EntityMapper<CertificationAttestationDTO, CertificationAttestation> {}
