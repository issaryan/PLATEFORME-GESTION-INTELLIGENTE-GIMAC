package cm.gimac.des.collaboration.repository;

import cm.gimac.des.collaboration.domain.messagerie.LectureConversation;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Avancées des positions de lecture (D-88), en ajout seulement. */
@Repository
public interface LectureConversationRepository extends JpaRepository<LectureConversation, UUID> {
    /** Heure de lecture d'un message envoyé à {@code envoyeLe}, par membre : première avancée qui l'a couvert. */
    record PremiereLecture(String membreId, Instant luLe) {}

    @Query(
        "select new cm.gimac.des.collaboration.repository.LectureConversationRepository$PremiereLecture(l.membreId, min(l.luLe)) " +
        "from LectureConversation l where l.conversation.id = :conversation and l.luJusquA >= :envoyeLe group by l.membreId"
    )
    List<PremiereLecture> premieresLectures(@Param("conversation") UUID conversationId, @Param("envoyeLe") Instant envoyeLe);
}
