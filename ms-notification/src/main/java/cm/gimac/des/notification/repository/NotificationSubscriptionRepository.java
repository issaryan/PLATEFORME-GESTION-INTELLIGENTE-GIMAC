package cm.gimac.des.notification.repository;

import cm.gimac.des.notification.domain.NotificationSubscription;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the NotificationSubscription entity.
 */
@SuppressWarnings("unused")
@Repository
public interface NotificationSubscriptionRepository
    extends JpaRepository<NotificationSubscription, UUID>, JpaSpecificationExecutor<NotificationSubscription> {}
