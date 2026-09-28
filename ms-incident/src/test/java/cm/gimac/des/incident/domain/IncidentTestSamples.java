package cm.gimac.des.incident.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class IncidentTestSamples {

    private static final Random random = new Random();
    private static final AtomicInteger intCount = new AtomicInteger(random.nextInt() + (2 * Short.MAX_VALUE));

    public static Incident getIncidentSample1() {
        return new Incident()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .impact(1)
            .urgency(1)
            .score(1)
            .qualifiedBy(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .rootCause("rootCause1")
            .rootCauseCategory("rootCauseCategory1")
            .rootCauseProofRef("rootCauseProofRef1")
            .resolutionTest("resolutionTest1")
            .resolutionTestAuthorId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .resolutionTestProofRef("resolutionTestProofRef1")
            .lessonsLearnedRef("lessonsLearnedRef1")
            .lessonsLearnedWaiver("lessonsLearnedWaiver1")
            .reportRef("reportRef1")
            .reportDigest("reportDigest1")
            .complianceVisaBy(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .validatorId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"));
    }

    public static Incident getIncidentSample2() {
        return new Incident()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .impact(2)
            .urgency(2)
            .score(2)
            .qualifiedBy(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .rootCause("rootCause2")
            .rootCauseCategory("rootCauseCategory2")
            .rootCauseProofRef("rootCauseProofRef2")
            .resolutionTest("resolutionTest2")
            .resolutionTestAuthorId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .resolutionTestProofRef("resolutionTestProofRef2")
            .lessonsLearnedRef("lessonsLearnedRef2")
            .lessonsLearnedWaiver("lessonsLearnedWaiver2")
            .reportRef("reportRef2")
            .reportDigest("reportDigest2")
            .complianceVisaBy(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .validatorId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"));
    }

    public static Incident getIncidentRandomSampleGenerator() {
        return new Incident()
            .id(UUID.randomUUID())
            .impact(intCount.incrementAndGet())
            .urgency(intCount.incrementAndGet())
            .score(intCount.incrementAndGet())
            .qualifiedBy(UUID.randomUUID())
            .rootCause(UUID.randomUUID().toString())
            .rootCauseCategory(UUID.randomUUID().toString())
            .rootCauseProofRef(UUID.randomUUID().toString())
            .resolutionTest(UUID.randomUUID().toString())
            .resolutionTestAuthorId(UUID.randomUUID())
            .resolutionTestProofRef(UUID.randomUUID().toString())
            .lessonsLearnedRef(UUID.randomUUID().toString())
            .lessonsLearnedWaiver(UUID.randomUUID().toString())
            .reportRef(UUID.randomUUID().toString())
            .reportDigest(UUID.randomUUID().toString())
            .complianceVisaBy(UUID.randomUUID())
            .validatorId(UUID.randomUUID());
    }
}
