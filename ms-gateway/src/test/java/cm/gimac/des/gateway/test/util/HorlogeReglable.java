package cm.gimac.des.gateway.test.util;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;

/** Horloge des tests : avance à la demande (échéances des sessions, recharge de la limitation). */
public final class HorlogeReglable extends Clock {

    private final AtomicReference<Instant> maintenant;

    public HorlogeReglable(Instant depart) {
        this.maintenant = new AtomicReference<>(depart);
    }

    public void avancer(Duration duree) {
        maintenant.updateAndGet(i -> i.plus(duree));
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return this;
    }

    @Override
    public Instant instant() {
        return maintenant.get();
    }
}
