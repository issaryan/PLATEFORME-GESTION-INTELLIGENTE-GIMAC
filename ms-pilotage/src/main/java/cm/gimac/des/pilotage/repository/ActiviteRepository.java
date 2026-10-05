package cm.gimac.des.pilotage.repository;

import cm.gimac.des.pilotage.domain.activite.Activite;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Activités du jour (Mon activité, §2.2). */
public interface ActiviteRepository extends JpaRepository<Activite, UUID> {
    List<Activite> findByAuteurIdAndJourBetweenOrderByJourAscOrdreAscCreeLeAsc(String auteurId, LocalDate du, LocalDate au);

    @Query("select coalesce(max(a.ordre), 0) from Activite a where a.auteurId = :auteur and a.jour = :jour")
    int dernierOrdre(@Param("auteur") String auteurId, @Param("jour") LocalDate jour);

    /** Nombre d'activités du jour par auteur et par statut : [auteurId, statut, nombre]. */
    @Query("select a.auteurId, a.statut, count(a) from Activite a where a.auteurId in :auteurs and a.jour = :jour group by a.auteurId, a.statut")
    List<Object[]> comptesDuJour(@Param("auteurs") Collection<String> auteurs, @Param("jour") LocalDate jour);
}
