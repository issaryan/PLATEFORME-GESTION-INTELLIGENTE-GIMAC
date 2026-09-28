package cm.gimac.des.collaboration.domain;

import java.util.UUID;

public class MessageReceiptTestSamples {

    public static MessageReceipt getMessageReceiptSample1() {
        return new MessageReceipt()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .userId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"));
    }

    public static MessageReceipt getMessageReceiptSample2() {
        return new MessageReceipt()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .userId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"));
    }

    public static MessageReceipt getMessageReceiptRandomSampleGenerator() {
        return new MessageReceipt().id(UUID.randomUUID()).userId(UUID.randomUUID());
    }
}
