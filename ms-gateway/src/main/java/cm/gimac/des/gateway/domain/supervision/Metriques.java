package cm.gimac.des.gateway.domain.supervision;

/**
 * Métriques d'exploitation lues sur /management/prometheus. Une valeur absente vaut null.
 * Les tailles sont en octets, les durées en secondes, les taux d'occupation CPU entre 0 et 1.
 */
public record Metriques(
    Double dureeFonctionnementSecondes,
    Double cpuProcessus,
    Double cpuSysteme,
    Integer nombreCpu,
    Double memoireTasUtiliseeOctets,
    Double memoireTasMaxOctets,
    Double memoireTasPourcent,
    Double disqueLibreOctets,
    Double disqueTotalOctets,
    Double chargeMoyenne1m,
    Integer threadsActifs
) {
    public static final Metriques VIDES = new Metriques(null, null, null, null, null, null, null, null, null, null, null);
}
