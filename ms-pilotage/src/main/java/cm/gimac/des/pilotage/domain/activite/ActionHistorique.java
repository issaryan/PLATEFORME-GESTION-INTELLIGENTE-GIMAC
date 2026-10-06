package cm.gimac.des.pilotage.domain.activite;

/** Action consignée dans l'historique d'une tâche (§2.3). */
public enum ActionHistorique {
    CREEE,
    MODIFIEE,
    REAFFECTEE,
    TRANSITION,
    /** Tâche à prendre prise par un membre de l'unité (D-81 §6). */
    PRISE,
    /** Tâche à prendre attribuée à un membre par le responsable de l'unité ou le créateur. */
    ATTRIBUEE,
    /** Relance adressée au responsable (une par 24 h). */
    RELANCEE,
}
