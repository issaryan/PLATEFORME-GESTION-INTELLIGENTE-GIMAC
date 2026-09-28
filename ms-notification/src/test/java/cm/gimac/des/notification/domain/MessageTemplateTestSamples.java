package cm.gimac.des.notification.domain;

import java.util.UUID;

public class MessageTemplateTestSamples {

    public static MessageTemplate getMessageTemplateSample1() {
        return new MessageTemplate()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .notificationType("notificationType1")
            .language("language1")
            .subjectPattern("subjectPattern1")
            .variables("variables1");
    }

    public static MessageTemplate getMessageTemplateSample2() {
        return new MessageTemplate()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .notificationType("notificationType2")
            .language("language2")
            .subjectPattern("subjectPattern2")
            .variables("variables2");
    }

    public static MessageTemplate getMessageTemplateRandomSampleGenerator() {
        return new MessageTemplate()
            .id(UUID.randomUUID())
            .notificationType(UUID.randomUUID().toString())
            .language(UUID.randomUUID().toString())
            .subjectPattern(UUID.randomUUID().toString())
            .variables(UUID.randomUUID().toString());
    }
}
