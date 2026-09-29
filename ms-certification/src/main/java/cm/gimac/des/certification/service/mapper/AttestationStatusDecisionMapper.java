package cm.gimac.des.certification.service.mapper;

import cm.gimac.des.certification.domain.Attestation;
import cm.gimac.des.certification.domain.AttestationStatusDecision;
import cm.gimac.des.certification.service.dto.AttestationDTO;
import cm.gimac.des.certification.service.dto.AttestationStatusDecisionDTO;
import java.util.Objects;
import java.util.UUID;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link AttestationStatusDecision} and its DTO {@link AttestationStatusDecisionDTO}.
 */
@Mapper(componentModel = "spring")
public interface AttestationStatusDecisionMapper extends EntityMapper<AttestationStatusDecisionDTO, AttestationStatusDecision> {
    @Mapping(target = "attestation", source = "attestation", qualifiedByName = "attestationId")
    AttestationStatusDecisionDTO toDto(AttestationStatusDecision s);

    @Named("attestationId")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    AttestationDTO toDtoAttestationId(Attestation attestation);

    default String map(UUID value) {
        return Objects.toString(value, null);
    }
}
