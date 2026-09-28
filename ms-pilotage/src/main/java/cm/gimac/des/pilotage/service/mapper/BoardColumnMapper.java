package cm.gimac.des.pilotage.service.mapper;

import cm.gimac.des.pilotage.domain.Board;
import cm.gimac.des.pilotage.domain.BoardColumn;
import cm.gimac.des.pilotage.service.dto.BoardColumnDTO;
import cm.gimac.des.pilotage.service.dto.BoardDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link BoardColumn} and its DTO {@link BoardColumnDTO}.
 */
@Mapper(componentModel = "spring")
public interface BoardColumnMapper extends EntityMapper<BoardColumnDTO, BoardColumn> {
    @Mapping(target = "board", source = "board", qualifiedByName = "boardName")
    BoardColumnDTO toDto(BoardColumn s);

    @Named("boardName")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", source = "name")
    BoardDTO toDtoBoardName(Board board);
}
