package cm.gimac.des.incident.repository;

import cm.gimac.des.incident.domain.QualificationMatrix;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the QualificationMatrix entity.
 */
@SuppressWarnings("unused")
@Repository
public interface QualificationMatrixRepository extends JpaRepository<QualificationMatrix, UUID> {}
