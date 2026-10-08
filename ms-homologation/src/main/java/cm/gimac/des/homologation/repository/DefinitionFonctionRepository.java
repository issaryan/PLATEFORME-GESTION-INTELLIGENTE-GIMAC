package cm.gimac.des.homologation.repository;

import cm.gimac.des.homologation.domain.DefinitionFonction;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Catalogue des fonctions (D-91). */
@Repository
public interface DefinitionFonctionRepository extends JpaRepository<DefinitionFonction, String> {
    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select f from DefinitionFonction f where f.code = :code")
    Optional<DefinitionFonction> findByIdForUpdate(@Param("code") String code);
}
