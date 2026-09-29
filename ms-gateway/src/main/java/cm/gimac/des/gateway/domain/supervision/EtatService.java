package cm.gimac.des.gateway.domain.supervision;

import java.time.Instant;
import java.util.Map;

/** Instantané d'un service : santé, latence de la sonde, construction et métriques. */
public record EtatService(
    String nom,
    String url,
    EtatSante etat,
    Long latenceMs,
    Map<String, String> composants,
    InfoService info,
    Metriques metriques,
    String erreur,
    Instant controleLe
) {
    public EtatService {
        composants = composants == null ? Map.of() : Map.copyOf(composants);
    }
}
