package cm.gimac.des.monitoring.service.mapper;

import cm.gimac.des.monitoring.domain.OnCallRoster;
import cm.gimac.des.monitoring.service.dto.OnCallRosterDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link OnCallRoster} and its DTO {@link OnCallRosterDTO}.
 */
@Mapper(componentModel = "spring")
public interface OnCallRosterMapper extends EntityMapper<OnCallRosterDTO, OnCallRoster> {
    @Mapping(target = "previousRevision", source = "previousRevision", qualifiedByName = "onCallRosterIsoWeek")
    OnCallRosterDTO toDto(OnCallRoster s);

    @Named("onCallRosterIsoWeek")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "isoWeek", source = "isoWeek")
    OnCallRosterDTO toDtoOnCallRosterIsoWeek(OnCallRoster onCallRoster);
}
