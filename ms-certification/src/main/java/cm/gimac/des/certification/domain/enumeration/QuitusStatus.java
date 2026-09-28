package cm.gimac.des.certification.domain.enumeration;

/**
 * État d'un quitus. Un quitus encore attendu quand le dossier passe
 * EN_CORRECTION est suspendu par l'état du dossier, sans état propre (CC).
 */
public enum QuitusStatus {
    ATTENDU,
    SIGNE,
    CADUC,
}
