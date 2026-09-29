package cm.gimac.des.homologation.repository;

import cm.gimac.des.homologation.domain.DeadlineNotification;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the DeadlineNotification entity.
 */
@SuppressWarnings("unused")
@Repository
public interface DeadlineNotificationRepository extends JpaRepository<DeadlineNotification, UUID> {}
