package cm.gimac.des.incident.service.mapper;

import cm.gimac.des.incident.domain.CrisisCell;
import cm.gimac.des.incident.domain.Incident;
import cm.gimac.des.incident.service.dto.CrisisCellDTO;
import cm.gimac.des.incident.service.dto.IncidentDTO;
import java.util.Objects;
import java.util.UUID;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link CrisisCell} and its DTO {@link CrisisCellDTO}.
 */
@Mapper(componentModel = "spring")
public interface CrisisCellMapper extends EntityMapper<CrisisCellDTO, CrisisCell> {
    @Mapping(target = "incident", source = "incident", qualifiedByName = "incidentId")
    CrisisCellDTO toDto(CrisisCell s);

    @Named("incidentId")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    IncidentDTO toDtoIncidentId(Incident incident);

    default String map(UUID value) {
        return Objects.toString(value, null);
    }
}
