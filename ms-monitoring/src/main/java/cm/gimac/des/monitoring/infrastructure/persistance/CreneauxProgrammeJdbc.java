package cm.gimac.des.monitoring.infrastructure.persistance;

import cm.gimac.des.monitoring.domain.exploitation.JsonCanonique;
import cm.gimac.des.monitoring.domain.exploitation.ModeleCreneaux;
import cm.gimac.des.monitoring.domain.exploitation.ModeleCreneaux.Creneau;
import cm.gimac.des.monitoring.domain.exploitation.ModeleCreneaux.Portee;
import java.sql.Timestamp;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Persistance du programme configurable (D-117, changeset 20261021090000) : versions du modèle des créneaux (JSON
 * canonique), version figée par grille, créneau et instants de chaque affectation et de chaque vacation. Compléments des
 * entités JHipster OnCallAssignment et Shift, laissées telles que générées.
 */
@Repository
public class CreneauxProgrammeJdbc {

    /** Version du modèle telle qu'enregistrée. */
    public record Version(int version, ModeleCreneaux modele, Instant creeLe, UUID creePar, String motif) {}

    /** Créneau d'une affectation. */
    public record CreneauAffecte(String code, Instant debut, Instant fin, boolean semaine) {}

    private final JdbcTemplate jdbc;
    private final NamedParameterJdbcTemplate nomme;

    public CreneauxProgrammeJdbc(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
        this.nomme = new NamedParameterJdbcTemplate(jdbc);
    }

    // ------------------------------------------------------------------ modèle

    public Optional<Version> courante() {
        return jdbc
            .query("select version, model, created_at, created_by, reason from roster_slot_model order by version desc limit 1", (rs, n) -> version(rs))
            .stream()
            .findFirst();
    }

    public Optional<ModeleCreneaux> version(int version) {
        return jdbc.query("select model from roster_slot_model where version = ?", (rs, n) -> decoder(rs.getString(1)), version).stream().findFirst();
    }

    /** Enregistre une nouvelle version et rend son numéro. */
    public int enregistrer(ModeleCreneaux m, UUID auteur, String motif, Instant le) {
        Integer max = jdbc.queryForObject("select coalesce(max(version), 0) from roster_slot_model", Integer.class);
        int v = (max == null ? 0 : max) + 1;
        jdbc.update("insert into roster_slot_model (version, model, created_at, created_by, reason) values (?, ?, ?, ?, ?)", v, encoder(m), Timestamp.from(le), auteur, motif);
        return v;
    }

    private Version version(java.sql.ResultSet rs) throws java.sql.SQLException {
        Timestamp t = rs.getTimestamp(3);
        return new Version(rs.getInt(1), decoder(rs.getString(2)), t == null ? null : t.toInstant(), rs.getObject(4, UUID.class), rs.getString(5));
    }

    // ------------------------------------------------------------------ grilles et affectations

    public void figer(UUID grille, int version) {
        jdbc.update("insert into roster_slot_snapshot (roster_id, model_version) values (?, ?) on conflict (roster_id) do update set model_version = excluded.model_version", grille, version);
    }

    public Optional<Integer> versionDe(UUID grille) {
        return jdbc.query("select model_version from roster_slot_snapshot where roster_id = ?", (rs, n) -> rs.getInt(1), grille).stream().findFirst();
    }

    public void affecter(UUID affectation, String code, Instant debut, Instant fin, boolean semaine) {
        jdbc.update(
            "insert into roster_slot_assignment (assignment_id, slot_code, starts_at, ends_at, whole_week) values (?, ?, ?, ?, ?)",
            affectation,
            code,
            Timestamp.from(debut),
            Timestamp.from(fin),
            semaine
        );
    }

    /** Créneaux des affectations demandées ; une affectation absente relève du modèle historique. */
    public Map<UUID, CreneauAffecte> creneaux(Collection<UUID> affectations) {
        Map<UUID, CreneauAffecte> m = new HashMap<>();
        if (affectations.isEmpty()) {
            return m;
        }
        nomme.query(
            "select assignment_id, slot_code, starts_at, ends_at, whole_week from roster_slot_assignment where assignment_id in (:ids)",
            new MapSqlParameterSource("ids", affectations),
            rs -> {
                m.put(rs.getObject(1, UUID.class), new CreneauAffecte(rs.getString(2), rs.getTimestamp(3).toInstant(), rs.getTimestamp(4).toInstant(), rs.getBoolean(5)));
            }
        );
        return m;
    }

    public void creneauVacation(UUID vacation, String code) {
        jdbc.update("insert into shift_slot (shift_id, slot_code) values (?, ?) on conflict (shift_id) do update set slot_code = excluded.slot_code", vacation, code);
    }

    public Optional<String> creneauVacation(UUID vacation) {
        return jdbc.query("select slot_code from shift_slot where shift_id = ?", (rs, n) -> rs.getString(1), vacation).stream().findFirst();
    }

    // ------------------------------------------------------------------ codec

    static String encoder(ModeleCreneaux m) {
        List<Map<String, Object>> l = new ArrayList<>();
        for (Creneau c : m.creneaux()) {
            Map<String, Object> x = new LinkedHashMap<>();
            x.put("code", c.code());
            x.put("libelles", new LinkedHashMap<>(c.libelles()));
            x.put("jours", c.jours().stream().sorted().map(DayOfWeek::getValue).toList());
            x.put("debut", c.debut().toString());
            x.put("fin", c.fin().toString());
            x.put("portee", c.portee().name());
            x.put("niveau", c.niveau());
            x.put("effectifMin", c.effectifMin());
            x.put("effectifMax", c.effectifMax());
            x.put("permissions", c.permissions());
            x.put("eligibles", c.eligibles().stream().map(UUID::toString).toList());
            x.put("chefDeQuart", c.chefDeQuart());
            x.put("rapportQuotidien", c.rapportQuotidien());
            x.put("heuresComptees", c.heuresComptees());
            x.put("ordre", c.ordre());
            x.put("actif", c.actif());
            l.add(x);
        }
        return JsonCanonique.ecrire(Map.of("creneaux", l));
    }

    @SuppressWarnings("unchecked")
    static ModeleCreneaux decoder(String json) {
        List<Map<String, Object>> l = (List<Map<String, Object>>) JsonCanonique.lire(json).getOrDefault("creneaux", List.of());
        List<Creneau> creneaux = new ArrayList<>();
        for (Map<String, Object> x : l) {
            Set<DayOfWeek> jours = EnumSet.noneOf(DayOfWeek.class);
            ((List<Number>) x.getOrDefault("jours", List.of())).forEach(j -> jours.add(DayOfWeek.of(j.intValue())));
            Object heures = x.get("heuresComptees");
            creneaux.add(
                new Creneau(
                    (String) x.get("code"),
                    (Map<String, String>) x.getOrDefault("libelles", Map.of()),
                    jours,
                    LocalTime.parse((String) x.get("debut")),
                    LocalTime.parse((String) x.get("fin")),
                    Portee.valueOf((String) x.get("portee")),
                    ((Number) x.get("niveau")).intValue(),
                    ((Number) x.get("effectifMin")).intValue(),
                    ((Number) x.get("effectifMax")).intValue(),
                    (List<String>) x.getOrDefault("permissions", List.of()),
                    ((List<String>) x.getOrDefault("eligibles", List.of())).stream().map(UUID::fromString).toList(),
                    Boolean.TRUE.equals(x.get("chefDeQuart")),
                    Boolean.TRUE.equals(x.get("rapportQuotidien")),
                    heures == null ? null : ((Number) heures).doubleValue(),
                    ((Number) x.getOrDefault("ordre", 0)).intValue(),
                    !Boolean.FALSE.equals(x.get("actif"))
                )
            );
        }
        return new ModeleCreneaux(creneaux);
    }
}
