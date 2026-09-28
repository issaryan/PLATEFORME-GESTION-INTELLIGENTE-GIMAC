package cm.gimac.des.pilotage.service.mapper;

import cm.gimac.des.pilotage.domain.Board;
import cm.gimac.des.pilotage.service.dto.BoardDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link Board} and its DTO {@link BoardDTO}.
 */
@Mapper(componentModel = "spring")
public interface BoardMapper extends EntityMapper<BoardDTO, Board> {}
