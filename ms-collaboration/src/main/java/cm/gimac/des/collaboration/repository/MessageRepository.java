package cm.gimac.des.collaboration.repository;

import cm.gimac.des.collaboration.domain.messagerie.Message;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Messages, immuables ; lecture du plus récent vers le passé (pagination par curseur). */
@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {
    String NON_LUS =
        "from Message m, ConversationMembre mb where mb.conversation = m.conversation and mb.membreId = :moi " +
        "and m.auteurId <> :moi and (mb.dernierLuLe is null or m.envoyeLe > mb.dernierLuLe)";

    @Query("select m from Message m where m.conversation.id = :conversation order by m.envoyeLe desc, m.id desc")
    List<Message> plusRecents(@Param("conversation") UUID conversationId, Pageable limite);

    @Query(
        "select m from Message m where m.conversation.id = :conversation " +
        "and (m.envoyeLe < :le or (m.envoyeLe = :le and m.id < :id)) order by m.envoyeLe desc, m.id desc"
    )
    List<Message> anterieurs(@Param("conversation") UUID conversationId, @Param("le") Instant le, @Param("id") UUID id, Pageable limite);

    Optional<Message> findFirstByConversationIdOrderByEnvoyeLeDescIdDesc(UUID conversationId);

    @Query("select count(m) " + NON_LUS)
    long compterNonLus(@Param("moi") String membreId);

    /** Paires (conversation, non-lus) des conversations données. */
    @Query("select m.conversation.id, count(m) " + NON_LUS + " and m.conversation.id in :ids group by m.conversation.id")
    List<Object[]> compterNonLusPar(@Param("moi") String membreId, @Param("ids") Collection<UUID> conversationIds);
}
