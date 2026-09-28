package cm.gimac.des.collaboration.domain.enumeration;

/**
 * État de l'archive. Écart CC corrigé : l'algorithme 6.63 (étape 5) laisse
 * l'archive « en attente » si l'autorité d'horodatage est indisponible.
 */
public enum ArchiveState {
    EN_CONSTITUTION,
    EN_ATTENTE_HORODATAGE,
    SCELLEE,
}
