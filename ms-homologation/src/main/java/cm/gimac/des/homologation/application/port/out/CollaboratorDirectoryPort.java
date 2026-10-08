package cm.gimac.des.homologation.application.port.out;

import cm.gimac.des.homologation.domain.cadrage.CollaboratorView;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Annuaire des collaborateurs (MS-IAM, IAM-04) : habilitation du conducteur de projet (D-58), porteurs d'une permission (D-94). */
public interface CollaboratorDirectoryPort {
    Optional<CollaboratorView> findCollaborator(UUID userId);

    /** Collaborateurs actifs portant un profil (intervenants d'une fonction du catalogue, D-91). */
    List<CollaboratorView> parProfil(String profil);

    /**
     * Comptes actifs dont un profil porte la permission (D-94 §3 : {@code GET /api/v1/interne/collaborateurs?permission=},
     * docs/lot1/ecrans-par-configuration.md §6.6) ; permission portée par aucun profil : liste vide. MS-IAM injoignable
     * ou en refus : 503 HOM_IAM_UNAVAILABLE (aucun repli).
     */
    List<CollaboratorView> parPermission(String permission);
}
