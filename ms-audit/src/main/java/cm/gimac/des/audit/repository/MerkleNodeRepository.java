package cm.gimac.des.audit.repository;

import cm.gimac.des.audit.domain.MerkleNode;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the MerkleNode entity.
 */
@SuppressWarnings("unused")
@Repository
public interface MerkleNodeRepository extends JpaRepository<MerkleNode, UUID> {
    /** Nœud d'un sous-arbre complet (unique par niveau et position). */
    Optional<MerkleNode> findFirstByLevelAndPosition(Integer level, Long position);
}
