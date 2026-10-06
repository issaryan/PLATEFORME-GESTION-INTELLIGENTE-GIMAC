package cm.gimac.des.iam.infrastructure.persistence;

import cm.gimac.des.iam.application.port.out.ProfilsCrees;
import cm.gimac.des.iam.domain.compte.ProfilCree;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Adaptateur JDBC des profils créés par l'administration (D-81, table profil_cree). */
@Component
public class ProfilsCreesJdbc implements ProfilsCrees {

    private final JdbcTemplate jdbc;

    public ProfilsCreesJdbc(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static ProfilCree ligne(ResultSet rs, int n) throws SQLException {
        return new ProfilCree(rs.getString("code"), rs.getString("description"), rs.getBoolean("privilegie"), rs.getString("cree_par"), rs.getTimestamp("cree_le").toInstant());
    }

    @Override
    public List<ProfilCree> tous() {
        return jdbc.query("select code, description, privilegie, cree_par, cree_le from profil_cree order by code", ProfilsCreesJdbc::ligne);
    }

    @Override
    public Optional<ProfilCree> lire(String code) {
        return jdbc.query("select code, description, privilegie, cree_par, cree_le from profil_cree where code = ?", ProfilsCreesJdbc::ligne, code).stream().findFirst();
    }

    @Override
    public void enregistrer(ProfilCree p) {
        int n = jdbc.update("update profil_cree set description = ?, privilegie = ? where code = ?", p.description(), p.privilegie(), p.code());
        if (n == 0) {
            jdbc.update(
                "insert into profil_cree (code, description, privilegie, cree_par, cree_le) values (?, ?, ?, ?, ?)",
                p.code(),
                p.description(),
                p.privilegie(),
                p.creePar(),
                Timestamp.from(p.creeLe())
            );
        }
    }

    @Override
    public void supprimer(String code) {
        jdbc.update("delete from profil_cree where code = ?", code);
    }
}
