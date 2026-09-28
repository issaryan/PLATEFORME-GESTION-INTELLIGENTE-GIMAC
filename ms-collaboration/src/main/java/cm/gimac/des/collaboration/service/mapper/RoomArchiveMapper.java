package cm.gimac.des.collaboration.service.mapper;

import cm.gimac.des.collaboration.domain.Room;
import cm.gimac.des.collaboration.domain.RoomArchive;
import cm.gimac.des.collaboration.service.dto.RoomArchiveDTO;
import cm.gimac.des.collaboration.service.dto.RoomDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link RoomArchive} and its DTO {@link RoomArchiveDTO}.
 */
@Mapper(componentModel = "spring")
public interface RoomArchiveMapper extends EntityMapper<RoomArchiveDTO, RoomArchive> {
    @Mapping(target = "room", source = "room", qualifiedByName = "roomCode")
    RoomArchiveDTO toDto(RoomArchive s);

    @Named("roomCode")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "code", source = "code")
    RoomDTO toDtoRoomCode(Room room);
}
