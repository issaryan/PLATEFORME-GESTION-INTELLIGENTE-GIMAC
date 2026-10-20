package cm.gimac.des.monitoring.application.usecase;

import cm.gimac.des.monitoring.application.port.in.Appelant;
import cm.gimac.des.monitoring.domain.exception.MonitoringException;
import cm.gimac.des.monitoring.domain.exploitation.ProblemeGrille;
import cm.gimac.des.monitoring.infrastructure.messaging.outbox.OutboxPublisher;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Contraintes de génération des programmes fixées par le responsable (D-116, docs/lot1/astreintes-monitoring.md §10.3),
 * table roster_constraints : effectifs, chef de quart exigé ou non, repos, nuits consécutives, plafond hebdomadaire et
 * délai du solveur. À défaut de valeur enregistrée, celles de la configuration (gimac.monitoring.grille.*).
 */
@Service
public class ContraintesGrille {

    /** Contraintes en vigueur ; {@code modifieLe} et {@code modifiePar} nuls pour les valeurs de la configuration. */
    public record Contraintes(
        int effectifSalle,
        int effectifAstreinte,
        boolean chefDeQuart,
        int reposHeures,
        int nuitsMax,
        int plafondHeures,
        int limiteSecondes,
        Instant modifieLe,
        UUID modifiePar
    ) {
        public ProblemeGrille.Parametres parametres() {
            return new ProblemeGrille.Parametres(effectifSalle, effectifAstreinte, reposHeures, nuitsMax, plafondHeures, chefDeQuart);
        }

        public Duration limite() {
            return Duration.ofSeconds(limiteSecondes);
        }
    }

    private final JdbcTemplate jdbc;
    private final Quarts quarts;
    private final OutboxPublisher outbox;

    public ContraintesGrille(JdbcTemplate jdbc, Quarts quarts, OutboxPublisher outbox) {
        this.jdbc = jdbc;
        this.quarts = quarts;
        this.outbox = outbox;
    }

    @Transactional(readOnly = true)
    public Contraintes lire() {
        List<Contraintes> l = jdbc.query(
            "select room_staff, on_call_staff, shift_lead, rest_hours, max_nights, weekly_hours, time_limit_seconds, updated_at, updated_by from roster_constraints where id = 1",
            (rs, n) ->
                new Contraintes(
                    rs.getInt(1),
                    rs.getInt(2),
                    rs.getBoolean(3),
                    rs.getInt(4),
                    rs.getInt(5),
                    rs.getInt(6),
                    rs.getInt(7),
                    rs.getTimestamp(8).toInstant(),
                    rs.getObject(9, UUID.class)
                )
        );
        if (!l.isEmpty()) {
            return l.get(0);
        }
        Reglages.Grille g = quarts.reglages().grille();
        ProblemeGrille.Parametres p = g.parametres();
        return new Contraintes(p.besoinSalle(), p.besoinAstreinte(), p.chefDeQuart(), p.reposHeures(), p.nuitsMax(), p.plafondHeures(), (int) g.limite().toSeconds(), null, null);
    }

    private static int borne(Integer v, String champ, int min, int max) {
        if (v == null || v < min || v > max) {
            throw MonitoringException.invalide(champ, champ + " : de " + min + " à " + max);
        }
        return v;
    }

    /** Remplace les contraintes ; motif de 3 à 500 caractères ; événement monitoring.constraints-changed.v1. */
    @Transactional
    public Contraintes modifier(
        Appelant appelant,
        Integer effectifSalle,
        Integer effectifAstreinte,
        Boolean chefDeQuart,
        Integer reposHeures,
        Integer nuitsMax,
        Integer plafondHeures,
        Integer limiteSecondes,
        String motif
    ) {
        UUID auteur = appelant.uuid().orElseThrow(() -> MonitoringException.interdit("MON_JETON_UTILISATEUR_EXIGE", "Jeton utilisateur exigé"));
        String m = motif == null ? "" : motif.strip();
        if (m.length() < 3 || m.length() > 500) {
            throw MonitoringException.invalide("motif", "Motif de 3 à 500 caractères");
        }
        if (chefDeQuart == null) {
            throw MonitoringException.invalide("chefDeQuart", "chefDeQuart obligatoire");
        }
        Contraintes c = new Contraintes(
            borne(effectifSalle, "effectifSalle", 1, 10),
            borne(effectifAstreinte, "effectifAstreinte", 0, 5),
            chefDeQuart,
            borne(reposHeures, "reposHeures", 8, 24),
            borne(nuitsMax, "nuitsMax", 1, 7),
            borne(plafondHeures, "plafondHeures", 24, 72),
            borne(limiteSecondes, "limiteSecondes", 10, 600),
            quarts.maintenant(),
            auteur
        );
        int n = jdbc.update(
            "update roster_constraints set room_staff = ?, on_call_staff = ?, shift_lead = ?, rest_hours = ?, max_nights = ?, weekly_hours = ?, time_limit_seconds = ?, updated_at = ?, updated_by = ? where id = 1",
            c.effectifSalle(),
            c.effectifAstreinte(),
            c.chefDeQuart(),
            c.reposHeures(),
            c.nuitsMax(),
            c.plafondHeures(),
            c.limiteSecondes(),
            Timestamp.from(c.modifieLe()),
            auteur
        );
        if (n == 0) {
            jdbc.update(
                "insert into roster_constraints (id, room_staff, on_call_staff, shift_lead, rest_hours, max_nights, weekly_hours, time_limit_seconds, updated_at, updated_by) values (1, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                c.effectifSalle(),
                c.effectifAstreinte(),
                c.chefDeQuart(),
                c.reposHeures(),
                c.nuitsMax(),
                c.plafondHeures(),
                c.limiteSecondes(),
                Timestamp.from(c.modifieLe()),
                auteur
            );
        }
        Map<String, Object> evt = new LinkedHashMap<>();
        evt.put("contraintes", c);
        evt.put("motif", m);
        evt.put("par", auteur);
        outbox.publish("RosterConstraints", UUID.nameUUIDFromBytes("roster-constraints".getBytes()), "monitoring.constraints-changed.v1", evt);
        return c;
    }
}
