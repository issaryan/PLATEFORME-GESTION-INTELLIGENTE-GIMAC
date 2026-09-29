package cm.gimac.des.homologation.infrastructure.client;

import cm.gimac.des.homologation.application.port.out.CollaboratorDirectoryPort;
import cm.gimac.des.homologation.domain.cadrage.CollaboratorView;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Adaptateur de substitution de l'annuaire (développement, tests) : deux conducteurs de projet de démonstration. */
@Component
@ConditionalOnProperty(name = "gimac.adapters.collaborator-reader", havingValue = "stub", matchIfMissing = true)
public class CollaboratorStubAdapter implements CollaboratorDirectoryPort {

    public static final UUID CONDUCTEUR_1 = UUID.fromString("00000000-0000-4000-8000-0000000c0001");
    public static final UUID CONDUCTEUR_2 = UUID.fromString("00000000-0000-4000-8000-0000000c0002");

    private final Map<UUID, CollaboratorView> collaborateurs = new ConcurrentHashMap<>();

    public CollaboratorStubAdapter() {
        enregistrer(new CollaboratorView(CONDUCTEUR_1, "Aminata Ngono", Set.of("ROLE_CONDUCTEUR_PROJET"), null, true));
        enregistrer(new CollaboratorView(CONDUCTEUR_2, "Paul Essomba", Set.of("ROLE_CONDUCTEUR_PROJET", "ROLE_SNA"), null, true));
    }

    public void enregistrer(CollaboratorView c) {
        collaborateurs.put(c.id(), c);
    }

    @Override
    public Optional<CollaboratorView> findCollaborator(UUID userId) {
        return Optional.ofNullable(collaborateurs.get(userId));
    }

    @Override
    public List<CollaboratorView> parProfil(String profil) {
        return collaborateurs.values().stream().filter(c -> c.actif() && c.profils().contains(profil)).sorted(Comparator.comparing(CollaboratorView::nomAffiche)).toList();
    }
}
