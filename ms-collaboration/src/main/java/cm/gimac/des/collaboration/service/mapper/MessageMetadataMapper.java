package cm.gimac.des.collaboration.service.mapper;

import cm.gimac.des.collaboration.domain.MessageMetadata;
import cm.gimac.des.collaboration.domain.Room;
import cm.gimac.des.collaboration.service.dto.MessageMetadataDTO;
import cm.gimac.des.collaboration.service.dto.RoomDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link MessageMetadata} and its DTO {@link MessageMetadataDTO}.
 */
@Mapper(componentModel = "spring")
public interface MessageMetadataMapper extends EntityMapper<MessageMetadataDTO, MessageMetadata> {
    @Mapping(target = "room", source = "room", qualifiedByName = "roomCode")
    MessageMetadataDTO toDto(MessageMetadata s);

    @Named("roomCode")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "code", source = "code")
    RoomDTO toDtoRoomCode(Room room);
}
