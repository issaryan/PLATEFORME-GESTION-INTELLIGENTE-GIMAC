package cm.gimac.des.homologation.repository;

import cm.gimac.des.homologation.domain.Liaison;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Liaison entity.
 */
@SuppressWarnings("unused")
@Repository
public interface LiaisonRepository extends JpaRepository<Liaison, UUID> {}
