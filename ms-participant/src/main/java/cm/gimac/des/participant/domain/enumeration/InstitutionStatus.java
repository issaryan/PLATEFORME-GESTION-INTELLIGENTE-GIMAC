package cm.gimac.des.participant.domain.enumeration;

/**
 * Cycle de vie institutionnel (REC D-08). Les états du CA (HOMOLOGATION,
 * SURVEILLANCE, SUSPENSION, RÉSILIÉ) sont caducs.
 */
public enum InstitutionStatus {
    CANDIDAT,
    EN_HOMOLOGATION,
    ACTIF,
    A_REGULARISER,
    SUSPENDU,
    RETIRE,
}
