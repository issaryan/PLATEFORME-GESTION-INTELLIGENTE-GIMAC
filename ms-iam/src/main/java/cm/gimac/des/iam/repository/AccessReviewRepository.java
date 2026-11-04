package cm.gimac.des.iam.repository;

import cm.gimac.des.iam.domain.AccessReview;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the AccessReview entity.
 */
@SuppressWarnings("unused")
@Repository
public interface AccessReviewRepository extends JpaRepository<AccessReview, UUID> {}
