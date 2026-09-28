package cm.gimac.des.reporting.repository;

import cm.gimac.des.reporting.domain.ReportVisa;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the ReportVisa entity.
 */
@SuppressWarnings("unused")
@Repository
public interface ReportVisaRepository extends JpaRepository<ReportVisa, UUID> {}
