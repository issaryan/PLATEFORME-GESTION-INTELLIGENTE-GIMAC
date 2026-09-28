package cm.gimac.des.notification.repository;

import cm.gimac.des.notification.domain.NotificationRecipient;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the NotificationRecipient entity.
 */
@SuppressWarnings("unused")
@Repository
public interface NotificationRecipientRepository
    extends JpaRepository<NotificationRecipient, UUID>, JpaSpecificationExecutor<NotificationRecipient> {}
