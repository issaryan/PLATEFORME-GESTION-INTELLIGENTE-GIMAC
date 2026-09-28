package cm.gimac.des.notification.service.mapper;

import static cm.gimac.des.notification.domain.MessageTemplateAsserts.*;
import static cm.gimac.des.notification.domain.MessageTemplateTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MessageTemplateMapperTest {

    private MessageTemplateMapper messageTemplateMapper;

    @BeforeEach
    void setUp() {
        messageTemplateMapper = new MessageTemplateMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getMessageTemplateSample1();
        var actual = messageTemplateMapper.toEntity(messageTemplateMapper.toDto(expected));
        assertMessageTemplateAllPropertiesEquals(expected, actual);
    }
}
