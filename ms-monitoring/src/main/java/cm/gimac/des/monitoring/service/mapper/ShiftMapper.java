package cm.gimac.des.monitoring.service.mapper;

import cm.gimac.des.monitoring.domain.OnCallRoster;
import cm.gimac.des.monitoring.domain.Shift;
import cm.gimac.des.monitoring.service.dto.OnCallRosterDTO;
import cm.gimac.des.monitoring.service.dto.ShiftDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link Shift} and its DTO {@link ShiftDTO}.
 */
@Mapper(componentModel = "spring")
public interface ShiftMapper extends EntityMapper<ShiftDTO, Shift> {
    @Mapping(target = "roster", source = "roster", qualifiedByName = "onCallRosterIsoWeek")
    ShiftDTO toDto(Shift s);

    @Named("onCallRosterIsoWeek")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "isoWeek", source = "isoWeek")
    OnCallRosterDTO toDtoOnCallRosterIsoWeek(OnCallRoster onCallRoster);
}
