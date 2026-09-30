package cm.gimac.des.gateway.domain.live;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tampon de reprise : borné en nombre et en âge ; reprise après un identifiant. */
class TamponRejeuTest {

    static final Instant T0 = Instant.parse("2026-09-30T10:00:00Z");

    /** Horloge réglable. */
    static final class Horloge extends Clock {

        final AtomicReference<Instant> maintenant = new AtomicReference<>(T0);

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return maintenant.get();
        }

        void avancer(Duration d) {
            maintenant.set(maintenant.get().plus(d));
        }
    }

    static EvenementLive e(String id) {
        return new EvenementLive(id, "t", "s", EvenementLive.HOMOLOGATION, null, null);
    }

    @Test
    @DisplayName("Reprise : événements postérieurs à l'identifiant ; sans identifiant, aucun")
    void depuis() {
        TamponRejeu t = new TamponRejeu(10, Duration.ofMinutes(5), new Horloge());
        t.ajouter(e("a"));
        t.ajouter(e("b"));
        t.ajouter(e("c"));
        assertThat(t.depuis("a")).extracting(EvenementLive::id).containsExactly("b", "c");
        assertThat(t.depuis("c")).isEmpty();
        assertThat(t.depuis(null)).isEmpty();
        assertThat(t.depuis("")).isEmpty();
    }

    @Test
    @DisplayName("Identifiant inconnu ou sorti du tampon : tout le tampon est rejoué")
    void inconnu() {
        TamponRejeu t = new TamponRejeu(2, Duration.ofMinutes(5), new Horloge());
        t.ajouter(e("a"));
        t.ajouter(e("b"));
        t.ajouter(e("c"));
        assertThat(t.taille()).isEqualTo(2);
        assertThat(t.depuis("a")).extracting(EvenementLive::id).containsExactly("b", "c");
    }

    @Test
    @DisplayName("Événements plus anciens que la durée du tampon : purgés")
    void expiration() {
        Horloge h = new Horloge();
        TamponRejeu t = new TamponRejeu(10, Duration.ofMinutes(5), h);
        t.ajouter(e("a"));
        h.avancer(Duration.ofMinutes(3));
        t.ajouter(e("b"));
        h.avancer(Duration.ofMinutes(3));
        assertThat(t.taille()).isEqualTo(1);
        assertThat(t.depuis("a")).extracting(EvenementLive::id).containsExactly("b");
        h.avancer(Duration.ofMinutes(10));
        assertThat(t.depuis("b")).isEmpty();
    }

    @Test
    @DisplayName("Capacité nulle refusée")
    void capacite() {
        assertThatThrownBy(() -> new TamponRejeu(0, Duration.ofMinutes(5), Clock.systemUTC())).isInstanceOf(IllegalArgumentException.class);
    }
}
