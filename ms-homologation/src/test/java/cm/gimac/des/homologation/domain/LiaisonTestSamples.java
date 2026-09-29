package cm.gimac.des.homologation.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class LiaisonTestSamples {

    private static final Random random = new Random();
    private static final AtomicInteger intCount = new AtomicInteger(random.nextInt() + 2 * Short.MAX_VALUE);

    public static Liaison getLiaisonSample1() {
        return new Liaison()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .institutionId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .institutionNom("institutionNom1")
            .chefDeProjetId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .banquePartenaireId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .motifEtat("motifEtat1")
            .cycleAptitude(1);
    }

    public static Liaison getLiaisonSample2() {
        return new Liaison()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .institutionId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .institutionNom("institutionNom2")
            .chefDeProjetId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .banquePartenaireId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .motifEtat("motifEtat2")
            .cycleAptitude(2);
    }

    public static Liaison getLiaisonRandomSampleGenerator() {
        return new Liaison()
            .id(UUID.randomUUID())
            .institutionId(UUID.randomUUID())
            .institutionNom(UUID.randomUUID().toString())
            .chefDeProjetId(UUID.randomUUID())
            .banquePartenaireId(UUID.randomUUID())
            .motifEtat(UUID.randomUUID().toString())
            .cycleAptitude(intCount.incrementAndGet());
    }
}
