package cm.gimac.des.certification.repository;

import cm.gimac.des.certification.domain.TimestampToken;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the TimestampToken entity.
 */
@SuppressWarnings("unused")
@Repository
public interface TimestampTokenRepository extends JpaRepository<TimestampToken, UUID> {}
