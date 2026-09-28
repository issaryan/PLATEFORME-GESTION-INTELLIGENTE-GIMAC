package cm.gimac.des.reporting.domain.enumeration;

/**
 * Domaine d'une table de faits du schéma en étoile. Écart CC corrigé : le CC
 * type le domaine en String ; les six domaines consommés (D-19) et les états
 * déclarés sont énumérés.
 */
public enum FactDomain {
    INCIDENT,
    MONITORING,
    HOMOLOGATION,
    CERTIFICATION,
    RAPPROCHEMENT,
    PILOTAGE,
    ETAT_DECLARE,
}
