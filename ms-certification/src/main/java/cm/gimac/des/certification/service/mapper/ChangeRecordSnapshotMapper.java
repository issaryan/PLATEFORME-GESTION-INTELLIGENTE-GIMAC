package cm.gimac.des.certification.service.mapper;

import cm.gimac.des.certification.domain.ChangeRecordSnapshot;
import cm.gimac.des.certification.domain.GoLiveDossier;
import cm.gimac.des.certification.service.dto.ChangeRecordSnapshotDTO;
import cm.gimac.des.certification.service.dto.GoLiveDossierDTO;
import java.util.Objects;
import java.util.UUID;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link ChangeRecordSnapshot} and its DTO {@link ChangeRecordSnapshotDTO}.
 */
@Mapper(componentModel = "spring")
public interface ChangeRecordSnapshotMapper extends EntityMapper<ChangeRecordSnapshotDTO, ChangeRecordSnapshot> {
    @Mapping(target = "dossier", source = "dossier", qualifiedByName = "goLiveDossierId")
    ChangeRecordSnapshotDTO toDto(ChangeRecordSnapshot s);

    @Named("goLiveDossierId")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    GoLiveDossierDTO toDtoGoLiveDossierId(GoLiveDossier goLiveDossier);

    default String map(UUID value) {
        return Objects.toString(value, null);
    }
}
