package cm.gimac.des.pilotage.domain.processus;

/**
 * Gravité d'une anomalie de définition : BLOQUANTE (publication refusée), LEVABLE (avertissement bloquant,
 * levé par un motif à la publication), AVERTISSEMENT (information, par exemple personne partie).
 */
public enum GraviteAnomalie {
    BLOQUANTE,
    LEVABLE,
    AVERTISSEMENT,
}
