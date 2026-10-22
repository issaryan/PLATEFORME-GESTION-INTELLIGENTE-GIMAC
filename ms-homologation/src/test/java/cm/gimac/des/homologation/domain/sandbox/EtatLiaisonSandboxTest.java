package cm.gimac.des.homologation.domain.sandbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** US-EXI-16 : dernier état publié d'une liaison, version strictement croissante. */
class EtatLiaisonSandboxTest {

    static final Instant T = Instant.parse("2026-10-22T08:00:00Z");

    @Test
    @DisplayName("Version 1 à la création, incrémentée seulement quand le statut change")
    void versionCroissante() {
        UUID id = UUID.randomUUID();
        EtatLiaisonSandbox e = new EtatLiaisonSandbox(id, "99001", StatutSandbox.OUVERTE, T);
        assertThat(e.getLiaisonId()).isEqualTo(id);
        assertThat(e.getVersionEtat()).isEqualTo(1L);
        assertThat(e.getPublieLe()).isEqualTo(T);
        assertThat(e.constater(StatutSandbox.OUVERTE, T.plusSeconds(1))).isFalse();
        assertThat(e.getPublieLe()).isEqualTo(T);
        assertThat(e.constater(StatutSandbox.GELEE, T.plusSeconds(2))).isTrue();
        assertThat(e.getVersionEtat()).isEqualTo(2L);
        assertThat(e.getStatut()).isEqualTo(StatutSandbox.GELEE);
        assertThat(e.getPublieLe()).isEqualTo(T.plusSeconds(2));
    }

    @Test
    @DisplayName("Liaison et statut obligatoires ; constructeur réservé à la persistance")
    void invariants() {
        assertThatThrownBy(() -> new EtatLiaisonSandbox(null, "99001", StatutSandbox.OUVERTE, T)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new EtatLiaisonSandbox(UUID.randomUUID(), "99001", null, T)).isInstanceOf(NullPointerException.class);
        assertThat(new EtatLiaisonSandbox().getStatut()).isNull();
    }
}
