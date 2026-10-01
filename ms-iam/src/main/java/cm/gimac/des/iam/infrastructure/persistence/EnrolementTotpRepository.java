package cm.gimac.des.iam.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EnrolementTotpRepository extends JpaRepository<EnrolementTotp, UUID> {
    @Modifying
    @Query("delete from EnrolementTotp e where e.sujet = :sujet")
    int supprimerPour(@Param("sujet") String sujet);

    @Modifying
    @Query("delete from EnrolementTotp e where e.expireLe < :avant")
    int purger(@Param("avant") Instant avant);
}
