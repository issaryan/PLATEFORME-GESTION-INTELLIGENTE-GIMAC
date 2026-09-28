package cm.gimac.des.reporting.repository;

import cm.gimac.des.reporting.domain.CockpitView;
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
public class CockpitViewRepositoryWithBagRelationshipsImpl implements CockpitViewRepositoryWithBagRelationships {

    private static final String ID_PARAMETER = "id";
    private static final String COCKPITVIEWS_PARAMETER = "cockpitViews";

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Optional<CockpitView> fetchBagRelationships(Optional<CockpitView> cockpitView) {
        return cockpitView.map(this::fetchIndicators);
    }

    @Override
    public Page<CockpitView> fetchBagRelationships(Page<CockpitView> cockpitViews) {
        return new PageImpl<>(
            fetchBagRelationships(cockpitViews.getContent()),
            cockpitViews.getPageable(),
            cockpitViews.getTotalElements()
        );
    }

    @Override
    public List<CockpitView> fetchBagRelationships(List<CockpitView> cockpitViews) {
        return Optional.of(cockpitViews).map(this::fetchIndicators).orElse(Collections.emptyList());
    }

    CockpitView fetchIndicators(CockpitView result) {
        return entityManager
            .createQuery(
                "select cockpitView from CockpitView cockpitView left join fetch cockpitView.indicators where cockpitView.id = :id",
                CockpitView.class
            )
            .setParameter(ID_PARAMETER, result.getId())
            .getSingleResult();
    }

    List<CockpitView> fetchIndicators(List<CockpitView> cockpitViews) {
        HashMap<Object, Integer> order = new HashMap<>();
        IntStream.range(0, cockpitViews.size()).forEach(index -> order.put(cockpitViews.get(index).getId(), index));
        List<CockpitView> result = entityManager
            .createQuery(
                "select cockpitView from CockpitView cockpitView left join fetch cockpitView.indicators where cockpitView in :cockpitViews",
                CockpitView.class
            )
            .setParameter(COCKPITVIEWS_PARAMETER, cockpitViews)
            .getResultList();
        Collections.sort(result, (o1, o2) -> Integer.compare(order.get(o1.getId()), order.get(o2.getId())));
        return result;
    }
}
