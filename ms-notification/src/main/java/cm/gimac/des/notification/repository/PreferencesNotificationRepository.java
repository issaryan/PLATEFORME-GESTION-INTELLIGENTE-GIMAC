package cm.gimac.des.notification.repository;

import cm.gimac.des.notification.domain.boite.PreferencesNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Préférences de notification par compte (§3) ; un compte sans ligne a les valeurs par défaut. */
@Repository
public interface PreferencesNotificationRepository extends JpaRepository<PreferencesNotification, String> {}
