package cm.gimac.des.document.repository;

import cm.gimac.des.document.domain.DepotDocument;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Métadonnées des fichiers déposés (table en insertion seule). */
@Repository
public interface DepotDocumentRepository extends JpaRepository<DepotDocument, UUID> {
    /** Empreinte seule, sans charger l'entité : interface interne de vérification. */
    @Query("select d.sha256 from DepotDocument d where d.id = :id")
    Optional<String> findSha256ById(@Param("id") UUID id);
}
