package cm.gimac.des.notification.infrastructure.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CacheExpirantTest {

    @Test
    @DisplayName("Valeur servie depuis le cache pendant 60 s, relue ensuite ; un échec n'est pas mis en cache")
    void expiration() {
        AtomicLong maintenant = new AtomicLong(0);
        AtomicInteger lectures = new AtomicInteger();
        CacheExpirant<String, Integer> cache = new CacheExpirant<>(Duration.ofSeconds(60), maintenant::get);
        assertThat(cache.lire("a", k -> lectures.incrementAndGet())).isEqualTo(1);
        maintenant.set(Duration.ofSeconds(59).toNanos());
        assertThat(cache.lire("a", k -> lectures.incrementAndGet())).isEqualTo(1);
        maintenant.set(Duration.ofSeconds(60).toNanos());
        assertThat(cache.lire("a", k -> lectures.incrementAndGet())).isEqualTo(2);
        assertThatThrownBy(() ->
            cache.lire("b", k -> {
                throw new IllegalStateException("injoignable");
            })
        ).isInstanceOf(IllegalStateException.class);
        assertThat(cache.lire("b", k -> 7)).isEqualTo(7);
        cache.vider();
        assertThat(cache.lire("a", k -> lectures.incrementAndGet())).isEqualTo(3);
    }
}
