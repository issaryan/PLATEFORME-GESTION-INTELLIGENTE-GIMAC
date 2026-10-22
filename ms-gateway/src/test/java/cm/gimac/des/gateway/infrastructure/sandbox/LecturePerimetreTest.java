package cm.gimac.des.gateway.infrastructure.sandbox;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.gateway.domain.sandbox.PerimetreMandats;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Lecture des instantanés sbx-espace.scope-changed.v1 (enveloppe CloudEvents de SBX-ESPACE). */
class LecturePerimetreTest {

    final LecturePerimetre lecture = new LecturePerimetre();

    static String enveloppe(String type, String data) {
        return "{\"specversion\":\"1.0\",\"id\":\"e1\",\"source\":\"/gimac/sandbox/sbx-espace\",\"type\":\"" + type + "\",\"data\":" + data + "}";
    }

    @Test
    @DisplayName("Instantané conforme : compte, institution propre, mandantes et version")
    void conforme() {
        String data = "{\"idCompte\":\"u-1\",\"toutesInstitutions\":false,\"institutionPropre\":\"99010\",\"mandantes\":{\"99002\":\"2026-11-10T00:00:00Z\"},\"droits\":{},\"version\":9,\"borneValidite\":\"2026-10-22T10:05:00Z\"}";
        PerimetreMandats p = lecture.lire(enveloppe(LecturePerimetre.TYPE, data)).orElseThrow();
        assertThat(p.compte()).isEqualTo("u-1");
        assertThat(p.institutionPropre()).isEqualTo("99010");
        assertThat(p.finMandat("99002")).isEqualTo(Instant.parse("2026-11-10T00:00:00Z"));
        assertThat(p.version()).isEqualTo(9);
        // Sans institution propre ni mandantes.
        PerimetreMandats gimac = lecture.lire(enveloppe(LecturePerimetre.TYPE, "{\"idCompte\":\"u-2\",\"version\":1}")).orElseThrow();
        assertThat(gimac.institutionPropre()).isEmpty();
        assertThat(gimac.mandantes()).isEmpty();
    }

    @Test
    @DisplayName("Enveloppe non conforme écartée : vide, autre type, sans données, sans compte, version absente, date ou JSON invalides")
    void nonConforme() {
        assertThat(lecture.lire(null)).isEmpty();
        assertThat(lecture.lire(" ")).isEmpty();
        assertThat(lecture.lire("{pas du json")).isEmpty();
        assertThat(lecture.lire(enveloppe("sbx-espace.space-opened.v1", "{\"idCompte\":\"u\",\"version\":1}"))).isEmpty();
        assertThat(lecture.lire(enveloppe(LecturePerimetre.TYPE, "\"texte\""))).isEmpty();
        assertThat(lecture.lire(enveloppe(LecturePerimetre.TYPE, "{\"version\":1}"))).isEmpty();
        assertThat(lecture.lire(enveloppe(LecturePerimetre.TYPE, "{\"idCompte\":\" \",\"version\":1}"))).isEmpty();
        assertThat(lecture.lire(enveloppe(LecturePerimetre.TYPE, "{\"idCompte\":\"u\",\"version\":\"1\"}"))).isEmpty();
        assertThat(lecture.lire(enveloppe(LecturePerimetre.TYPE, "{\"idCompte\":\"u\",\"version\":1,\"mandantes\":{\"99002\":\"demain\"}}"))).isEmpty();
    }
}
