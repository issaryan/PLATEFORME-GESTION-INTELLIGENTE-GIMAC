package cm.gimac.des.document.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class DocumentTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));
    private static final AtomicInteger intCount = new AtomicInteger(random.nextInt() + (2 * Short.MAX_VALUE));

    public static Document getDocumentSample1() {
        return new Document()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .title("title1")
            .version(1)
            .authorId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .callerService("callerService1")
            .institutionId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .ownerUnit("ownerUnit1")
            .restrictedReaderIds("restrictedReaderIds1")
            .sha256("sha2561")
            .sizeBytes(1L)
            .mediaType("mediaType1")
            .objectKey("objectKey1")
            .retentionPeriod("retentionPeriod1")
            .scanSignatureName("scanSignatureName1")
            .scanEngineVersion("scanEngineVersion1")
            .maskingCandidates(1)
            .maskingLuhnValid(1)
            .maskingMasked(1)
            .rejectionReason("rejectionReason1");
    }

    public static Document getDocumentSample2() {
        return new Document()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .title("title2")
            .version(2)
            .authorId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .callerService("callerService2")
            .institutionId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .ownerUnit("ownerUnit2")
            .restrictedReaderIds("restrictedReaderIds2")
            .sha256("sha2562")
            .sizeBytes(2L)
            .mediaType("mediaType2")
            .objectKey("objectKey2")
            .retentionPeriod("retentionPeriod2")
            .scanSignatureName("scanSignatureName2")
            .scanEngineVersion("scanEngineVersion2")
            .maskingCandidates(2)
            .maskingLuhnValid(2)
            .maskingMasked(2)
            .rejectionReason("rejectionReason2");
    }

    public static Document getDocumentRandomSampleGenerator() {
        return new Document()
            .id(UUID.randomUUID())
            .title(UUID.randomUUID().toString())
            .version(intCount.incrementAndGet())
            .authorId(UUID.randomUUID())
            .callerService(UUID.randomUUID().toString())
            .institutionId(UUID.randomUUID())
            .ownerUnit(UUID.randomUUID().toString())
            .restrictedReaderIds(UUID.randomUUID().toString())
            .sha256(UUID.randomUUID().toString())
            .sizeBytes(longCount.incrementAndGet())
            .mediaType(UUID.randomUUID().toString())
            .objectKey(UUID.randomUUID().toString())
            .retentionPeriod(UUID.randomUUID().toString())
            .scanSignatureName(UUID.randomUUID().toString())
            .scanEngineVersion(UUID.randomUUID().toString())
            .maskingCandidates(intCount.incrementAndGet())
            .maskingLuhnValid(intCount.incrementAndGet())
            .maskingMasked(intCount.incrementAndGet())
            .rejectionReason(UUID.randomUUID().toString());
    }
}
