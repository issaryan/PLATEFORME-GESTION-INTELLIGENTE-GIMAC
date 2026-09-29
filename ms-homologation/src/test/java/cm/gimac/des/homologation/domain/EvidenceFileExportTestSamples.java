package cm.gimac.des.homologation.domain;

import java.util.UUID;

public class EvidenceFileExportTestSamples {

    public static EvidenceFileExport getEvidenceFileExportSample1() {
        return new EvidenceFileExport()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .empreinteManifeste("empreinteManifeste1")
            .documentRef("documentRef1")
            .demandeurId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .socleEmpreinte("socleEmpreinte1")
            .etat("etat1");
    }

    public static EvidenceFileExport getEvidenceFileExportSample2() {
        return new EvidenceFileExport()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .empreinteManifeste("empreinteManifeste2")
            .documentRef("documentRef2")
            .demandeurId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .socleEmpreinte("socleEmpreinte2")
            .etat("etat2");
    }

    public static EvidenceFileExport getEvidenceFileExportRandomSampleGenerator() {
        return new EvidenceFileExport()
            .id(UUID.randomUUID())
            .empreinteManifeste(UUID.randomUUID().toString())
            .documentRef(UUID.randomUUID().toString())
            .demandeurId(UUID.randomUUID())
            .socleEmpreinte(UUID.randomUUID().toString())
            .etat(UUID.randomUUID().toString());
    }
}
