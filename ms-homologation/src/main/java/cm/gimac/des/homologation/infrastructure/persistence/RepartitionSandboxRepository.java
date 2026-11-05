package cm.gimac.des.homologation.infrastructure.persistence;

import cm.gimac.des.homologation.domain.sandbox.ExecutionSandbox;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Répartition des exécutions relevées d'une campagne (D-121), calculée par la base sur sbx_execution (une ligne par
 * exécution retenue) : par cas, défavorables d'abord, au plus 200 cas ; par jour du fuseau demandé, au plus 366 jours.
 * Les dates sont stockées en temps universel (hibernate.jdbc.time_zone UTC).
 */
public interface RepartitionSandboxRepository extends JpaRepository<ExecutionSandbox, UUID> {
    interface ParCas {
        String getIdentifiantCas();
        long getExecutions();
        long getFavorables();
        long getDefavorables();
        long getNonExecutes();
        long getEnAttente();
    }

    interface ParJour {
        String getJour();
        long getExecutions();
        long getFavorables();
        long getDefavorables();
    }

    @Query(
        value = "select coalesce(identifiant_cas, '') as identifiantCas, count(*) as executions, " +
        "count(*) filter (where lower(verdict_normalise) = 'favorable') as favorables, " +
        "count(*) filter (where lower(verdict_normalise) = 'defavorable') as defavorables, " +
        "count(*) filter (where lower(verdict_normalise) = 'non_execute') as nonExecutes, " +
        "count(*) filter (where verdict_normalise is null) as enAttente " +
        "from sbx_execution where liaison_id = :liaison and campagne_id = :campagne " +
        "group by identifiant_cas order by defavorables desc, identifiantCas limit 200",
        nativeQuery = true
    )
    List<ParCas> parCas(@Param("liaison") UUID liaison, @Param("campagne") String campagne);

    @Query(
        value = "select to_char((date_execution at time zone 'UTC') at time zone :fuseau, 'YYYY-MM-DD') as jour, count(*) as executions, " +
        "count(*) filter (where lower(verdict_normalise) = 'favorable') as favorables, " +
        "count(*) filter (where lower(verdict_normalise) = 'defavorable') as defavorables " +
        "from sbx_execution where liaison_id = :liaison and campagne_id = :campagne and date_execution is not null " +
        "group by 1 order by 1 desc limit 366",
        nativeQuery = true
    )
    List<ParJour> parJour(@Param("liaison") UUID liaison, @Param("campagne") String campagne, @Param("fuseau") String fuseau);
}
