package cm.gimac.des.pilotage.repository;

import cm.gimac.des.pilotage.domain.activite.EtatRapport;
import cm.gimac.des.pilotage.domain.activite.Rapport;
import cm.gimac.des.pilotage.domain.activite.TypeRapport;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Rapports quotidiens et hebdomadaires (Mon activité, §2.4). */
public interface RapportRepository extends JpaRepository<Rapport, UUID> {
    boolean existsByAuteurIdAndTypeAndPeriode(String auteurId, TypeRapport type, String periode);

    Optional<Rapport> findByAuteurIdAndTypeAndPeriode(String auteurId, TypeRapport type, String periode);

    @Query(value = "select r from Rapport r where r.auteurId = :auteur and (:type is null or r.type = :type) and (:etat is null or r.etat = :etat) order by r.periode desc, r.type",
        countQuery = "select count(r) from Rapport r where r.auteurId = :auteur and (:type is null or r.type = :type) and (:etat is null or r.etat = :etat)")
    Page<Rapport> deAuteur(@Param("auteur") String auteur, @Param("type") TypeRapport type, @Param("etat") EtatRapport etat, Pageable page);

    /** Rapports soumis ou visés reçus comme destinataire, ou écrits par un membre de l'équipe donnée. */
    @Query(value = "select r from Rapport r where (r.destinataireId = :moi or r.auteurId in :equipe) and r.etat in :etats " +
        "and (:type is null or r.type = :type) and (:etat is null or r.etat = :etat) order by r.soumisLe desc",
        countQuery = "select count(r) from Rapport r where (r.destinataireId = :moi or r.auteurId in :equipe) and r.etat in :etats " +
        "and (:type is null or r.type = :type) and (:etat is null or r.etat = :etat)")
    Page<Rapport> recus(
        @Param("moi") String moi,
        @Param("equipe") Collection<String> equipe,
        @Param("etats") Collection<EtatRapport> etats,
        @Param("type") TypeRapport type,
        @Param("etat") EtatRapport etat,
        Pageable page
    );

    /** [auteurId, etat] des rapports d'un type et d'une période, pour les auteurs donnés. */
    @Query("select r.auteurId, r.etat from Rapport r where r.auteurId in :auteurs and r.type = :type and r.periode = :periode")
    List<Object[]> etats(@Param("auteurs") Collection<String> auteurs, @Param("type") TypeRapport type, @Param("periode") String periode);

    long countByDestinataireIdAndEtat(String destinataireId, EtatRapport etat);
}
