package cm.gimac.des.homologation.repository;

import cm.gimac.des.homologation.domain.ReferenceOperation;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;

public interface ReferenceOperationRepositoryWithBagRelationships {
    Optional<ReferenceOperation> fetchBagRelationships(Optional<ReferenceOperation> referenceOperation);

    List<ReferenceOperation> fetchBagRelationships(List<ReferenceOperation> referenceOperations);

    Page<ReferenceOperation> fetchBagRelationships(Page<ReferenceOperation> referenceOperations);
}
