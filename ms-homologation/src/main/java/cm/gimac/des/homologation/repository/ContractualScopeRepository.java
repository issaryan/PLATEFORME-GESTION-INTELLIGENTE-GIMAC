package cm.gimac.des.homologation.repository;

import cm.gimac.des.homologation.domain.ContractualScope;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the ContractualScope entity.
 */
@SuppressWarnings("unused")
@Repository
public interface ContractualScopeRepository extends JpaRepository<ContractualScope, UUID> {}
