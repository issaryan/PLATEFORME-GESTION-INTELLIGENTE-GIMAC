package cm.gimac.des.homologation.repository;

import cm.gimac.des.homologation.domain.sandbox.ReleveLiaisonSandbox;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** État de la relève des résultats de la sandbox, par liaison (US-EXI-14, US-EXI-17). */
@Repository
public interface ReleveLiaisonSandboxRepository extends JpaRepository<ReleveLiaisonSandbox, UUID> {}
