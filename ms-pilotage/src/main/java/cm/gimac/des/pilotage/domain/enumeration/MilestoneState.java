package cm.gimac.des.pilotage.domain.enumeration;

/**
 * État d'un jalon. Écart CC corrigé : le CC type l'état en String ; une
 * énumération est retenue pour porter le visa tripartite de RG-46.
 */
public enum MilestoneState {
    PREVU,
    EN_ATTENTE_VISA,
    ATTEINT,
}
