package cm.gimac.des.homologation.service.mapper;

import cm.gimac.des.homologation.domain.EvidenceFileExport;
import cm.gimac.des.homologation.domain.Liaison;
import cm.gimac.des.homologation.service.dto.EvidenceFileExportDTO;
import cm.gimac.des.homologation.service.dto.LiaisonDTO;
import java.util.Objects;
import java.util.UUID;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link EvidenceFileExport} and its DTO {@link EvidenceFileExportDTO}.
 */
@Mapper(componentModel = "spring")
public interface EvidenceFileExportMapper extends EntityMapper<EvidenceFileExportDTO, EvidenceFileExport> {
    @Mapping(target = "liaison", source = "liaison", qualifiedByName = "liaisonId")
    EvidenceFileExportDTO toDto(EvidenceFileExport s);

    @Named("liaisonId")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    LiaisonDTO toDtoLiaisonId(Liaison liaison);

    default String map(UUID value) {
        return Objects.toString(value, null);
    }
}
