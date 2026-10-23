package cm.gimac.des.iam.infrastructure.persistence.sandbox;

import cm.gimac.des.iam.application.port.out.sandbox.VerrouTaches;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Verrou consultatif PostgreSQL de transaction (pg_try_advisory_xact_lock), clé dérivée du nom de la tâche (D-119). */
@Component
public class VerrouTachesPostgres implements VerrouTaches {

    private final JdbcTemplate jdbc;

    public VerrouTachesPostgres(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    static long cle(String tache) {
        return UUID.nameUUIDFromBytes(("gimac-iam-tache:" + tache).getBytes(StandardCharsets.UTF_8)).getMostSignificantBits();
    }

    @Override
    public boolean prendre(String tache) {
        return Boolean.TRUE.equals(jdbc.queryForObject("select pg_try_advisory_xact_lock(?)", Boolean.class, cle(tache)));
    }
}
