package cm.gimac.des.participant.domain.enumeration;

/**
 * Catégories d'établissement (PAR-01, CA §3.4.2).
 */
public enum InstitutionCategory {
    BANQUE,
    MICROFINANCE,
    ETABLISSEMENT_PAIEMENT,
    EMETTEUR_MONNAIE_ELECTRONIQUE,
    TRESOR_PUBLIC,
    AGREGATEUR,
    FINTECH,
    EDITEUR_LOGICIEL,
    /** Facturier : émetteur de factures payées par les réseaux du GIMAC (D-107). */
    FACTURIER,
    /** Partenaire de remittance (transfert international), souvent raccordé par un agrégateur (D-107). */
    PARTENAIRE_REMITTANCE,
}
