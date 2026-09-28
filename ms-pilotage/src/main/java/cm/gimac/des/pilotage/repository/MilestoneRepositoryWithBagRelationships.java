package cm.gimac.des.pilotage.repository;

import cm.gimac.des.pilotage.domain.Milestone;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;

public interface MilestoneRepositoryWithBagRelationships {
    Optional<Milestone> fetchBagRelationships(Optional<Milestone> milestone);

    List<Milestone> fetchBagRelationships(List<Milestone> milestones);

    Page<Milestone> fetchBagRelationships(Page<Milestone> milestones);
}
