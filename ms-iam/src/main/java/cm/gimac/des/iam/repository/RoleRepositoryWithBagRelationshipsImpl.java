package cm.gimac.des.iam.repository;

import cm.gimac.des.iam.domain.Role;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

/**
 * Utility repository to load bag relationships based on https://vladmihalcea.com/hibernate-multiplebagfetchexception/
 */
public class RoleRepositoryWithBagRelationshipsImpl implements RoleRepositoryWithBagRelationships {

    private static final String ID_PARAMETER = "id";
    private static final String ROLES_PARAMETER = "roles";

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Optional<Role> fetchBagRelationships(Optional<Role> role) {
        return role.map(this::fetchPermissionses);
    }

    @Override
    public Page<Role> fetchBagRelationships(Page<Role> roles) {
        return new PageImpl<>(fetchBagRelationships(roles.getContent()), roles.getPageable(), roles.getTotalElements());
    }

    @Override
    public List<Role> fetchBagRelationships(List<Role> roles) {
        return Optional.of(roles).map(this::fetchPermissionses).orElse(List.of());
    }

    Role fetchPermissionses(Role result) {
        return entityManager
            .createQuery("select role from Role role left join fetch role.permissionses where role.id = :id", Role.class)
            .setParameter(ID_PARAMETER, result.getId())
            .getSingleResult();
    }

    List<Role> fetchPermissionses(List<Role> roles) {
        HashMap<Object, Integer> order = new HashMap<>();
        IntStream.range(0, roles.size()).forEach(index -> order.put(roles.get(index).getId(), index));
        List<Role> result = entityManager
            .createQuery("select role from Role role left join fetch role.permissionses where role in :roles", Role.class)
            .setParameter(ROLES_PARAMETER, roles)
            .getResultList();
        result.sort((o1, o2) -> Integer.compare(order.get(o1.getId()), order.get(o2.getId())));
        return result;
    }
}
