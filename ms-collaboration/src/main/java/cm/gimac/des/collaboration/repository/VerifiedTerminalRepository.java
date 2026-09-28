package cm.gimac.des.collaboration.repository;

import cm.gimac.des.collaboration.domain.VerifiedTerminal;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the VerifiedTerminal entity.
 */
@SuppressWarnings("unused")
@Repository
public interface VerifiedTerminalRepository extends JpaRepository<VerifiedTerminal, UUID>, JpaSpecificationExecutor<VerifiedTerminal> {}
