package cm.gimac.des.iam.infrastructure.persistence.sandbox;

import cm.gimac.des.iam.application.port.out.sandbox.ProtectionsAdministrateur;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Adaptateur JDBC de l'attribut de protection du dernier administrateur (D-119, table protection_administrateur). */
@Component
public class ProtectionsAdministrateurJdbc implements ProtectionsAdministrateur {

    private final JdbcTemplate jdbc;

    public ProtectionsAdministrateurJdbc(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean protege(String compteId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("select exists (select 1 from protection_administrateur where compte_id = ?)", Boolean.class, compteId));
    }

    @Override
    public boolean definir(String compteId, String institutionId, boolean protege, String acteurId, Instant le) {
        if (!protege) {
            return jdbc.update("delete from protection_administrateur where compte_id = ?", compteId) == 1;
        }
        return jdbc.update(
            "insert into protection_administrateur (compte_id, institution_id, protege_le, acteur_id) values (?, ?, ?, ?) on conflict (compte_id) do nothing",
            compteId,
            institutionId,
            Timestamp.from(le),
            acteurId
        ) == 1;
    }

    @Override
    public List<Protection> lister(int page, int taille) {
        return jdbc.query(
            "select * from protection_administrateur order by institution_id, compte_id limit ? offset ?",
            (rs, i) -> new Protection(rs.getString("compte_id"), rs.getString("institution_id"), rs.getTimestamp("protege_le").toInstant(), rs.getString("acteur_id")),
            taille,
            (long) page * taille
        );
    }

    @Override
    public long compter() {
        return java.util.Objects.requireNonNullElse(jdbc.queryForObject("select count(*) from protection_administrateur", Long.class), 0L);
    }
}
