package cm.gimac.des.collaboration.repository;

import cm.gimac.des.collaboration.domain.messagerie.PieceJointe;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PieceJointeRepository extends JpaRepository<PieceJointe, UUID> {
    /** Pièce, avec son message et sa conversation, pour contrôler l'appartenance du lecteur. */
    @Query("select p from PieceJointe p join fetch p.message m join fetch m.conversation where p.id = :id")
    Optional<PieceJointe> avecConversation(@Param("id") UUID id);
}
