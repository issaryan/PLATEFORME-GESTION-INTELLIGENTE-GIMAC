package cm.gimac.des.document.domain.enumeration;

/**
 * Cycle de vie d'une version de pièce (algorithmes 6.17 et 6.18).
 */
public enum DocumentState {
    RECU,
    EN_QUARANTAINE,
    REJETE,
    SCELLE,
    REMPLACE,
    PURGE,
}
