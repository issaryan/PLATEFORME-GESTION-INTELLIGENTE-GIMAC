package cm.gimac.des.pilotage.service.mapper;

import cm.gimac.des.pilotage.domain.ActionSheet;
import cm.gimac.des.pilotage.domain.Board;
import cm.gimac.des.pilotage.domain.BoardColumn;
import cm.gimac.des.pilotage.domain.Task;
import cm.gimac.des.pilotage.service.dto.ActionSheetDTO;
import cm.gimac.des.pilotage.service.dto.BoardColumnDTO;
import cm.gimac.des.pilotage.service.dto.BoardDTO;
import cm.gimac.des.pilotage.service.dto.TaskDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link Task} and its DTO {@link TaskDTO}.
 */
@Mapper(componentModel = "spring")
public interface TaskMapper extends EntityMapper<TaskDTO, Task> {
    @Mapping(target = "board", source = "board", qualifiedByName = "boardName")
    @Mapping(target = "boardColumn", source = "boardColumn", qualifiedByName = "boardColumnCode")
    @Mapping(target = "actionSheet", source = "actionSheet", qualifiedByName = "actionSheetObjective")
    TaskDTO toDto(Task s);

    @Named("boardName")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", source = "name")
    BoardDTO toDtoBoardName(Board board);

    @Named("boardColumnCode")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "code", source = "code")
    BoardColumnDTO toDtoBoardColumnCode(BoardColumn boardColumn);

    @Named("actionSheetObjective")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "objective", source = "objective")
    ActionSheetDTO toDtoActionSheetObjective(ActionSheet actionSheet);
}
