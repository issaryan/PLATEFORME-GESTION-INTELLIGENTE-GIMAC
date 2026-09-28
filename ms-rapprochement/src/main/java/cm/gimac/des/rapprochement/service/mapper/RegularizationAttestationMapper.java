package cm.gimac.des.rapprochement.service.mapper;

import cm.gimac.des.rapprochement.domain.RegularizationAttestation;
import cm.gimac.des.rapprochement.domain.Suspens;
import cm.gimac.des.rapprochement.service.dto.RegularizationAttestationDTO;
import cm.gimac.des.rapprochement.service.dto.SuspensDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link RegularizationAttestation} and its DTO {@link RegularizationAttestationDTO}.
 */
@Mapper(componentModel = "spring")
public interface RegularizationAttestationMapper extends EntityMapper<RegularizationAttestationDTO, RegularizationAttestation> {
    @Mapping(target = "suspens", source = "suspens", qualifiedByName = "suspensReference")
    RegularizationAttestationDTO toDto(RegularizationAttestation s);

    @Named("suspensReference")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "reference", source = "reference")
    SuspensDTO toDtoSuspensReference(Suspens suspens);
}
