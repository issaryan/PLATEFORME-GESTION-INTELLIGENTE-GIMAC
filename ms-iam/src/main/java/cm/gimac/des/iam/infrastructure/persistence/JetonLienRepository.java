package cm.gimac.des.iam.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JetonLienRepository extends JpaRepository<JetonLien, UUID> {
    /** Verrou : un jeton ne sert qu'une fois, même sous des soumissions concurrentes. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select j from JetonLien j where j.empreinte = :empreinte")
    Optional<JetonLien> findByEmpreinteForUpdate(@Param("empreinte") String empreinte);

    @Query("select j from JetonLien j where j.compteId = :compteId and j.utiliseLe is null")
    List<JetonLien> ouverts(@Param("compteId") String compteId);

    @Modifying
    @Query("delete from JetonLien j where j.expireLe < :avant")
    int purger(@Param("avant") Instant avant);
}
