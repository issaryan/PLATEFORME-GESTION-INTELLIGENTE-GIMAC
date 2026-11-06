package cm.gimac.des.pilotage.repository;

import cm.gimac.des.pilotage.domain.processus.EtatProcessus;
import cm.gimac.des.pilotage.domain.processus.Processus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Versions des processus d'unité (D-87). */
public interface ProcessusRepository extends JpaRepository<Processus, UUID> {
    /** Versions des unités données (toutes si {@code toutes}), filtrées par état et texte (code, libellé, catégorie). */
    @Query(
        "select p from Processus p where (:toutes = true or p.unite in :unites) and (:etat is null or p.etat = :etat) " +
        "and (:q is null or lower(p.code) like :q or lower(p.libelle) like :q or lower(coalesce(p.categorie, '')) like :q) " +
        "order by p.unite, p.code, p.version desc"
    )
    List<Processus> visibles(@Param("toutes") boolean toutes, @Param("unites") Collection<String> unites, @Param("etat") EtatProcessus etat, @Param("q") String q);

    List<Processus> findByCodeOrderByVersionDesc(String code);

    boolean existsByCode(String code);

    Optional<Processus> findFirstByCodeAndEtat(String code, EtatProcessus etat);

    @Query("select coalesce(max(p.version), 0) from Processus p where p.code = :code")
    int derniereVersion(@Param("code") String code);

    /** Numéro d'ordre du prochain code de processus généré (séquence, sans collision en concurrence). */
    @Query(value = "select nextval('processus_code_seq')", nativeQuery = true)
    long prochainNumeroCode();
}
