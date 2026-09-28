package cm.gimac.des.collaboration.service.mapper;

import cm.gimac.des.collaboration.domain.Conference;
import cm.gimac.des.collaboration.domain.Room;
import cm.gimac.des.collaboration.service.dto.ConferenceDTO;
import cm.gimac.des.collaboration.service.dto.RoomDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link Conference} and its DTO {@link ConferenceDTO}.
 */
@Mapper(componentModel = "spring")
public interface ConferenceMapper extends EntityMapper<ConferenceDTO, Conference> {
    @Mapping(target = "room", source = "room", qualifiedByName = "roomCode")
    ConferenceDTO toDto(Conference s);

    @Named("roomCode")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "code", source = "code")
    RoomDTO toDtoRoomCode(Room room);
}
