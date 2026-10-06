package cm.gimac.des.iam.infrastructure.persistence;

import cm.gimac.des.iam.application.port.out.MatricePouvoirs;
import cm.gimac.des.iam.domain.pouvoir.ActionPouvoir;
import cm.gimac.des.iam.domain.pouvoir.Portee;
import cm.gimac.des.iam.domain.pouvoir.PouvoirsProfil;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Adaptateur JDBC de la matrice des pouvoirs (D-81) : table pouvoir_profil (profil, action, portée ; seules
 * les portées autres que AUCUNE sont gardées) et table profil_demandable (profil, profil demandable).
 */
@Component
public class MatricePouvoirsJdbc implements MatricePouvoirs {

    private final JdbcTemplate jdbc;

    public MatricePouvoirsJdbc(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Map<String, PouvoirsProfil> toutes() {
        Map<String, Map<ActionPouvoir, Portee>> actions = new HashMap<>();
        Map<String, Set<String>> demandables = new HashMap<>();
        jdbc.query("select profil, action, portee from pouvoir_profil", rs -> {
            actions.computeIfAbsent(rs.getString(1), k -> new EnumMap<>(ActionPouvoir.class)).put(ActionPouvoir.valueOf(rs.getString(2)), Portee.valueOf(rs.getString(3)));
        });
        jdbc.query("select profil, profil_demandable from profil_demandable", rs -> {
            demandables.computeIfAbsent(rs.getString(1), k -> new HashSet<>()).add(rs.getString(2));
        });
        Set<String> profils = new HashSet<>(actions.keySet());
        profils.addAll(demandables.keySet());
        Map<String, PouvoirsProfil> m = new TreeMap<>();
        profils.forEach(p -> m.put(p, new PouvoirsProfil(p, actions.getOrDefault(p, Map.of()), demandables.getOrDefault(p, Set.of()))));
        return m;
    }

    @Override
    public PouvoirsProfil lire(String profil) {
        Map<ActionPouvoir, Portee> actions = new EnumMap<>(ActionPouvoir.class);
        jdbc.query("select action, portee from pouvoir_profil where profil = ?", rs -> {
            actions.put(ActionPouvoir.valueOf(rs.getString(1)), Portee.valueOf(rs.getString(2)));
        }, profil);
        Set<String> demandables = new HashSet<>(jdbc.queryForList("select profil_demandable from profil_demandable where profil = ?", String.class, profil));
        return new PouvoirsProfil(profil, actions, demandables);
    }

    @Override
    public void enregistrer(PouvoirsProfil ligne) {
        jdbc.update("delete from pouvoir_profil where profil = ?", ligne.profil());
        jdbc.update("delete from profil_demandable where profil = ?", ligne.profil());
        ligne.actions().forEach((a, p) -> {
            if (p != Portee.AUCUNE) {
                jdbc.update("insert into pouvoir_profil (profil, action, portee) values (?, ?, ?)", ligne.profil(), a.name(), p.name());
            }
        });
        ligne.profilsDemandables().forEach(d -> jdbc.update("insert into profil_demandable (profil, profil_demandable) values (?, ?)", ligne.profil(), d));
    }

    @Override
    public void supprimer(String profil) {
        jdbc.update("delete from pouvoir_profil where profil = ?", profil);
        jdbc.update("delete from profil_demandable where profil = ? or profil_demandable = ?", profil, profil);
    }
}
