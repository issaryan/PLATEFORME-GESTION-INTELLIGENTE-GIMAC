package cm.gimac.des.iam.infrastructure.persistence.sandbox;

import cm.gimac.des.iam.domain.Collaborator;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Collaborateurs par identifiant fédéré (réexamen des accès, US-EXI-23), hors du dépôt généré CollaboratorRepository (D-48). */
public interface CollaborateursFederesRepository extends JpaRepository<Collaborator, UUID> {
    Optional<Collaborator> findByFederatedId(String federatedId);
}
