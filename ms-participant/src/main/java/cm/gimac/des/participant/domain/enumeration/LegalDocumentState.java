package cm.gimac.des.participant.domain.enumeration;

/**
 * État d'une pièce légale (PAR-05 : valide, expirée, remplacée).
 */
public enum LegalDocumentState {
    EN_ATTENTE_SCELLEMENT,
    VALIDE,
    EXPIRANT,
    EXPIREE,
    REMPLACEE,
}
