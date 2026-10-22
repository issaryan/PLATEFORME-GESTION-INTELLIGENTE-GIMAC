package cm.gimac.des.gateway.infrastructure.sandbox;

import cm.gimac.des.gateway.domain.sandbox.PerimetreMandats;
import cm.gimac.des.gateway.domain.sandbox.port.ProjectionPerimetres;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import reactor.core.publisher.Mono;

/** Projection des périmètres en mémoire : contexte sans base (tests web), même règle de version que la base. */
public class ProjectionPerimetresMemoire implements ProjectionPerimetres {

    private final Map<String, PerimetreMandats> perimetres = new ConcurrentHashMap<>();

    @Override
    public Mono<PerimetreMandats> lire(String compte) {
        return Mono.justOrEmpty(perimetres.get(compte));
    }

    @Override
    public Mono<Boolean> appliquer(PerimetreMandats perimetre) {
        AtomicBoolean applique = new AtomicBoolean(false);
        perimetres.compute(perimetre.compte(), (cle, actuel) -> {
            if (actuel != null && actuel.version() >= perimetre.version()) {
                return actuel;
            }
            applique.set(true);
            return perimetre;
        });
        return Mono.just(applique.get());
    }
}
