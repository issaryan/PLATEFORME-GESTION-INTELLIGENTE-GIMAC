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

    /**
     * Document de même empreinte et de même taille, le plus ancien (déduplication des dépôts
     * fragmentés, D-74) : le nouveau document pointe vers le même objet du dépôt WORM.
     */
    Optional<DepotDocument> findFirstBySha256AndTailleOrderByDeposeLeAsc(String sha256, long taille);
}
