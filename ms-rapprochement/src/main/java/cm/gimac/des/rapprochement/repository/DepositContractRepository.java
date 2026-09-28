package cm.gimac.des.rapprochement.repository;

import cm.gimac.des.rapprochement.domain.DepositContract;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the DepositContract entity.
 */
@SuppressWarnings("unused")
@Repository
public interface DepositContractRepository extends JpaRepository<DepositContract, UUID> {}
