package cm.gimac.des.collaboration.service.mapper;

import cm.gimac.des.collaboration.domain.Room;
import cm.gimac.des.collaboration.domain.RoomMember;
import cm.gimac.des.collaboration.service.dto.RoomDTO;
import cm.gimac.des.collaboration.service.dto.RoomMemberDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link RoomMember} and its DTO {@link RoomMemberDTO}.
 */
@Mapper(componentModel = "spring")
public interface RoomMemberMapper extends EntityMapper<RoomMemberDTO, RoomMember> {
    @Mapping(target = "room", source = "room", qualifiedByName = "roomCode")
    RoomMemberDTO toDto(RoomMember s);

    @Named("roomCode")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "code", source = "code")
    RoomDTO toDtoRoomCode(Room room);
}
