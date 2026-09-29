package cm.gimac.des.certification.repository;

import cm.gimac.des.certification.domain.GoLiveReport;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the GoLiveReport entity.
 */
@SuppressWarnings("unused")
@Repository
public interface GoLiveReportRepository extends JpaRepository<GoLiveReport, UUID> {}
