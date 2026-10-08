package cm.gimac.des.certification.repository;

import cm.gimac.des.certification.domain.operation.OperationLiaison;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Projection des opérations du parcours (D-93 §4). */
@Repository
public interface OperationLiaisonRepository extends JpaRepository<OperationLiaison, UUID> {
    List<OperationLiaison> findByLiaisonId(UUID liaisonId);
}
