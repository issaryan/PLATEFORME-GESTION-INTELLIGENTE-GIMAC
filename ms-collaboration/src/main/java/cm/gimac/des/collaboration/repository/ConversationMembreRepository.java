package cm.gimac.des.collaboration.repository;

import cm.gimac.des.collaboration.domain.messagerie.ConversationMembre;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConversationMembreRepository extends JpaRepository<ConversationMembre, UUID> {
    Optional<ConversationMembre> findByConversationIdAndMembreId(UUID conversationId, String membreId);
}
