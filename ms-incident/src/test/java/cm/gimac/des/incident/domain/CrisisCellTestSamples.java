package cm.gimac.des.incident.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class CrisisCellTestSamples {

    private static final Random random = new Random();
    private static final AtomicInteger intCount = new AtomicInteger(random.nextInt() + (2 * Short.MAX_VALUE));

    public static CrisisCell getCrisisCellSample1() {
        return new CrisisCell()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .reference("reference1")
            .activationReason("activationReason1")
            .convenedBy(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .quorumThreshold(1)
            .roomRef("roomRef1")
            .postIncidentReportRef("postIncidentReportRef1")
            .postIncidentReportDigest("postIncidentReportDigest1")
            .postIncidentReportSignedBy(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .closedBy(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .archiveRef("archiveRef1");
    }

    public static CrisisCell getCrisisCellSample2() {
        return new CrisisCell()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .reference("reference2")
            .activationReason("activationReason2")
            .convenedBy(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .quorumThreshold(2)
            .roomRef("roomRef2")
            .postIncidentReportRef("postIncidentReportRef2")
            .postIncidentReportDigest("postIncidentReportDigest2")
            .postIncidentReportSignedBy(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .closedBy(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .archiveRef("archiveRef2");
    }

    public static CrisisCell getCrisisCellRandomSampleGenerator() {
        return new CrisisCell()
            .id(UUID.randomUUID())
            .reference(UUID.randomUUID().toString())
            .activationReason(UUID.randomUUID().toString())
            .convenedBy(UUID.randomUUID())
            .quorumThreshold(intCount.incrementAndGet())
            .roomRef(UUID.randomUUID().toString())
            .postIncidentReportRef(UUID.randomUUID().toString())
            .postIncidentReportDigest(UUID.randomUUID().toString())
            .postIncidentReportSignedBy(UUID.randomUUID())
            .closedBy(UUID.randomUUID())
            .archiveRef(UUID.randomUUID().toString());
    }
}
