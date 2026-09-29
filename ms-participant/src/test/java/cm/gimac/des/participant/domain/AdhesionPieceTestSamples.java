package cm.gimac.des.participant.domain;

import java.util.UUID;

public class AdhesionPieceTestSamples {

    public static AdhesionPiece getAdhesionPieceSample1() {
        return new AdhesionPiece()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .code("code1")
            .label("label1")
            .documentId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .documentSha256("documentSha2561")
            .fileName("fileName1")
            .depositedById(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .reviewedById(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .rejectionReason("rejectionReason1");
    }

    public static AdhesionPiece getAdhesionPieceSample2() {
        return new AdhesionPiece()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .code("code2")
            .label("label2")
            .documentId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .documentSha256("documentSha2562")
            .fileName("fileName2")
            .depositedById(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .reviewedById(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .rejectionReason("rejectionReason2");
    }

    public static AdhesionPiece getAdhesionPieceRandomSampleGenerator() {
        return new AdhesionPiece()
            .id(UUID.randomUUID())
            .code(UUID.randomUUID().toString())
            .label(UUID.randomUUID().toString())
            .documentId(UUID.randomUUID())
            .documentSha256(UUID.randomUUID().toString())
            .fileName(UUID.randomUUID().toString())
            .depositedById(UUID.randomUUID())
            .reviewedById(UUID.randomUUID())
            .rejectionReason(UUID.randomUUID().toString());
    }
}
