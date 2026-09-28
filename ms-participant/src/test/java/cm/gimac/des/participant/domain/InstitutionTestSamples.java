package cm.gimac.des.participant.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class InstitutionTestSamples {

    private static final Random random = new Random();
    private static final AtomicInteger intCount = new AtomicInteger(random.nextInt() + (2 * Short.MAX_VALUE));

    public static Institution getInstitutionSample1() {
        return new Institution()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .name("name1")
            .acronym("acronym1")
            .participantCode("participantCode1")
            .identificationCode("identificationCode1")
            .beacBankCode("beacBankCode1")
            .country("country1")
            .settlementAccountRef("settlementAccountRef1")
            .ringFencedAccountRef("ringFencedAccountRef1")
            .openLinkCount(1)
            .openSuspenseCount(1);
    }

    public static Institution getInstitutionSample2() {
        return new Institution()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .name("name2")
            .acronym("acronym2")
            .participantCode("participantCode2")
            .identificationCode("identificationCode2")
            .beacBankCode("beacBankCode2")
            .country("country2")
            .settlementAccountRef("settlementAccountRef2")
            .ringFencedAccountRef("ringFencedAccountRef2")
            .openLinkCount(2)
            .openSuspenseCount(2);
    }

    public static Institution getInstitutionRandomSampleGenerator() {
        return new Institution()
            .id(UUID.randomUUID())
            .name(UUID.randomUUID().toString())
            .acronym(UUID.randomUUID().toString())
            .participantCode(UUID.randomUUID().toString())
            .identificationCode(UUID.randomUUID().toString())
            .beacBankCode(UUID.randomUUID().toString())
            .country(UUID.randomUUID().toString())
            .settlementAccountRef(UUID.randomUUID().toString())
            .ringFencedAccountRef(UUID.randomUUID().toString())
            .openLinkCount(intCount.incrementAndGet())
            .openSuspenseCount(intCount.incrementAndGet());
    }
}
