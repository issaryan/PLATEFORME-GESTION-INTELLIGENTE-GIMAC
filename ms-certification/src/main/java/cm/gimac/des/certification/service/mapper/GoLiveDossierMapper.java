package cm.gimac.des.certification.service.mapper;

import cm.gimac.des.certification.domain.CertificationCircuit;
import cm.gimac.des.certification.domain.GoLiveDossier;
import cm.gimac.des.certification.service.dto.CertificationCircuitDTO;
import cm.gimac.des.certification.service.dto.GoLiveDossierDTO;
import java.util.Objects;
import java.util.UUID;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link GoLiveDossier} and its DTO {@link GoLiveDossierDTO}.
 */
@Mapper(componentModel = "spring")
public interface GoLiveDossierMapper extends EntityMapper<GoLiveDossierDTO, GoLiveDossier> {
    @Mapping(target = "circuit", source = "circuit", qualifiedByName = "certificationCircuitId")
    GoLiveDossierDTO toDto(GoLiveDossier s);

    @Named("certificationCircuitId")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    CertificationCircuitDTO toDtoCertificationCircuitId(CertificationCircuit certificationCircuit);

    default String map(UUID value) {
        return Objects.toString(value, null);
    }
}
