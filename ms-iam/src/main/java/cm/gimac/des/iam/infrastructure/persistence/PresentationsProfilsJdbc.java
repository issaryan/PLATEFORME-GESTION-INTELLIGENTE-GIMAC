package cm.gimac.des.iam.infrastructure.persistence;

import cm.gimac.des.iam.application.port.out.PresentationsProfils;
import cm.gimac.des.iam.domain.compte.PresentationProfil;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Adaptateur JDBC des modifications de profils par l'administration (D-86, table profil_presentation) ; attribut
 * externe fixé depuis la console (D-94, colonne externe, nulle s'il n'a jamais été modifié).
 */
@Component
public class PresentationsProfilsJdbc implements PresentationsProfils {

    private static final String COLONNES = "select code, libelle, description, modifie_le, modifie_par_id, modifie_par_nom from profil_presentation";

    private final JdbcTemplate jdbc;

    public PresentationsProfilsJdbc(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static PresentationProfil ligne(ResultSet rs, int n) throws SQLException {
        Timestamp le = rs.getTimestamp("modifie_le");
        return new PresentationProfil(
            rs.getString("code"),
            rs.getString("libelle"),
            rs.getString("description"),
            le == null ? null : le.toInstant(),
            rs.getString("modifie_par_id"),
            rs.getString("modifie_par_nom")
        );
    }

    @Override
    public Map<String, PresentationProfil> toutes() {
        Map<String, PresentationProfil> m = new TreeMap<>();
        jdbc.query(COLONNES, PresentationsProfilsJdbc::ligne).forEach(p -> m.put(p.code(), p));
        return m;
    }

    @Override
    public Optional<PresentationProfil> lire(String code) {
        return jdbc.query(COLONNES + " where code = ?", PresentationsProfilsJdbc::ligne, code).stream().findFirst();
    }

    @Override
    public void fixer(String code, String libelle, String description, Instant le, String parId, String parNom) {
        int n = jdbc.update(
            "update profil_presentation set libelle = ?, description = ?, modifie_le = ?, modifie_par_id = ?, modifie_par_nom = ? where code = ?",
            libelle, description, Timestamp.from(le), parId, parNom, code
        );
        if (n == 0) {
            jdbc.update(
                "insert into profil_presentation (code, libelle, description, modifie_le, modifie_par_id, modifie_par_nom) values (?, ?, ?, ?, ?, ?)",
                code, libelle, description, Timestamp.from(le), parId, parNom
            );
        }
    }

    @Override
    public void toucher(String code, Instant le, String parId, String parNom) {
        int n = jdbc.update("update profil_presentation set modifie_le = ?, modifie_par_id = ?, modifie_par_nom = ? where code = ?", Timestamp.from(le), parId, parNom, code);
        if (n == 0) {
            jdbc.update("insert into profil_presentation (code, modifie_le, modifie_par_id, modifie_par_nom) values (?, ?, ?, ?)", code, Timestamp.from(le), parId, parNom);
        }
    }

    @Override
    public Map<String, Boolean> externes() {
        Map<String, Boolean> m = new TreeMap<>();
        jdbc.query("select code, externe from profil_presentation where externe is not null", rs -> {
            m.put(rs.getString("code"), rs.getBoolean("externe"));
        });
        return m;
    }

    @Override
    public void fixerExterne(String code, boolean externe, Instant le, String parId, String parNom) {
        int n = jdbc.update(
            "update profil_presentation set externe = ?, modifie_le = ?, modifie_par_id = ?, modifie_par_nom = ? where code = ?",
            externe, Timestamp.from(le), parId, parNom, code
        );
        if (n == 0) {
            jdbc.update(
                "insert into profil_presentation (code, externe, modifie_le, modifie_par_id, modifie_par_nom) values (?, ?, ?, ?, ?)",
                code, externe, Timestamp.from(le), parId, parNom
            );
        }
    }

    @Override
    public void supprimer(String code) {
        jdbc.update("delete from profil_presentation where code = ?", code);
    }
}
