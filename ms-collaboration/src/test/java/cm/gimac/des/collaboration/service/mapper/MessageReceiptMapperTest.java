package cm.gimac.des.collaboration.service.mapper;

import static cm.gimac.des.collaboration.domain.MessageReceiptAsserts.*;
import static cm.gimac.des.collaboration.domain.MessageReceiptTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MessageReceiptMapperTest {

    private MessageReceiptMapper messageReceiptMapper;

    @BeforeEach
    void setUp() {
        messageReceiptMapper = new MessageReceiptMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getMessageReceiptSample1();
        var actual = messageReceiptMapper.toEntity(messageReceiptMapper.toDto(expected));
        assertMessageReceiptAllPropertiesEquals(expected, actual);
    }
}
