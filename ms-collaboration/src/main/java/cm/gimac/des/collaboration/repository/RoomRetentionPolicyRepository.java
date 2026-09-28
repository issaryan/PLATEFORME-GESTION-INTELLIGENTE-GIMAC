package cm.gimac.des.collaboration.repository;

import cm.gimac.des.collaboration.domain.RoomRetentionPolicy;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the RoomRetentionPolicy entity.
 */
@SuppressWarnings("unused")
@Repository
public interface RoomRetentionPolicyRepository extends JpaRepository<RoomRetentionPolicy, UUID> {}
