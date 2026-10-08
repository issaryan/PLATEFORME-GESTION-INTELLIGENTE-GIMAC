package cm.gimac.des.pilotage.domain.processus;

/** Action consignée dans l'historique d'un processus ou d'une instance (qui, quoi, quand). */
public enum ActionProcessus {
    CREE,
    MODIFIE,
    SUPPRIME,
    PUBLIE,
    RETIRE,
    VERSION_CREEE,
    LANCEE,
    ETAPE_ACTIVEE,
    ETAPE_PRISE,
    LIVRABLE_DEPOSE,
    CHAMPS_RENSEIGNES,
    ETAPE_SOUMISE,
    SIGNEE,
    VALIDEE,
    RENVOYEE,
    ETAPE_TERMINEE,
    REAFFECTEE,
    DEPART,
    /** Intervenant revu après un changement d'organisation (profil retiré, changement d'unité, nouveau chef, D-93). */
    ORGANISATION,
    SUSPENDUE,
    REPRISE,
    ANNULEE,
    CLOSE,
}
