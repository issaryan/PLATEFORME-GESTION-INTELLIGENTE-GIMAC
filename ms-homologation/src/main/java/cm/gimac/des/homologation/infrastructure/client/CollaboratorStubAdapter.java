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

/**
 * Adaptateur de substitution de l'annuaire (développement, tests) : deux conducteurs de projet de démonstration ; la
 * composition des profils en permissions (D-94) est celle du domaine Keycloak généré, modifiable par {@link #composer}.
 */
@Component
@ConditionalOnProperty(name = "gimac.adapters.collaborator-reader", havingValue = "stub", matchIfMissing = true)
public class CollaboratorStubAdapter implements CollaboratorDirectoryPort {

    public static final UUID CONDUCTEUR_1 = UUID.fromString("00000000-0000-4000-8000-0000000c0001");
    public static final UUID CONDUCTEUR_2 = UUID.fromString("00000000-0000-4000-8000-0000000c0002");

    private final Map<UUID, CollaboratorView> collaborateurs = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> compositions = new ConcurrentHashMap<>();

    public CollaboratorStubAdapter() {
        enregistrer(new CollaboratorView(CONDUCTEUR_1, "Aminata Ngono", Set.of("ROLE_CONDUCTEUR_PROJET"), null, true));
        enregistrer(new CollaboratorView(CONDUCTEUR_2, "Paul Essomba", Set.of("ROLE_CONDUCTEUR_PROJET", "ROLE_SNA"), null, true));
        reinitialiserCompositions();
    }

    /** Compositions par défaut (infra/keycloak/generer-realm-gimac.py) des permissions lues par MS-HOMOLOGATION. */
    public final void reinitialiserCompositions() {
        compositions.clear();
        compositions.put("ROLE_CONDUCTEUR_PROJET", Set.of("HOM_PROJECT_CONDUCT"));
        compositions.put("ROLE_INGENIEUR_HOMOLOGATION", Set.of("HOM_REFERENTIEL_EDIT", "REF_METIER_ADMIN"));
        compositions.put("ROLE_SUPER_ADMIN", Set.of("HOM_REFERENTIEL_EDIT", "REF_METIER_ADMIN"));
        compositions.put("ROLE_CHEF_DES", Set.of("HOM_REFERENTIEL_EDIT"));
        compositions.put("ROLE_CHEF_DDRM", Set.of("HOM_REFERENTIEL_EDIT"));
        compositions.put("ROLE_RESPONSABLE_SERVICE", Set.of("HOM_REFERENTIEL_EDIT"));
    }

    /** Permissions d'un profil (profil créé ou modifié par l'administration, D-81, D-86, D-94). */
    public void composer(String profil, Set<String> permissions) {
        compositions.put(profil, Set.copyOf(permissions));
    }

    public void enregistrer(CollaboratorView c) {
        collaborateurs.put(c.id(), c);
    }

    @Override
    public Optional<CollaboratorView> findCollaborator(UUID userId) {
        return Optional.ofNullable(collaborateurs.get(userId));
    }

    @Override
    public List<CollaboratorView> parPermission(String permission) {
        return collaborateurs
            .values()
            .stream()
            .filter(c -> c.actif() && c.profils().stream().anyMatch(p -> compositions.getOrDefault(p, Set.of()).contains(permission)))
            .sorted(Comparator.comparing(CollaboratorView::nomAffiche))
            .toList();
    }

    @Override
    public List<CollaboratorView> parProfil(String profil) {
        return collaborateurs.values().stream().filter(c -> c.actif() && c.profils().contains(profil)).sorted(Comparator.comparing(CollaboratorView::nomAffiche)).toList();
    }
}
