package cm.gimac.des.incident.service.mapper;

import cm.gimac.des.incident.domain.CrisisCell;
import cm.gimac.des.incident.domain.CrisisLogEntry;
import cm.gimac.des.incident.service.dto.CrisisCellDTO;
import cm.gimac.des.incident.service.dto.CrisisLogEntryDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link CrisisLogEntry} and its DTO {@link CrisisLogEntryDTO}.
 */
@Mapper(componentModel = "spring")
public interface CrisisLogEntryMapper extends EntityMapper<CrisisLogEntryDTO, CrisisLogEntry> {
    @Mapping(target = "cell", source = "cell", qualifiedByName = "crisisCellReference")
    CrisisLogEntryDTO toDto(CrisisLogEntry s);

    @Named("crisisCellReference")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "reference", source = "reference")
    CrisisCellDTO toDtoCrisisCellReference(CrisisCell crisisCell);
}
