package cm.gimac.des.certification.service.mapper;

import cm.gimac.des.certification.domain.Attestation;
import cm.gimac.des.certification.domain.GoLiveDossier;
import cm.gimac.des.certification.service.dto.AttestationDTO;
import cm.gimac.des.certification.service.dto.GoLiveDossierDTO;
import java.util.Objects;
import java.util.UUID;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link Attestation} and its DTO {@link AttestationDTO}.
 */
@Mapper(componentModel = "spring")
public interface AttestationMapper extends EntityMapper<AttestationDTO, Attestation> {
    @Mapping(target = "dossier", source = "dossier", qualifiedByName = "goLiveDossierId")
    AttestationDTO toDto(Attestation s);

    @Named("goLiveDossierId")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    GoLiveDossierDTO toDtoGoLiveDossierId(GoLiveDossier goLiveDossier);

    default String map(UUID value) {
        return Objects.toString(value, null);
    }
}
