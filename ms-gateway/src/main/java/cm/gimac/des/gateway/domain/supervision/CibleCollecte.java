package cm.gimac.des.gateway.domain.supervision;

import java.time.Instant;

/** Cible de collecte de Prometheus (/api/v1/targets) : état up, down ou unknown. */
public record CibleCollecte(
    String job,
    String instance,
    String etat,
    Instant derniereCollecte,
    Double dureeDerniereCollecteSecondes,
    String erreur
) {}
