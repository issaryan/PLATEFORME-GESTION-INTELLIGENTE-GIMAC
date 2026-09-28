package cm.gimac.des.incident.domain;

import java.util.UUID;

public class QualificationMatrixTestSamples {

    public static QualificationMatrix getQualificationMatrixSample1() {
        return new QualificationMatrix().id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa")).matrixVersion("matrixVersion1");
    }

    public static QualificationMatrix getQualificationMatrixSample2() {
        return new QualificationMatrix().id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367")).matrixVersion("matrixVersion2");
    }

    public static QualificationMatrix getQualificationMatrixRandomSampleGenerator() {
        return new QualificationMatrix().id(UUID.randomUUID()).matrixVersion(UUID.randomUUID().toString());
    }
}
