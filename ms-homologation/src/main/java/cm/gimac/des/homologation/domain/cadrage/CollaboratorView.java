package cm.gimac.des.homologation.domain.cadrage;

import java.util.Set;
import java.util.UUID;

/** Collaborateur lu dans l'annuaire de MS-IAM (IAM-04) : profils, rattachement, activité. */
public record CollaboratorView(UUID id, String nomAffiche, Set<String> profils, String institutionId, boolean actif) {
    public CollaboratorView {
        profils = profils == null ? Set.of() : Set.copyOf(profils);
    }
}
