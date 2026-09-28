package cm.gimac.des.iam.domain;

import java.util.UUID;

public class AccessReviewTestSamples {

    public static AccessReview getAccessReviewSample1() {
        return new AccessReview()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .period("period1")
            .approverId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .digest("digest1");
    }

    public static AccessReview getAccessReviewSample2() {
        return new AccessReview()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .period("period2")
            .approverId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .digest("digest2");
    }

    public static AccessReview getAccessReviewRandomSampleGenerator() {
        return new AccessReview()
            .id(UUID.randomUUID())
            .period(UUID.randomUUID().toString())
            .approverId(UUID.randomUUID())
            .digest(UUID.randomUUID().toString());
    }
}
