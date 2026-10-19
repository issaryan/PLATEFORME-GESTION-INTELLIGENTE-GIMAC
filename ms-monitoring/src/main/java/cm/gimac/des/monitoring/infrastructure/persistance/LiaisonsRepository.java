package cm.gimac.des.monitoring.infrastructure.persistance;

import cm.gimac.des.monitoring.domain.SupervisedLink;
import cm.gimac.des.monitoring.domain.enumeration.LinkState;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Liaisons supervisées (MON-01), triées de la plus grave à la plus saine. */
public interface LiaisonsRepository extends JpaRepository<SupervisedLink, UUID> {
    @Query(
        value = "select l from SupervisedLink l left join fetch l.parentPlatform where (:etat is null or l.state = :etat) " +
        "and (:recherche is null or lower(l.code) like :recherche or lower(l.label) like :recherche) " +
        "order by case when l.state = cm.gimac.des.monitoring.domain.enumeration.LinkState.INJOIGNABLE then 0 " +
        "when l.state = cm.gimac.des.monitoring.domain.enumeration.LinkState.CRITIQUE then 1 " +
        "when l.state = cm.gimac.des.monitoring.domain.enumeration.LinkState.DEGRADEE then 2 " +
        "when l.state = cm.gimac.des.monitoring.domain.enumeration.LinkState.MAINTENANCE then 3 else 4 end, l.code",
        countQuery = "select count(l) from SupervisedLink l where (:etat is null or l.state = :etat) " +
        "and (:recherche is null or lower(l.code) like :recherche or lower(l.label) like :recherche)"
    )
    Page<SupervisedLink> rechercher(@Param("etat") LinkState etat, @Param("recherche") String recherche, Pageable page);

    Optional<SupervisedLink> findByCode(String code);

    @Query("select l.state, count(l) from SupervisedLink l group by l.state")
    java.util.List<Object[]> parEtat();
}
