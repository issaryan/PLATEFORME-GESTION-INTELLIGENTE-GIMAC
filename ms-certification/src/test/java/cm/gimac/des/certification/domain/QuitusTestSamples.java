package cm.gimac.des.certification.domain;

import java.util.UUID;

public class QuitusTestSamples {

    public static Quitus getQuitusSample1() {
        return new Quitus().id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa")).motifCaducite("motifCaducite1");
    }

    public static Quitus getQuitusSample2() {
        return new Quitus().id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367")).motifCaducite("motifCaducite2");
    }

    public static Quitus getQuitusRandomSampleGenerator() {
        return new Quitus().id(UUID.randomUUID()).motifCaducite(UUID.randomUUID().toString());
    }
}
