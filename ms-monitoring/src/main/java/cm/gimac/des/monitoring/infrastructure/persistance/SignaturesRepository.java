package cm.gimac.des.monitoring.infrastructure.persistance;

import cm.gimac.des.monitoring.domain.ReportSignature;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Signatures d'un rapport de quart, dans l'ordre (algorithme 6.46). */
public interface SignaturesRepository extends JpaRepository<ReportSignature, UUID> {
    List<ReportSignature> findByReportIdOrderBySignedAtAsc(UUID rapport);
}
