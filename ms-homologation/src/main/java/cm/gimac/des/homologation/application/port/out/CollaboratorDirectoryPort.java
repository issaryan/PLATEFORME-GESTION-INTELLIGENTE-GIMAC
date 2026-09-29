package cm.gimac.des.homologation.application.port.out;

import cm.gimac.des.homologation.domain.cadrage.CollaboratorView;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Annuaire des collaborateurs (MS-IAM, IAM-04) : habilitation du conducteur de projet (D-58). */
public interface CollaboratorDirectoryPort {
    Optional<CollaboratorView> findCollaborator(UUID userId);

    /** Collaborateurs actifs portant un profil (ROLE_CONDUCTEUR_PROJET pour la désignation). */
    List<CollaboratorView> parProfil(String profil);
}
