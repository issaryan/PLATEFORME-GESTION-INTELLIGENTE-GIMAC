package cm.gimac.des.pilotage.domain;

import java.util.UUID;

public class MilestoneApprovalTestSamples {

    public static MilestoneApproval getMilestoneApprovalSample1() {
        return new MilestoneApproval()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .approverId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .comment("comment1");
    }

    public static MilestoneApproval getMilestoneApprovalSample2() {
        return new MilestoneApproval()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .approverId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .comment("comment2");
    }

    public static MilestoneApproval getMilestoneApprovalRandomSampleGenerator() {
        return new MilestoneApproval().id(UUID.randomUUID()).approverId(UUID.randomUUID()).comment(UUID.randomUUID().toString());
    }
}
