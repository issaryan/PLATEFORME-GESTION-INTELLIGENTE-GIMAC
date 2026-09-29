package cm.gimac.des.certification.service.mapper;

import cm.gimac.des.certification.domain.CertificationCircuit;
import cm.gimac.des.certification.service.dto.CertificationCircuitDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link CertificationCircuit} and its DTO {@link CertificationCircuitDTO}.
 */
@Mapper(componentModel = "spring")
public interface CertificationCircuitMapper extends EntityMapper<CertificationCircuitDTO, CertificationCircuit> {}
