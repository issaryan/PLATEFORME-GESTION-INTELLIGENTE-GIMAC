package cm.gimac.des.pilotage.domain.activite;

/** Origine d'une tâche (§2.3 ; CC OriginKind) ; PROCESSUS : étape d'un processus d'unité (D-87). */
public enum TypeOrigine {
    INTERNE,
    OPERATION,
    REFERENTIEL,
    DECISION,
    TICKET,
    FICHE_ACTION,
    RISQUE,
    INDICATEUR,
    PROCESSUS,
}
