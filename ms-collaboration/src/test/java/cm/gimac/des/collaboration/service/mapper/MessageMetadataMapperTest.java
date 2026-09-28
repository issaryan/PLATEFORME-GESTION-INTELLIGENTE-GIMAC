package cm.gimac.des.collaboration.service.mapper;

import static cm.gimac.des.collaboration.domain.MessageMetadataAsserts.*;
import static cm.gimac.des.collaboration.domain.MessageMetadataTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MessageMetadataMapperTest {

    private MessageMetadataMapper messageMetadataMapper;

    @BeforeEach
    void setUp() {
        messageMetadataMapper = new MessageMetadataMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getMessageMetadataSample1();
        var actual = messageMetadataMapper.toEntity(messageMetadataMapper.toDto(expected));
        assertMessageMetadataAllPropertiesEquals(expected, actual);
    }
}
