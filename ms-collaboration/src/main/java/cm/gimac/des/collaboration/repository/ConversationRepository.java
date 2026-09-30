package cm.gimac.des.collaboration.repository;

import cm.gimac.des.collaboration.domain.messagerie.Conversation;
import cm.gimac.des.collaboration.domain.messagerie.ConversationMembre;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Subquery;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Conversations de la messagerie (D-64) : toute lecture est bornée aux membres. */
@Repository
public interface ConversationRepository extends JpaRepository<Conversation, UUID>, JpaSpecificationExecutor<Conversation> {
    Optional<Conversation> findByCleDirecte(String cleDirecte);

    @Query(
        "select c from Conversation c where c.id = :id and exists " +
        "(select 1 from ConversationMembre m where m.conversation = c and m.membreId = :membre)"
    )
    Optional<Conversation> pourMembre(@Param("id") UUID id, @Param("membre") String membreId);

    /** Conversations dont la personne est membre, filtrées par liaison et par texte (titre, référence, membres). */
    static Specification<Conversation> visibles(String membreId, String recherche, UUID liaisonId) {
        return (racine, requete, cb) -> {
            List<Predicate> conditions = new ArrayList<>();
            Subquery<Integer> membre = requete.subquery(Integer.class);
            var m = membre.from(ConversationMembre.class);
            membre.select(cb.literal(1)).where(cb.equal(m.get("conversation"), racine), cb.equal(m.get("membreId"), membreId));
            conditions.add(cb.exists(membre));
            if (liaisonId != null) {
                conditions.add(cb.equal(racine.get("liaisonId"), liaisonId));
            }
            if (recherche != null && !recherche.isBlank()) {
                String motif = "%" + recherche.strip().toLowerCase(Locale.ROOT).replace("%", "\\%").replace("_", "\\_") + "%";
                Subquery<Integer> nom = requete.subquery(Integer.class);
                var n = nom.from(ConversationMembre.class);
                nom.select(cb.literal(1)).where(cb.equal(n.get("conversation"), racine), cb.like(cb.lower(n.get("nom")), motif, '\\'));
                conditions.add(
                    cb.or(
                        cb.like(cb.lower(cb.coalesce(racine.get("titre"), "")), motif, '\\'),
                        cb.like(cb.lower(cb.coalesce(racine.get("liaisonReference"), "")), motif, '\\'),
                        cb.exists(nom)
                    )
                );
            }
            return cb.and(conditions.toArray(Predicate[]::new));
        };
    }
}
