package cm.gimac.des.rapprochement.domain.enumeration;

/**
 * Automate d'un suspens (REC D-08). Seule la validation d'une attestation conduit à CLOS.
 */
public enum SuspensState {
    OUVERT,
    AFFECTE,
    EN_CONCILIATION,
    CONTESTE,
    REGULARISATION_DECLAREE,
    CLOS,
}
