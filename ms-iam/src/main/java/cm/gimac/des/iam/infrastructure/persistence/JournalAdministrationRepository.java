package cm.gimac.des.iam.infrastructure.persistence;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JournalAdministrationRepository extends JpaRepository<JournalAdministration, UUID> {
    Page<JournalAdministration> findAllByOrderByHorodatageDesc(Pageable pageable);

    Page<JournalAdministration> findByCibleIdOrderByHorodatageDesc(String cibleId, Pageable pageable);

    long countByHorodatageAfter(Instant depuis);

    /** Horodatages des actes postérieurs à « depuis » (activité par jour des tableaux de bord). */
    @Query("select j.horodatage from JournalAdministration j where j.horodatage > :depuis")
    List<Instant> horodatagesDepuis(@Param("depuis") Instant depuis);
}
