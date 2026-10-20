package cm.gimac.des.monitoring.infrastructure.persistance;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Rapports de monitoring (D-116 §10.1) : filtres, visibilité (auteur, destinataire ou chef), rapports quotidiens. */
public interface RapportsMonitoringRepository extends JpaRepository<RapportMonitoring, UUID> {
    @Query(
        "select r from RapportMonitoring r where (:tous = true or r.auteurId = :moi or exists (select e from EnvoiRapport e where e.rapportId = r.id and e.destinataireId = :moi))" +
        " and (:nature is null or r.nature = :nature) and (:auteur is null or r.auteurId = :auteur)" +
        " and (cast(:du as timestamp) is null or r.deposeLe >= :du) and (cast(:au as timestamp) is null or r.deposeLe < :au) order by r.deposeLe desc"
    )
    Page<RapportMonitoring> rechercher(
        @Param("tous") boolean tous,
        @Param("moi") UUID moi,
        @Param("nature") RapportMonitoring.Nature nature,
        @Param("auteur") UUID auteur,
        @Param("du") Instant du,
        @Param("au") Instant au,
        Pageable page
    );

    Optional<RapportMonitoring> findFirstByNatureAndDate(RapportMonitoring.Nature nature, LocalDate date);

    List<RapportMonitoring> findByNatureAndDateBetween(RapportMonitoring.Nature nature, LocalDate du, LocalDate au);

    List<RapportMonitoring> findByDeposeLeBetween(Instant du, Instant au);
}
