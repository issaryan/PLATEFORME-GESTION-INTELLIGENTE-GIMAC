package cm.gimac.des.pilotage.application.usecase;

import java.util.UUID;

/**
 * Contrôle synchrone avant la prise ({@code attribution} faux) ou l'attribution d'une tâche d'origine PROCESSUS
 * (D-99, séparation des fonctions) : le moteur des processus refuse si {@code compteId} est l'initiateur écarté.
 */
public record PriseTacheProcessus(UUID tacheId, String reference, String compteId, boolean attribution) {}
