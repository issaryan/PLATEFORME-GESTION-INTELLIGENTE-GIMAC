package cm.gimac.des.gateway.domain.supervision;

/** Événement du flux temps réel : instantané des services et nombre d'alertes actives (-1 si Alertmanager est injoignable). */
public record ImpulsionPlateforme(long sequence, InstantanePlateforme instantane, int nombreAlertes) {}
