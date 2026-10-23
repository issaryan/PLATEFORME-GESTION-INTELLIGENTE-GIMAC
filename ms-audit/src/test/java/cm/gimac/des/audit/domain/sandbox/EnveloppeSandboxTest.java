package cm.gimac.des.audit.domain.sandbox;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** US-EXI-10 : validation des enveloppes des onze services de la sandbox (contrat CloudEvents commun). */
class EnveloppeSandboxTest {

    static EnveloppeSandbox.Champs champs(String spec, String id, String type, String source, String time, boolean data, String ext, String inst, String acteur) {
        return new EnveloppeSandbox.Champs(spec, id, type, source, time, data, ext, inst, acteur, "c-1");
    }

    static EnveloppeSandbox.Champs nominal() {
        return champs("1.0", "e-1", "sbx-campagne.verdict-validated.v1", "/sbx-campagne", "2026-10-22T08:00:00Z", true, "99001", null, "u-certificateur");
    }

    @Test
    @DisplayName("Critère 1 : verdict validé, auteur, horodatage, objet et institution retenus")
    void nominal_() {
        EnveloppeSandbox.Lecture l = EnveloppeSandbox.lire(nominal());
        assertThat(l.motif()).isNull();
        assertThat(l.ignore()).isFalse();
        EnveloppeSandbox e = l.enveloppe();
        assertThat(e.service()).isEqualTo("sbx-campagne");
        assertThat(e.institution()).isEqualTo("99001");
        assertThat(e.acteur()).isEqualTo("u-certificateur");
        assertThat(e.survenueLe()).isEqualTo(Instant.parse("2026-10-22T08:00:00Z"));
        assertThat(e.correlationId()).isEqualTo("c-1");
        assertThat(e.type()).isEqualTo("sbx-campagne.verdict-validated.v1");
    }

    @Test
    @DisplayName("Institution des données à défaut d'extension ; auteur par défaut : le service ; acteur tronqué ; sans institution")
    void defauts() {
        EnveloppeSandbox e = EnveloppeSandbox.lire(champs("1.0", " e-2 ", "sbx-lecture.access-recorded.v1", "sbx-lecture/v1", "2026-10-22T08:00:00Z", true, " ", " 99002 ", null)).enveloppe();
        assertThat(e.id()).isEqualTo("e-2");
        assertThat(e.institution()).isEqualTo("99002");
        assertThat(e.acteur()).isEqualTo("sbx-lecture");
        assertThat(EnveloppeSandbox.lire(champs("1.0", "e", "sbx-trace.trace-purged.v1", "/sbx-trace", "2026-10-22T08:00:00Z", true, null, null, "a".repeat(150))).enveloppe().acteur()).hasSize(100);
        assertThat(EnveloppeSandbox.lire(champs("1.0", "e", "sbx-trace.trace-purged.v1", "/sbx-trace", "2026-10-22T08:00:00Z", true, null, null, null)).enveloppe().institution()).isNull();
    }

    @Test
    @DisplayName("Critère 3 : enveloppe non conforme, motif dans l'ordre des contrôles ; demande de notification ignorée")
    void nonConformes() {
        assertThat(EnveloppeSandbox.lire(champs("0.3", "e", "sbx-trace.trace-purged.v1", "/sbx-trace", "2026-10-22T08:00:00Z", true, null, null, null)).motif()).isEqualTo("SPECVERSION_INVALIDE");
        assertThat(EnveloppeSandbox.lire(champs("1.0", " ", "sbx-trace.trace-purged.v1", "/sbx-trace", "2026-10-22T08:00:00Z", true, null, null, null)).motif()).isEqualTo("ID_ABSENT");
        assertThat(EnveloppeSandbox.lire(champs("1.0", null, "sbx-trace.trace-purged.v1", "/sbx-trace", "2026-10-22T08:00:00Z", true, null, null, null)).motif()).isEqualTo("ID_ABSENT");
        assertThat(EnveloppeSandbox.lire(champs("1.0", "x".repeat(201), "sbx-trace.trace-purged.v1", "/sbx-trace", "2026-10-22T08:00:00Z", true, null, null, null)).motif()).isEqualTo("ID_ABSENT");
        for (String type : new String[] { null, "sbx-inconnu.trace-purged.v1", "pilotage.process-step-signed.v1", "sbx-trace.trace.v1", "sbx-trace.trace-purged.v0", "sbx-trace.trace-purged.v1" + "-x".repeat(60) }) {
            assertThat(EnveloppeSandbox.lire(champs("1.0", "e", type, "/sbx-trace", "2026-10-22T08:00:00Z", true, null, null, null)).motif()).as(type).isEqualTo("TYPE_INVALIDE");
        }
        assertThat(EnveloppeSandbox.lire(champs("1.0", "e", "sbx-trace.notification-requested.v1", null, null, false, null, null, null)).ignore()).isTrue();
        assertThat(EnveloppeSandbox.lire(champs("1.0", "e", "sbx-espace.space-opened.v1", "/gimac/sandbox/sbx-espace", "2026-10-22T08:00:00Z", true, null, null, null)).motif()).isNull();
        for (String source : new String[] { null, " ", "/sbx-campagne", "https://ailleurs/sbx-trace", "/gimac/sandbox/sbx-traces" }) {
            assertThat(EnveloppeSandbox.lire(champs("1.0", "e", "sbx-trace.trace-purged.v1", source, "2026-10-22T08:00:00Z", true, null, null, null)).motif()).as(source).isEqualTo("SOURCE_INVALIDE");
        }
        for (String time : new String[] { null, " ", "22/10/2026" }) {
            assertThat(EnveloppeSandbox.lire(champs("1.0", "e", "sbx-trace.trace-purged.v1", "/sbx-trace", time, true, null, null, null)).motif()).isEqualTo("TIME_INVALIDE");
        }
        assertThat(EnveloppeSandbox.lire(champs("1.0", "e", "sbx-trace.trace-purged.v1", "/sbx-trace", "2026-10-22T08:00:00Z", false, null, null, null)).motif()).isEqualTo("DATA_ABSENTES");
        assertThat(EnveloppeSandbox.lire(champs("1.0", "e", "sbx-trace.trace-purged.v1", "/sbx-trace", "2026-10-22T08:00:00Z", true, "9".repeat(65), null, null)).motif()).isEqualTo("INSTITUTION_INVALIDE");
        assertThat(EnveloppeSandbox.SERVICES).hasSize(11);
    }
}
