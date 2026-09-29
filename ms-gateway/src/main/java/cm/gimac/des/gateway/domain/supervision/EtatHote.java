package cm.gimac.des.gateway.domain.supervision;

/** Hôte vu par node-exporter : taux d'occupation en pourcentage, charge moyenne sur une minute. */
public record EtatHote(
    String instance,
    Double cpuPourcent,
    Double memoirePourcent,
    Double memoireTotaleOctets,
    Double disquePourcent,
    Double chargeMoyenne1m
) {}
