package cm.gimac.des.monitoring.application.usecase;

import cm.gimac.des.monitoring.application.port.in.Appelant;
import cm.gimac.des.monitoring.domain.exception.MonitoringException;
import cm.gimac.des.monitoring.domain.exploitation.ProblemeGrille;
import cm.gimac.des.monitoring.infrastructure.messaging.outbox.OutboxPublisher;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Contraintes générales du programme fixées par le responsable (D-116 §10.3, D-117 §11.2), table roster_constraints :
 * repos minimal, nuits consécutives, plafond hebdomadaire, délai du solveur, échéance et rappel du rapport de 8 h.
 * Effectifs et chef de quart relèvent désormais de chaque créneau du modèle. À défaut de valeur enregistrée, celles de
 * la configuration (gimac.monitoring.grille.*, gimac.monitoring.rapport-quotidien.*).
 */
@Service
public class ContraintesGrille {

    /** Contraintes en vigueur ; {@code modifieLe} et {@code modifiePar} nuls pour les valeurs de la configuration. */
    public record Contraintes(
        int reposHeures,
        int nuitsMax,
        int plafondHeures,
        int limiteSecondes,
        LocalTime echeanceRapport,
        LocalTime rappelRapport,
        Instant modifieLe,
        UUID modifiePar
    ) {
        public ProblemeGrille.Parametres parametres() {
            return new ProblemeGrille.Parametres(reposHeures, nuitsMax, plafondHeures);
        }

        public Duration limite() {
            return Duration.ofSeconds(limiteSecondes);
        }
    }

    private final JdbcTemplate jdbc;
    private final Quarts quarts;
    private final OutboxPublisher outbox;
    private final LocalTime echeance;
    private final LocalTime rappel;

    public ContraintesGrille(
        JdbcTemplate jdbc,
        Quarts quarts,
        OutboxPublisher outbox,
        @Value("${gimac.monitoring.rapport-quotidien.echeance:08:00}") LocalTime echeance,
        @Value("${gimac.monitoring.rapport-quotidien.rappel:07:30}") LocalTime rappel
    ) {
        this.jdbc = jdbc;
        this.quarts = quarts;
        this.outbox = outbox;
        this.echeance = echeance;
        this.rappel = rappel;
    }

    @Transactional(readOnly = true)
    public Contraintes lire() {
        List<Contraintes> l = jdbc.query(
            "select rest_hours, max_nights, weekly_hours, time_limit_seconds, daily_report_deadline, daily_report_reminder, updated_at, updated_by from roster_constraints where id = 1",
            (rs, n) ->
                new Contraintes(
                    rs.getInt(1),
                    rs.getInt(2),
                    rs.getInt(3),
                    rs.getInt(4),
                    rs.getTime(5).toLocalTime(),
                    rs.getTime(6).toLocalTime(),
                    rs.getTimestamp(7).toInstant(),
                    rs.getObject(8, UUID.class)
                )
        );
        if (!l.isEmpty()) {
            return l.get(0);
        }
        Reglages.Grille g = quarts.reglages().grille();
        ProblemeGrille.Parametres p = g.parametres();
        return new Contraintes(p.reposHeures(), p.nuitsMax(), p.plafondHeures(), (int) g.limite().toSeconds(), echeance, rappel, null, null);
    }

    private static int borne(Integer v, String champ, int min, int max) {
        if (v == null || v < min || v > max) {
            throw MonitoringException.invalide(champ, champ + " : de " + min + " à " + max);
        }
        return v;
    }

    private static LocalTime heure(String v, String champ) {
        try {
            return LocalTime.parse(v == null ? "" : v.strip());
        } catch (DateTimeParseException e) {
            throw MonitoringException.invalide(champ, champ + " : heure HH:mm");
        }
    }

    /** Remplace les contraintes ; motif de 3 à 500 caractères ; événement monitoring.constraints-changed.v1. */
    @Transactional
    public Contraintes modifier(
        Appelant appelant,
        Integer reposHeures,
        Integer nuitsMax,
        Integer plafondHeures,
        Integer limiteSecondes,
        String echeanceRapport,
        String rappelRapport,
        String motif
    ) {
        UUID auteur = appelant.uuid().orElseThrow(() -> MonitoringException.interdit("MON_JETON_UTILISATEUR_EXIGE", "Jeton utilisateur exigé"));
        String m = motif == null ? "" : motif.strip();
        if (m.length() < 3 || m.length() > 500) {
            throw MonitoringException.invalide("motif", "Motif de 3 à 500 caractères");
        }
        LocalTime e = heure(echeanceRapport, "echeanceRapport");
        LocalTime r = heure(rappelRapport, "rappelRapport");
        if (!r.isBefore(e)) {
            throw MonitoringException.invalide("rappelRapport", "Le rappel précède l'échéance");
        }
        Contraintes c = new Contraintes(
            borne(reposHeures, "reposHeures", 0, 24),
            borne(nuitsMax, "nuitsMax", 1, 7),
            borne(plafondHeures, "plafondHeures", 8, 84),
            borne(limiteSecondes, "limiteSecondes", 10, 600),
            e,
            r,
            quarts.maintenant(),
            auteur
        );
        int n = jdbc.update(
            "update roster_constraints set rest_hours = ?, max_nights = ?, weekly_hours = ?, time_limit_seconds = ?, daily_report_deadline = ?, daily_report_reminder = ?, updated_at = ?, updated_by = ? where id = 1",
            c.reposHeures(),
            c.nuitsMax(),
            c.plafondHeures(),
            c.limiteSecondes(),
            Time.valueOf(c.echeanceRapport()),
            Time.valueOf(c.rappelRapport()),
            Timestamp.from(c.modifieLe()),
            auteur
        );
        if (n == 0) {
            // Colonnes de D-116 (effectifs, chef de quart) gardées pour l'historique, sans effet depuis D-117.
            jdbc.update(
                "insert into roster_constraints (id, room_staff, on_call_staff, shift_lead, rest_hours, max_nights, weekly_hours, time_limit_seconds, daily_report_deadline, daily_report_reminder, updated_at, updated_by) values (1, 2, 1, false, ?, ?, ?, ?, ?, ?, ?, ?)",
                c.reposHeures(),
                c.nuitsMax(),
                c.plafondHeures(),
                c.limiteSecondes(),
                Time.valueOf(c.echeanceRapport()),
                Time.valueOf(c.rappelRapport()),
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
