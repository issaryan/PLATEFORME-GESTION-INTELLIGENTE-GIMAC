package cm.gimac.des.pilotage.repository;

import cm.gimac.des.pilotage.domain.Milestone;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

/**
 * Utility repository to load bag relationships based on https://vladmihalcea.com/hibernate-multiplebagfetchexception/
 */
public class MilestoneRepositoryWithBagRelationshipsImpl implements MilestoneRepositoryWithBagRelationships {

    private static final String ID_PARAMETER = "id";
    private static final String MILESTONES_PARAMETER = "milestones";

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Optional<Milestone> fetchBagRelationships(Optional<Milestone> milestone) {
        return milestone.map(this::fetchPredecessors);
    }

    @Override
    public Page<Milestone> fetchBagRelationships(Page<Milestone> milestones) {
        return new PageImpl<>(fetchBagRelationships(milestones.getContent()), milestones.getPageable(), milestones.getTotalElements());
    }

    @Override
    public List<Milestone> fetchBagRelationships(List<Milestone> milestones) {
        return Optional.of(milestones).map(this::fetchPredecessors).orElse(Collections.emptyList());
    }

    Milestone fetchPredecessors(Milestone result) {
        return entityManager
            .createQuery(
                "select milestone from Milestone milestone left join fetch milestone.predecessors where milestone.id = :id",
                Milestone.class
            )
            .setParameter(ID_PARAMETER, result.getId())
            .getSingleResult();
    }

    List<Milestone> fetchPredecessors(List<Milestone> milestones) {
        HashMap<Object, Integer> order = new HashMap<>();
        IntStream.range(0, milestones.size()).forEach(index -> order.put(milestones.get(index).getId(), index));
        List<Milestone> result = entityManager
            .createQuery(
                "select milestone from Milestone milestone left join fetch milestone.predecessors where milestone in :milestones",
                Milestone.class
            )
            .setParameter(MILESTONES_PARAMETER, milestones)
            .getResultList();
        Collections.sort(result, (o1, o2) -> Integer.compare(order.get(o1.getId()), order.get(o2.getId())));
        return result;
    }
}
