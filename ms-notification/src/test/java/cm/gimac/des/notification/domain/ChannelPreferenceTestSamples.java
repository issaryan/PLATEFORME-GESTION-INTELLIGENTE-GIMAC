package cm.gimac.des.notification.domain;

import java.util.UUID;

public class ChannelPreferenceTestSamples {

    public static ChannelPreference getChannelPreferenceSample1() {
        return new ChannelPreference()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .userId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"));
    }

    public static ChannelPreference getChannelPreferenceSample2() {
        return new ChannelPreference()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .userId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"));
    }

    public static ChannelPreference getChannelPreferenceRandomSampleGenerator() {
        return new ChannelPreference().id(UUID.randomUUID()).userId(UUID.randomUUID());
    }
}
