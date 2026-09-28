package cm.gimac.des.homologation.repository;

import cm.gimac.des.homologation.domain.ReferenceOperation;
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
public class ReferenceOperationRepositoryWithBagRelationshipsImpl implements ReferenceOperationRepositoryWithBagRelationships {

    private static final String ID_PARAMETER = "id";
    private static final String REFERENCEOPERATIONS_PARAMETER = "referenceOperations";

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Optional<ReferenceOperation> fetchBagRelationships(Optional<ReferenceOperation> referenceOperation) {
        return referenceOperation.map(this::fetchPredecessors);
    }

    @Override
    public Page<ReferenceOperation> fetchBagRelationships(Page<ReferenceOperation> referenceOperations) {
        return new PageImpl<>(
            fetchBagRelationships(referenceOperations.getContent()),
            referenceOperations.getPageable(),
            referenceOperations.getTotalElements()
        );
    }

    @Override
    public List<ReferenceOperation> fetchBagRelationships(List<ReferenceOperation> referenceOperations) {
        return Optional.of(referenceOperations).map(this::fetchPredecessors).orElse(Collections.emptyList());
    }

    ReferenceOperation fetchPredecessors(ReferenceOperation result) {
        return entityManager
            .createQuery(
                "select referenceOperation from ReferenceOperation referenceOperation left join fetch referenceOperation.predecessors where referenceOperation.id = :id",
                ReferenceOperation.class
            )
            .setParameter(ID_PARAMETER, result.getId())
            .getSingleResult();
    }

    List<ReferenceOperation> fetchPredecessors(List<ReferenceOperation> referenceOperations) {
        HashMap<Object, Integer> order = new HashMap<>();
        IntStream.range(0, referenceOperations.size()).forEach(index -> order.put(referenceOperations.get(index).getId(), index));
        List<ReferenceOperation> result = entityManager
            .createQuery(
                "select referenceOperation from ReferenceOperation referenceOperation left join fetch referenceOperation.predecessors where referenceOperation in :referenceOperations",
                ReferenceOperation.class
            )
            .setParameter(REFERENCEOPERATIONS_PARAMETER, referenceOperations)
            .getResultList();
        Collections.sort(result, (o1, o2) -> Integer.compare(order.get(o1.getId()), order.get(o2.getId())));
        return result;
    }
}
