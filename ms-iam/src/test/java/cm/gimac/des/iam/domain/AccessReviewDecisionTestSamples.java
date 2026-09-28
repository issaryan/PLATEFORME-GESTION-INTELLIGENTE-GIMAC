package cm.gimac.des.iam.domain;

import java.util.UUID;

public class AccessReviewDecisionTestSamples {

    public static AccessReviewDecision getAccessReviewDecisionSample1() {
        return new AccessReviewDecision()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .comment("comment1")
            .decidedBy(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"));
    }

    public static AccessReviewDecision getAccessReviewDecisionSample2() {
        return new AccessReviewDecision()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .comment("comment2")
            .decidedBy(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"));
    }

    public static AccessReviewDecision getAccessReviewDecisionRandomSampleGenerator() {
        return new AccessReviewDecision().id(UUID.randomUUID()).comment(UUID.randomUUID().toString()).decidedBy(UUID.randomUUID());
    }
}
