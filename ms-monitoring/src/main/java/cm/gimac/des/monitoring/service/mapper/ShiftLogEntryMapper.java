package cm.gimac.des.monitoring.service.mapper;

import cm.gimac.des.monitoring.domain.Shift;
import cm.gimac.des.monitoring.domain.ShiftLogEntry;
import cm.gimac.des.monitoring.service.dto.ShiftDTO;
import cm.gimac.des.monitoring.service.dto.ShiftLogEntryDTO;
import java.util.Objects;
import java.util.UUID;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link ShiftLogEntry} and its DTO {@link ShiftLogEntryDTO}.
 */
@Mapper(componentModel = "spring")
public interface ShiftLogEntryMapper extends EntityMapper<ShiftLogEntryDTO, ShiftLogEntry> {
    @Mapping(target = "shift", source = "shift", qualifiedByName = "shiftReference")
    @Mapping(target = "corrects", source = "corrects", qualifiedByName = "shiftLogEntryId")
    ShiftLogEntryDTO toDto(ShiftLogEntry s);

    @Named("shiftReference")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "reference", source = "reference")
    ShiftDTO toDtoShiftReference(Shift shift);

    @Named("shiftLogEntryId")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    ShiftLogEntryDTO toDtoShiftLogEntryId(ShiftLogEntry shiftLogEntry);

    default String map(UUID value) {
        return Objects.toString(value, null);
    }
}
