package cm.gimac.des.iam.infrastructure.persistence;

import cm.gimac.des.iam.application.port.out.CatalogueFonctionnalites;
import cm.gimac.des.iam.domain.fonctionnalite.Fonctionnalite;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * Adaptateur JDBC du catalogue des fonctionnalités (D-94, table fonctionnalite) : la définition (sans trace) est
 * gardée en JSON ; {@code modifie_le} nul distingue une valeur de départ, que le chargement du registre peut
 * remplacer, d'une modification faite depuis la plateforme, jamais écrasée.
 */
@Component
public class CatalogueFonctionnalitesJdbc implements CatalogueFonctionnalites {

    private static final String COLONNES = "select cle, definition, modifie_le, modifie_par_id, modifie_par_nom from fonctionnalite";

    private final JdbcTemplate jdbc;
    private final ObjectMapper json;

    public CatalogueFonctionnalitesJdbc(JdbcTemplate jdbc, ObjectMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    private Fonctionnalite ligne(ResultSet rs, int n) throws SQLException {
        Fonctionnalite f;
        try {
            f = json.readValue(rs.getString("definition"), Fonctionnalite.class);
        } catch (JacksonException e) {
            throw new SQLException("Définition illisible de la fonctionnalité " + rs.getString("cle"), e);
        }
        Timestamp le = rs.getTimestamp("modifie_le");
        return le == null ? f.sansTrace() : f.modifiee(le.toInstant(), rs.getString("modifie_par_id"), rs.getString("modifie_par_nom"));
    }

    private String definition(Fonctionnalite f) {
        try {
            return json.writeValueAsString(f.sansTrace());
        } catch (JacksonException e) {
            throw new IllegalStateException("Fonctionnalité non sérialisable : " + f.cle(), e);
        }
    }

    @Override
    public List<Fonctionnalite> toutes() {
        return jdbc.query(COLONNES + " order by application, ordre, cle", this::ligne);
    }

    @Override
    public Optional<Fonctionnalite> lire(String cle) {
        return jdbc.query(COLONNES + " where cle = ?", this::ligne, cle).stream().findFirst();
    }

    @Override
    public boolean ajouterInitiale(Fonctionnalite f) {
        // Autre instance démarrée en même temps : la valeur de départ déjà là n'est pas touchée.
        return (
            jdbc.update(
                "insert into fonctionnalite (cle, application, ordre, definition) values (?, ?, ?, ?) on conflict (cle) do nothing",
                f.cle(),
                f.application(),
                f.ordre(),
                definition(f)
            ) >
            0
        );
    }

    @Override
    public boolean remplacerInitiale(Fonctionnalite f) {
        return (
            jdbc.update(
                "update fonctionnalite set application = ?, ordre = ?, definition = ? where cle = ? and modifie_le is null",
                f.application(),
                f.ordre(),
                definition(f),
                f.cle()
            ) >
            0
        );
    }

    @Override
    public boolean retirerInitiale(String cle) {
        return jdbc.update("delete from fonctionnalite where cle = ? and modifie_le is null", cle) > 0;
    }

    @Override
    public void modifier(Fonctionnalite f) {
        jdbc.update(
            "update fonctionnalite set application = ?, ordre = ?, definition = ?, modifie_le = ?, modifie_par_id = ?, modifie_par_nom = ? where cle = ?",
            f.application(),
            f.ordre(),
            definition(f),
            f.modifieLe() == null ? null : Timestamp.from(f.modifieLe()),
            f.modifiePar() == null ? null : f.modifiePar().id(),
            f.modifiePar() == null ? null : f.modifiePar().nomAffiche(),
            f.cle()
        );
    }
}
