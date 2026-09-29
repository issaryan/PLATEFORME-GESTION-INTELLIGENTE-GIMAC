package cm.gimac.des.iam.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JournalAdministrationRepository extends JpaRepository<JournalAdministration, UUID> {
    Page<JournalAdministration> findAllByOrderByHorodatageDesc(Pageable pageable);

    Page<JournalAdministration> findByCibleIdOrderByHorodatageDesc(String cibleId, Pageable pageable);

    long countByHorodatageAfter(Instant depuis);
}
