package cm.gimac.des.iam.repository;

import cm.gimac.des.iam.domain.Role;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;

public interface RoleRepositoryWithBagRelationships {
    Optional<Role> fetchBagRelationships(Optional<Role> role);

    List<Role> fetchBagRelationships(List<Role> roles);

    Page<Role> fetchBagRelationships(Page<Role> roles);
}
