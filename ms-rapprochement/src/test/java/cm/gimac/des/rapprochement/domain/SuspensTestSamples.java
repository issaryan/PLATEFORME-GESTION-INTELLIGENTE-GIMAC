package cm.gimac.des.rapprochement.domain;

import java.util.UUID;

public class SuspensTestSamples {

    public static Suspens getSuspensSample1() {
        return new Suspens()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .reference("reference1")
            .institutionId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .pivotTransactionRef("pivotTransactionRef1")
            .pivotAuthorizationRef("pivotAuthorizationRef1")
            .pivotParticipantCode("pivotParticipantCode1")
            .assigneeId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .disputeTicketId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"));
    }

    public static Suspens getSuspensSample2() {
        return new Suspens()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .reference("reference2")
            .institutionId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .pivotTransactionRef("pivotTransactionRef2")
            .pivotAuthorizationRef("pivotAuthorizationRef2")
            .pivotParticipantCode("pivotParticipantCode2")
            .assigneeId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .disputeTicketId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"));
    }

    public static Suspens getSuspensRandomSampleGenerator() {
        return new Suspens()
            .id(UUID.randomUUID())
            .reference(UUID.randomUUID().toString())
            .institutionId(UUID.randomUUID())
            .pivotTransactionRef(UUID.randomUUID().toString())
            .pivotAuthorizationRef(UUID.randomUUID().toString())
            .pivotParticipantCode(UUID.randomUUID().toString())
            .assigneeId(UUID.randomUUID())
            .disputeTicketId(UUID.randomUUID());
    }
}
