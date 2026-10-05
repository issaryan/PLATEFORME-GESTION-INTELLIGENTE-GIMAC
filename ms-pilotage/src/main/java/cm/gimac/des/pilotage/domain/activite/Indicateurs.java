package cm.gimac.des.pilotage.domain.activite;

/**
 * Indicateurs d'un rapport (§2.4) sur sa période : activités faites et prévues (hors annulées et
 * reportées), minutes passées sur les activités faites, tâches terminées par l'auteur.
 */
public record Indicateurs(int activitesFaites, int activitesPrevues, int minutes, int tachesTerminees) {}
