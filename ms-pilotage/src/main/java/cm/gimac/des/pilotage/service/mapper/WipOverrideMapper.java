package cm.gimac.des.pilotage.service.mapper;

import cm.gimac.des.pilotage.domain.BoardColumn;
import cm.gimac.des.pilotage.domain.Task;
import cm.gimac.des.pilotage.domain.WipOverride;
import cm.gimac.des.pilotage.service.dto.BoardColumnDTO;
import cm.gimac.des.pilotage.service.dto.TaskDTO;
import cm.gimac.des.pilotage.service.dto.WipOverrideDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link WipOverride} and its DTO {@link WipOverrideDTO}.
 */
@Mapper(componentModel = "spring")
public interface WipOverrideMapper extends EntityMapper<WipOverrideDTO, WipOverride> {
    @Mapping(target = "boardColumn", source = "boardColumn", qualifiedByName = "boardColumnCode")
    @Mapping(target = "task", source = "task", qualifiedByName = "taskTitle")
    WipOverrideDTO toDto(WipOverride s);

    @Named("boardColumnCode")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "code", source = "code")
    BoardColumnDTO toDtoBoardColumnCode(BoardColumn boardColumn);

    @Named("taskTitle")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "title", source = "title")
    TaskDTO toDtoTaskTitle(Task task);
}
