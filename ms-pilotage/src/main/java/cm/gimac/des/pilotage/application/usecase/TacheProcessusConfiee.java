package cm.gimac.des.pilotage.application.usecase;

import java.util.UUID;

/**
 * Tâche d'une étape de processus prise ou attribuée par la voie des tâches (D-81 §6) : événement interne,
 * synchrone et dans la transaction, qui historise la prise dans l'instance (D-87).
 */
public record TacheProcessusConfiee(UUID tacheId, String reference, String responsableId, String acteurId, boolean prise) {}
