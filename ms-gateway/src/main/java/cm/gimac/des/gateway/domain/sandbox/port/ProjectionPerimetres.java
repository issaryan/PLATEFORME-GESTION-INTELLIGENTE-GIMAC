package cm.gimac.des.gateway.domain.sandbox.port;

import cm.gimac.des.gateway.domain.sandbox.PerimetreMandats;
import reactor.core.publisher.Mono;

/** Projection locale des périmètres de visibilité de SBX-ESPACE, par compte. */
public interface ProjectionPerimetres {
    /** Périmètre du compte, vide si aucun instantané n'a été reçu. */
    Mono<PerimetreMandats> lire(String compte);

    /** Remplace la projection si la version reçue est supérieure ; vrai si elle a été appliquée. */
    Mono<Boolean> appliquer(PerimetreMandats perimetre);
}
