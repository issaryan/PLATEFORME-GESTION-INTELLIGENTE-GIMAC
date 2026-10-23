package cm.gimac.des.iam.infrastructure.persistence.sandbox;

import cm.gimac.des.iam.application.port.out.sandbox.ActiviteComptes;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Adaptateur JDBC de la dernière connexion des comptes (D-119, table activite_compte). */
@Component
public class ActiviteComptesJdbc implements ActiviteComptes {

    private final JdbcTemplate jdbc;

    public ActiviteComptesJdbc(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void connexion(String compteId, Instant le) {
        jdbc.update(
            "insert into activite_compte (compte_id, derniere_connexion) values (?, ?) on conflict (compte_id) do update set derniere_connexion = greatest(activite_compte.derniere_connexion, excluded.derniere_connexion)",
            compteId,
            Timestamp.from(le)
        );
    }

    @Override
    public Map<String, Instant> dernieres() {
        Map<String, Instant> m = new HashMap<>();
        jdbc.query("select compte_id, derniere_connexion from activite_compte", rs -> {
            m.put(rs.getString(1), rs.getTimestamp(2).toInstant());
        });
        return m;
    }
}
