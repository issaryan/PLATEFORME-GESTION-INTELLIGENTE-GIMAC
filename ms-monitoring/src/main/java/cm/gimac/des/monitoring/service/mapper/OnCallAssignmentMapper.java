package cm.gimac.des.monitoring.service.mapper;

import cm.gimac.des.monitoring.domain.OnCallAssignment;
import cm.gimac.des.monitoring.domain.OnCallRoster;
import cm.gimac.des.monitoring.service.dto.OnCallAssignmentDTO;
import cm.gimac.des.monitoring.service.dto.OnCallRosterDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link OnCallAssignment} and its DTO {@link OnCallAssignmentDTO}.
 */
@Mapper(componentModel = "spring")
public interface OnCallAssignmentMapper extends EntityMapper<OnCallAssignmentDTO, OnCallAssignment> {
    @Mapping(target = "roster", source = "roster", qualifiedByName = "onCallRosterIsoWeek")
    OnCallAssignmentDTO toDto(OnCallAssignment s);

    @Named("onCallRosterIsoWeek")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "isoWeek", source = "isoWeek")
    OnCallRosterDTO toDtoOnCallRosterIsoWeek(OnCallRoster onCallRoster);
}
