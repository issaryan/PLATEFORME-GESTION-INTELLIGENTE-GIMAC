package cm.gimac.des.participant.application.usecase;

import cm.gimac.des.participant.application.port.out.VersionsStatutRegistre;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Versions de statut en mémoire pour les tests sans base (même règle que la table : 1 au premier changement). */
class VersionsStatutMemoire implements VersionsStatutRegistre {

    final Map<UUID, Long> versions = new ConcurrentHashMap<>();

    @Override
    public long suivante(UUID institutionId) {
        // Institution non encore enregistrée par un dépôt simulé (identifiant nul) : première version.
        return institutionId == null ? 1L : versions.merge(institutionId, 1L, Long::sum);
    }

    @Override
    public long courante(UUID institutionId) {
        return versions.getOrDefault(institutionId, 0L);
    }
}
