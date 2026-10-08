package cm.gimac.des.certification.domain.operation;

/**
 * État d'une opération du parcours tel que MS-CERTIFICATION le projette depuis les événements de MS-HOMOLOGATION
 * (D-93 §4) : seule {@code OUVERTE} admet un geste.
 */
public enum EtatOperation {
    /** homologation.operation-activated.v1 (ouverture, renvoi, reprise). */
    OUVERTE,
    /** homologation.operation-validation-requested.v1. */
    EN_VALIDATION,
    /** homologation.operation-signed.v1 sans clôture : signatures D-91 en attente. */
    EN_SIGNATURE,
    /**
     * homologation.operation-closed.v1 (FAITE), homologation.operation-signed.v1 avec clôture, ou
     * homologation.liaison-milestone-reached.v1 de l'opération.
     */
    FAITE,
    /** homologation.operation-closed.v1 (SANS_OBJET) : opération écartée à l'instanciation ou par une migration. */
    SANS_OBJET,
}
