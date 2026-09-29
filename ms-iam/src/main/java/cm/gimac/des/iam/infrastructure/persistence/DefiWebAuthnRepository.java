package cm.gimac.des.iam.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DefiWebAuthnRepository extends JpaRepository<DefiWebAuthn, UUID> {
    /** Verrou : un défi n'est consommé qu'une fois, même sous vérifications concurrentes. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from DefiWebAuthn d where d.id = :id")
    Optional<DefiWebAuthn> findByIdForUpdate(@Param("id") UUID id);

    @Modifying
    @Query("delete from DefiWebAuthn d where d.expireLe < :avant")
    int purger(@Param("avant") Instant avant);
}
