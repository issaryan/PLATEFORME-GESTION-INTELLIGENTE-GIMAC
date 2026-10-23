package cm.gimac.des.iam.infrastructure.persistence.sandbox;

import cm.gimac.des.iam.application.port.out.sandbox.PerimetresSuspendus;
import cm.gimac.des.iam.domain.sandbox.PerimetreSuspendu;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Adaptateur JDBC des périmètres suspendus (D-119, table perimetre_suspendu). */
@Component
public class PerimetresSuspendusJdbc implements PerimetresSuspendus {

    private final JdbcTemplate jdbc;

    public PerimetresSuspendusJdbc(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static PerimetreSuspendu ligne(ResultSet rs, int i) throws SQLException {
        return new PerimetreSuspendu(rs.getString("code"), rs.getTimestamp("suspendu_le").toInstant(), rs.getString("acteur_id"), rs.getString("motif"));
    }

    @Override
    public Optional<PerimetreSuspendu> lire(String code) {
        return jdbc.query("select * from perimetre_suspendu where code = ?", PerimetresSuspendusJdbc::ligne, code).stream().findFirst();
    }

    @Override
    public boolean poser(String code, String acteurId, String motif, Instant le) {
        return jdbc.update(
            "insert into perimetre_suspendu (code, suspendu_le, acteur_id, motif) values (?, ?, ?, ?) on conflict (code) do nothing",
            code,
            Timestamp.from(le),
            acteurId,
            motif
        ) == 1;
    }

    @Override
    public boolean lever(String code) {
        return jdbc.update("delete from perimetre_suspendu where code = ?", code) == 1;
    }

    @Override
    public List<PerimetreSuspendu> lister(int page, int taille) {
        return jdbc.query("select * from perimetre_suspendu order by suspendu_le desc, code limit ? offset ?", PerimetresSuspendusJdbc::ligne, taille, (long) page * taille);
    }

    @Override
    public long compter() {
        return java.util.Objects.requireNonNullElse(jdbc.queryForObject("select count(*) from perimetre_suspendu", Long.class), 0L);
    }
}
