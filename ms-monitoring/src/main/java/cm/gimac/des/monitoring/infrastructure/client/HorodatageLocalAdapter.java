package cm.gimac.des.monitoring.infrastructure.client;

import cm.gimac.des.monitoring.application.port.out.HorodatagePort;
import cm.gimac.des.monitoring.domain.exploitation.Empreintes;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import org.springframework.stereotype.Component;

/**
 * Horodatage local en attendant le raccordement de l'autorité d'horodatage RFC 3161 (reste à faire, D-114) : le
 * jeton est l'empreinte de l'empreinte et de l'instant, sa série est préfixée « LOCAL- » pour qu'il ne passe jamais
 * pour un jeton qualifié.
 */
@Component
public class HorodatageLocalAdapter implements HorodatagePort {

    private final Clock clock;

    public HorodatageLocalAdapter(Clock clock) {
        this.clock = clock;
    }

    @Override
    public Jeton horodater(String empreinteHex) {
        String instant = clock.instant().toString();
        String jeton = Empreintes.sha256(empreinteHex, instant);
        return new Jeton("LOCAL-" + instant + "-" + jeton.substring(0, 8), (instant + " " + jeton).getBytes(StandardCharsets.UTF_8));
    }
}
