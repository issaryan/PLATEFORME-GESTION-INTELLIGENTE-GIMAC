package cm.gimac.des.iam.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class CollaboratorTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + 2L * Integer.MAX_VALUE);
    private static final AtomicInteger intCount = new AtomicInteger(random.nextInt() + 2 * Short.MAX_VALUE);

    public static Collaborator getCollaboratorSample1() {
        return new Collaborator()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .federatedId("federatedId1")
            .directoryId("directoryId1")
            .displayName("displayName1")
            .email("email1")
            .institutionId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .language("language1")
            .failedAttempts(1)
            .directoryCursor(1L);
    }

    public static Collaborator getCollaboratorSample2() {
        return new Collaborator()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .federatedId("federatedId2")
            .directoryId("directoryId2")
            .displayName("displayName2")
            .email("email2")
            .institutionId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .language("language2")
            .failedAttempts(2)
            .directoryCursor(2L);
    }

    public static Collaborator getCollaboratorRandomSampleGenerator() {
        return new Collaborator()
            .id(UUID.randomUUID())
            .federatedId(UUID.randomUUID().toString())
            .directoryId(UUID.randomUUID().toString())
            .displayName(UUID.randomUUID().toString())
            .email(UUID.randomUUID().toString())
            .institutionId(UUID.randomUUID())
            .language(UUID.randomUUID().toString())
            .failedAttempts(intCount.incrementAndGet())
            .directoryCursor(longCount.incrementAndGet());
    }
}
