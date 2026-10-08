package cm.gimac.des.homologation.infrastructure.persistence;

import cm.gimac.des.homologation.domain.JsonTexte;
import cm.gimac.des.homologation.domain.parcours.CatalogueFonctions;
import jakarta.annotation.PostConstruct;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Branche le catalogue des fonctions du domaine (D-91) sur la table {@code referentiel_fonction}. La lecture passe
 * par JDBC, dans la connexion de la transaction en cours le cas échéant, sans vidage du contexte JPA.
 */
@Component
public class SourceCatalogueFonctions {

    static final String LECTURE = "select code, libelle, profils::text as profils, externe, actif, permissions::text as permissions from referentiel_fonction";

    private final JdbcTemplate jdbc;

    public SourceCatalogueFonctions(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @PostConstruct
    void brancher() {
        CatalogueFonctions.brancher(this::lire);
    }

    List<CatalogueFonctions.Definition> lire() {
        return jdbc.query(LECTURE, (rs, i) ->
            new CatalogueFonctions.Definition(
                rs.getString("code"),
                rs.getString("libelle"),
                JsonTexte.listeDeTextes(rs.getString("profils")),
                rs.getBoolean("externe"),
                rs.getBoolean("actif"),
                JsonTexte.listeDeTextes(rs.getString("permissions"))
            )
        );
    }
}
