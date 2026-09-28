package cm.gimac.des.document.domain.enumeration;

/**
 * Types de pièces du tableau 6.18 du CC, complétés par le justificatif
 * d'absence, type restreint (REC D-29, RG-74).
 */
public enum DocumentType {
    RAPPORT_DE_QUART,
    ATTESTATION,
    ARCHIVE_DE_CRISE,
    PIECE_LEGALE,
    PREUVE_HOMOLOGATION,
    PROCES_VERBAL,
    RAPPORT_PERIODIQUE,
    ETAT_IMPORTE,
    JUSTIFICATIF_ABSENCE,
    /**
     * Rapport circonstancié d&#39;un incident S1 ou S2 (MS-INCIDENT, D-10).
     */
    RAPPORT_CIRCONSTANCIE,
    /**
     * Rapport post-incident signé d&#39;une cellule de crise (MS-INCIDENT, D-14, RG-42).
     */
    RAPPORT_POST_INCIDENT,
    /**
     * Attestation de régularisation d&#39;un suspens (MS-RAPPROCHEMENT, RG-45).
     */
    ATTESTATION_REGULARISATION,
    /**
     * Convention d&#39;adhésion ou de parrainage (MS-PARTICIPANT, RG-12).
     */
    CONVENTION,
    /**
     * Export contrôlé d&#39;agrégats (MS-REPORTING, REP-08).
     */
    EXPORT_CONTROLE,
    /**
     * Segment d&#39;enregistrement de conférence, avec consentement (MS-COLLABORATION, RG-55).
     */
    ENREGISTREMENT_CONFERENCE,
}
