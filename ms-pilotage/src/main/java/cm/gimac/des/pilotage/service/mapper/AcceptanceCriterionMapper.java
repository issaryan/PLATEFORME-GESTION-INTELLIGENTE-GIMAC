package cm.gimac.des.pilotage.service.mapper;

import cm.gimac.des.pilotage.domain.AcceptanceCriterion;
import cm.gimac.des.pilotage.domain.Task;
import cm.gimac.des.pilotage.service.dto.AcceptanceCriterionDTO;
import cm.gimac.des.pilotage.service.dto.TaskDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link AcceptanceCriterion} and its DTO {@link AcceptanceCriterionDTO}.
 */
@Mapper(componentModel = "spring")
public interface AcceptanceCriterionMapper extends EntityMapper<AcceptanceCriterionDTO, AcceptanceCriterion> {
    @Mapping(target = "task", source = "task", qualifiedByName = "taskTitle")
    AcceptanceCriterionDTO toDto(AcceptanceCriterion s);

    @Named("taskTitle")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "title", source = "title")
    TaskDTO toDtoTaskTitle(Task task);
}
