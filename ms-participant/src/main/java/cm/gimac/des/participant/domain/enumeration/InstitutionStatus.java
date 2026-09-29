package cm.gimac.des.participant.domain.enumeration;

/**
 * Cycle de vie de l'institution (D-08 complété par D-57) : CANDIDAT (« Prospect »
 * du CR, prise de contact), ADHERENT (dossier d'adhésion complet, adhésion prononcée
 * par la DDRM), EN_HOMOLOGATION (première liaison ouverte), ... ; CLOS_SANS_SUITE
 * depuis CANDIDAT ou ADHERENT, avec motif.
 */
public enum InstitutionStatus {
    CANDIDAT,
    ADHERENT,
    EN_HOMOLOGATION,
    ACTIF,
    A_REGULARISER,
    SUSPENDU,
    RETIRE,
    CLOS_SANS_SUITE,
}
