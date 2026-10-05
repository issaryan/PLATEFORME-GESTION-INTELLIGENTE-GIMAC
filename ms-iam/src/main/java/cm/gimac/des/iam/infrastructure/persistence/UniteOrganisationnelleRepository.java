package cm.gimac.des.iam.infrastructure.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Unités organisationnelles du GIMAC. */
public interface UniteOrganisationnelleRepository extends JpaRepository<UniteOrganisationnelle, String> {
    List<UniteOrganisationnelle> findAllByOrderByCodeAsc();
}
