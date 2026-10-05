package cm.gimac.des.notification.repository;

import cm.gimac.des.notification.domain.boite.NotificationUtilisateur;
import cm.gimac.des.notification.domain.boite.TypeNotification;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Notifications par destinataire (D-80) : toute lecture est bornée au destinataire. */
@Repository
public interface NotificationUtilisateurRepository extends JpaRepository<NotificationUtilisateur, UUID> {
    Optional<NotificationUtilisateur> findByIdAndDestinataireId(UUID id, String destinataireId);

    /** Notification non lue à regrouper : même destinataire, même type, même objet. */
    @Query(
        "select n from NotificationUtilisateur n where n.destinataireId = :d and n.type = :t and n.lueLe is null " +
        "and n.reference.service = :s and n.reference.type = :rt and n.reference.id = :rid"
    )
    Optional<NotificationUtilisateur> nonLuePour(
        @Param("d") String destinataireId,
        @Param("t") TypeNotification type,
        @Param("s") String service,
        @Param("rt") String referenceType,
        @Param("rid") String referenceId
    );

    Page<NotificationUtilisateur> findByDestinataireId(String destinataireId, Pageable pageable);

    Page<NotificationUtilisateur> findByDestinataireIdAndLueLeIsNull(String destinataireId, Pageable pageable);

    long countByDestinataireIdAndLueLeIsNull(String destinataireId);

    /** Non-lues du destinataire (marquage en lecture, §3 POST /notifications/lecture). */
    java.util.List<NotificationUtilisateur> findByDestinataireIdAndLueLeIsNull(String destinataireId);

    /** Purge (§2) : notifications dont la dernière mise à jour précède la limite de conservation. */
    @Modifying
    @Query("delete from NotificationUtilisateur n where n.misAJourLe < :limite")
    int purger(@Param("limite") Instant limite);
}
