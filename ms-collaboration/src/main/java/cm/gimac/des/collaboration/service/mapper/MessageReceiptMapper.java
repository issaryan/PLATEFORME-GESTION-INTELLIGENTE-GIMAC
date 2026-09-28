package cm.gimac.des.collaboration.service.mapper;

import cm.gimac.des.collaboration.domain.MessageMetadata;
import cm.gimac.des.collaboration.domain.MessageReceipt;
import cm.gimac.des.collaboration.service.dto.MessageMetadataDTO;
import cm.gimac.des.collaboration.service.dto.MessageReceiptDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link MessageReceipt} and its DTO {@link MessageReceiptDTO}.
 */
@Mapper(componentModel = "spring")
public interface MessageReceiptMapper extends EntityMapper<MessageReceiptDTO, MessageReceipt> {
    @Mapping(target = "message", source = "message", qualifiedByName = "messageMetadataMatrixEventId")
    MessageReceiptDTO toDto(MessageReceipt s);

    @Named("messageMetadataMatrixEventId")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "matrixEventId", source = "matrixEventId")
    MessageMetadataDTO toDtoMessageMetadataMatrixEventId(MessageMetadata messageMetadata);
}
