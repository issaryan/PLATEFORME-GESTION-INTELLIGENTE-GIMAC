package cm.gimac.des.homologation.application.port.out;

import cm.gimac.des.homologation.domain.cadrage.CollaboratorView;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Port sortant : annuaire des collaborateurs du GIMAC (MS-IAM, IAM-04). */
public interface CollaboratorDirectoryPort {
    Optional<CollaboratorView> findCollaborator(UUID userId);

    /** Collaborateurs détenteurs d'un profil (par exemple CONDUCTEUR_PROJET). */
    List<CollaboratorView> parProfil(String profil);
}
