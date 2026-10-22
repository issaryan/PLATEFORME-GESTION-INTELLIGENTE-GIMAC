package cm.gimac.des.participant.infrastructure.persistence;

import cm.gimac.des.participant.application.port.out.VersionsStatutRegistre;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Versions de statut dans la table institution_version_statut (Liquibase, D-120). L'incrément est un seul ordre
 * PostgreSQL (insert … on conflict … do update … returning) : la ligne reste verrouillée jusqu'à la fin de la
 * transaction du changement de statut, ce qui rend la suite strictement croissante même entre deux transactions
 * concurrentes sur la même institution.
 */
@Component
public class VersionsStatutRegistreJdbc implements VersionsStatutRegistre {

    private final JdbcClient jdbc;

    public VersionsStatutRegistreJdbc(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public long suivante(UUID institutionId) {
        return jdbc
            .sql(
                "insert into institution_version_statut (institution_id, version) values (:id, 1) " +
                "on conflict (institution_id) do update set version = institution_version_statut.version + 1 returning version"
            )
            .param("id", institutionId)
            .query(Long.class)
            .single();
    }

    @Override
    public long courante(UUID institutionId) {
        return jdbc
            .sql("select version from institution_version_statut where institution_id = :id")
            .param("id", institutionId)
            .query(Long.class)
            .optional()
            .orElse(0L);
    }
}
