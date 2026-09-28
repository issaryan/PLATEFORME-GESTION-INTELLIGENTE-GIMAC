package cm.gimac.des.incident.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class TicketTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));
    private static final AtomicInteger intCount = new AtomicInteger(random.nextInt() + (2 * Short.MAX_VALUE));

    public static Ticket getTicketSample1() {
        return new Ticket()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .ticketNumber("ticketNumber1")
            .subject("subject1")
            .originRef("originRef1")
            .requesterId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .institutionId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .linkId("linkId1")
            .alertId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .priority(1)
            .assigneeId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .resolverId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .closedBy(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .version(1L);
    }

    public static Ticket getTicketSample2() {
        return new Ticket()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .ticketNumber("ticketNumber2")
            .subject("subject2")
            .originRef("originRef2")
            .requesterId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .institutionId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .linkId("linkId2")
            .alertId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .priority(2)
            .assigneeId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .resolverId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .closedBy(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .version(2L);
    }

    public static Ticket getTicketRandomSampleGenerator() {
        return new Ticket()
            .id(UUID.randomUUID())
            .ticketNumber(UUID.randomUUID().toString())
            .subject(UUID.randomUUID().toString())
            .originRef(UUID.randomUUID().toString())
            .requesterId(UUID.randomUUID())
            .institutionId(UUID.randomUUID())
            .linkId(UUID.randomUUID().toString())
            .alertId(UUID.randomUUID())
            .priority(intCount.incrementAndGet())
            .assigneeId(UUID.randomUUID())
            .resolverId(UUID.randomUUID())
            .closedBy(UUID.randomUUID())
            .version(longCount.incrementAndGet());
    }
}
