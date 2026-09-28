package cm.gimac.des.pilotage.domain.enumeration;

/**
 * État d'une décision de réunion. Écart CC corrigé : le CC ne type pas ce
 * cycle ; l'algorithme 6.55 (étapes 2 et 3) distingue la décision saisie, la
 * décision renvoyée au secrétaire de séance et la décision convertie en tâche.
 */
public enum DecisionState {
    SAISIE,
    RENVOYEE,
    CONVERTIE,
}
