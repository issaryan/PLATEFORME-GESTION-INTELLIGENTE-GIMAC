package cm.gimac.des.certification.service.mapper;

import cm.gimac.des.certification.domain.GoLiveDossier;
import cm.gimac.des.certification.domain.GoLiveReport;
import cm.gimac.des.certification.service.dto.GoLiveDossierDTO;
import cm.gimac.des.certification.service.dto.GoLiveReportDTO;
import java.util.Objects;
import java.util.UUID;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link GoLiveReport} and its DTO {@link GoLiveReportDTO}.
 */
@Mapper(componentModel = "spring")
public interface GoLiveReportMapper extends EntityMapper<GoLiveReportDTO, GoLiveReport> {
    @Mapping(target = "dossier", source = "dossier", qualifiedByName = "goLiveDossierId")
    GoLiveReportDTO toDto(GoLiveReport s);

    @Named("goLiveDossierId")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    GoLiveDossierDTO toDtoGoLiveDossierId(GoLiveDossier goLiveDossier);

    default String map(UUID value) {
        return Objects.toString(value, null);
    }
}
