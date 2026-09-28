package cm.gimac.des.participant.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class StatusTransitionTestSamples {

    private static final Random random = new Random();
    private static final AtomicInteger intCount = new AtomicInteger(random.nextInt() + (2 * Short.MAX_VALUE));

    public static StatusTransition getStatusTransitionSample1() {
        return new StatusTransition()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .reason("reason1")
            .requesterId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .requiredApprovals(1)
            .approverId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .secondApproverId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .rejectionReason("rejectionReason1")
            .evidenceDocumentId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"));
    }

    public static StatusTransition getStatusTransitionSample2() {
        return new StatusTransition()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .reason("reason2")
            .requesterId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .requiredApprovals(2)
            .approverId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .secondApproverId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .rejectionReason("rejectionReason2")
            .evidenceDocumentId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"));
    }

    public static StatusTransition getStatusTransitionRandomSampleGenerator() {
        return new StatusTransition()
            .id(UUID.randomUUID())
            .reason(UUID.randomUUID().toString())
            .requesterId(UUID.randomUUID())
            .requiredApprovals(intCount.incrementAndGet())
            .approverId(UUID.randomUUID())
            .secondApproverId(UUID.randomUUID())
            .rejectionReason(UUID.randomUUID().toString())
            .evidenceDocumentId(UUID.randomUUID());
    }
}
