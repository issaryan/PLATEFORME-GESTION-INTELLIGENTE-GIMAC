package cm.gimac.des.homologation.repository;

import cm.gimac.des.homologation.domain.DefinitionTypeProjet;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Types de projet (D-91). */
@Repository
public interface DefinitionTypeProjetRepository extends JpaRepository<DefinitionTypeProjet, String> {
    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from DefinitionTypeProjet t where t.code = :code")
    Optional<DefinitionTypeProjet> findByIdForUpdate(@Param("code") String code);

    List<DefinitionTypeProjet> findByActif(boolean actif);
}
