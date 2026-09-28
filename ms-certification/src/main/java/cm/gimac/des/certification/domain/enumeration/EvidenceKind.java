package cm.gimac.des.certification.domain.enumeration;

/**
 * Nature d'une pièce du dossier. Écart CC corrigé : chaîne libre dans le CC,
 * typée car l'algorithme 6.36 (C3, C6) doit reconnaître l'attestation et le
 * procès-verbal.
 */
public enum EvidenceKind {
    SOCLE_EVALUATION,
    DECISION_QUITUS,
    ATTESTATION,
    FICHE_CHANGEMENT,
    PROCES_VERBAL_MISE_EN_SERVICE,
    MANIFESTE,
    AUTRE,
}
