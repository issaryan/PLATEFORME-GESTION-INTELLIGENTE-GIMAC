package cm.gimac.des.collaboration.service.mapper;

import cm.gimac.des.collaboration.domain.Room;
import cm.gimac.des.collaboration.service.dto.RoomDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link Room} and its DTO {@link RoomDTO}.
 */
@Mapper(componentModel = "spring")
public interface RoomMapper extends EntityMapper<RoomDTO, Room> {}
