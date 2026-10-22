package cm.gimac.des.iam.infrastructure.persistence;

import cm.gimac.des.iam.application.port.out.EtapesIdempotentes;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Adaptateur JDBC des étapes de saga déjà appliquées (D-115, table etape_idempotente). */
@Component
public class EtapesIdempotentesJdbc implements EtapesIdempotentes {

    private final JdbcTemplate jdbc;
    private final Clock clock;

    public EtapesIdempotentesJdbc(JdbcTemplate jdbc, Clock clock) {
        this.jdbc = jdbc;
        this.clock = clock;
    }

    @Override
    public Optional<String> resultat(String cle, String operation) {
        return jdbc.queryForList("select resultat from etape_idempotente where cle = ? and operation = ?", String.class, cle, operation).stream().findFirst();
    }

    @Override
    public void enregistrer(String cle, String operation, String resultat, String acteur) {
        jdbc.update(
            "insert into etape_idempotente (cle, operation, resultat, acteur_id, cree_le) values (?, ?, ?, ?, ?)",
            cle,
            operation,
            resultat,
            acteur,
            Timestamp.from(clock.instant())
        );
    }

    /**
     * Insertion sans conflit (D-119) : la clé primaire (cle, operation) sérialise deux reprises concurrentes ; la
     * seconde attend la validation de la première, n'insère rien et lit son résultat.
     */
    @Override
    public Optional<String> reserver(String cle, String operation, String resultat, String acteur) {
        int lignes = jdbc.update(
            "insert into etape_idempotente (cle, operation, resultat, acteur_id, cree_le) values (?, ?, ?, ?, ?) on conflict (cle, operation) do nothing",
            cle,
            operation,
            resultat,
            acteur,
            Timestamp.from(clock.instant())
        );
        return lignes == 1 ? Optional.empty() : resultat(cle, operation);
    }
}
