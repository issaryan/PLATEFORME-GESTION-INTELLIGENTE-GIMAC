package cm.gimac.des.notification.repository;

import cm.gimac.des.notification.domain.ChannelPreference;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the ChannelPreference entity.
 */
@SuppressWarnings("unused")
@Repository
public interface ChannelPreferenceRepository extends JpaRepository<ChannelPreference, UUID>, JpaSpecificationExecutor<ChannelPreference> {}
