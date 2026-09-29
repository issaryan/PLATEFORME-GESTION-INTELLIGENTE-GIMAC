package cm.gimac.des.certification.domain;

import java.util.UUID;

public class CertificationRegisterEntryTestSamples {

    public static CertificationRegisterEntry getCertificationRegisterEntrySample1() {
        return new CertificationRegisterEntry()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .numeroAttestation("numeroAttestation1")
            .institutionId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .typeProjet("typeProjet1")
            .precedenteId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .dernierSeuilNotifie("dernierSeuilNotifie1");
    }

    public static CertificationRegisterEntry getCertificationRegisterEntrySample2() {
        return new CertificationRegisterEntry()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .numeroAttestation("numeroAttestation2")
            .institutionId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .typeProjet("typeProjet2")
            .precedenteId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .dernierSeuilNotifie("dernierSeuilNotifie2");
    }

    public static CertificationRegisterEntry getCertificationRegisterEntryRandomSampleGenerator() {
        return new CertificationRegisterEntry()
            .id(UUID.randomUUID())
            .numeroAttestation(UUID.randomUUID().toString())
            .institutionId(UUID.randomUUID())
            .typeProjet(UUID.randomUUID().toString())
            .precedenteId(UUID.randomUUID())
            .dernierSeuilNotifie(UUID.randomUUID().toString());
    }
}
