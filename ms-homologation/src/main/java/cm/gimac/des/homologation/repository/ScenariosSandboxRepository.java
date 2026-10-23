package cm.gimac.des.homologation.repository;

import cm.gimac.des.homologation.domain.UnmatchedScenario;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Rapprochement des scénarios de la sandbox (US-EXI-14, HOM-15) : scénarios portés par les opérations de la version du
 * référentiel d'une liaison, et scénarios non rapprochés isolés (table générée unmatched_scenario, unique par liaison,
 * campagne et scénario).
 */
@Repository
public interface ScenariosSandboxRepository extends JpaRepository<UnmatchedScenario, UUID> {
    /** Paires (identifiant de scénario, code de l'opération) de la version du référentiel. */
    @Query("select o.idScenario, o.code from ReferenceOperation o where o.version.id = :version and o.idScenario is not null")
    List<Object[]> scenariosDeLaVersion(@Param("version") UUID versionId);

    boolean existsByLiaison_IdAndCampagneIdAndIdScenario(UUID liaisonId, String campagneId, String idScenario);

    List<UnmatchedScenario> findByLiaison_IdOrderByDateDetectionAsc(UUID liaisonId);
}
