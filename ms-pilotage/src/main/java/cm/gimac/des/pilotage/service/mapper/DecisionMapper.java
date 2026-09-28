package cm.gimac.des.pilotage.service.mapper;

import cm.gimac.des.pilotage.domain.Decision;
import cm.gimac.des.pilotage.domain.Meeting;
import cm.gimac.des.pilotage.domain.Task;
import cm.gimac.des.pilotage.service.dto.DecisionDTO;
import cm.gimac.des.pilotage.service.dto.MeetingDTO;
import cm.gimac.des.pilotage.service.dto.TaskDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link Decision} and its DTO {@link DecisionDTO}.
 */
@Mapper(componentModel = "spring")
public interface DecisionMapper extends EntityMapper<DecisionDTO, Decision> {
    @Mapping(target = "task", source = "task", qualifiedByName = "taskTitle")
    @Mapping(target = "meeting", source = "meeting", qualifiedByName = "meetingInstance")
    @Mapping(target = "rectifies", source = "rectifies", qualifiedByName = "decisionText")
    DecisionDTO toDto(Decision s);

    @Named("taskTitle")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "title", source = "title")
    TaskDTO toDtoTaskTitle(Task task);

    @Named("meetingInstance")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "instance", source = "instance")
    MeetingDTO toDtoMeetingInstance(Meeting meeting);

    @Named("decisionText")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "text", source = "text")
    DecisionDTO toDtoDecisionText(Decision decision);
}
