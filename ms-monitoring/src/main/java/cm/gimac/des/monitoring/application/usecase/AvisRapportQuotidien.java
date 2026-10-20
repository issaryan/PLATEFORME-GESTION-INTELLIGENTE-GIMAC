package cm.gimac.des.monitoring.application.usecase;

import cm.gimac.des.monitoring.application.port.in.Appelant;
import cm.gimac.des.monitoring.application.port.out.AnnuairePort;
import cm.gimac.des.monitoring.infrastructure.messaging.outbox.OutboxPublisher;
import cm.gimac.des.monitoring.infrastructure.persistance.RapportMonitoring;
import cm.gimac.des.monitoring.infrastructure.persistance.RapportsMonitoringRepository;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Rappel et signalement du rapport quotidien de 8 h (D-116 §10.2, D-117 §11.4) : à partir de l'heure de rappel des
 * contraintes (7 h 30), les personnes programmées sur le créneau qui porte le rapport sont rappelées une fois s'il n'est pas déposé ; après
 * l'échéance, les porteurs de MON_ROSTER_MANAGE sont prévenus une fois. Table daily_report_notice.
 */
@Service
public class AvisRapportQuotidien {

    private final RapportsMonitoringRepository rapports;
    private final RapportsMonitoringService suivi;
    private final AnnuairePort annuaire;
    private final Quarts quarts;
    private final OutboxPublisher outbox;
    private final JdbcTemplate jdbc;
    private final ContraintesGrille contraintes;

    public AvisRapportQuotidien(
        RapportsMonitoringRepository rapports,
        RapportsMonitoringService suivi,
        AnnuairePort annuaire,
        Quarts quarts,
        OutboxPublisher outbox,
        JdbcTemplate jdbc,
        ContraintesGrille contraintes
    ) {
        this.rapports = rapports;
        this.suivi = suivi;
        this.annuaire = annuaire;
        this.quarts = quarts;
        this.outbox = outbox;
        this.jdbc = jdbc;
        this.contraintes = contraintes;
    }

    /** Passage de l'ordonnanceur : rend les avis émis (RAPPEL, RETARD), pour les journaux et les tests. */
    @Transactional
    public List<String> verifier() {
        Instant maintenant = quarts.maintenant();
        LocalDate aujourdhui = LocalDate.ofInstant(maintenant, quarts.reglages().fuseau());
        LocalDate jour = aujourdhui.minusDays(1);
        List<String> emis = new ArrayList<>();
        if (rapports.findFirstByNatureAndDate(RapportMonitoring.Nature.QUOTIDIEN, jour).isPresent()) {
            return emis;
        }
        List<String> equipe = suivi.equipe(jour);
        Instant echeance = suivi.echeance(jour);
        Instant heureRappel = aujourdhui.atTime(contraintes.lire().rappelRapport()).atZone(quarts.reglages().fuseau()).toInstant();
        jdbc.update("insert into daily_report_notice (report_date) values (?) on conflict do nothing", Date.valueOf(jour));
        if (!equipe.isEmpty() && !maintenant.isBefore(heureRappel) && maintenant.isBefore(echeance)) {
            int n = jdbc.update("update daily_report_notice set reminder_sent_at = ? where report_date = ? and reminder_sent_at is null", Timestamp.from(maintenant), Date.valueOf(jour));
            if (n == 1) {
                avis(new ArrayList<>(equipe), "URGENT", "RAPPORT_QUOTIDIEN_ATTENDU", jour);
                emis.add("RAPPEL");
            }
        }
        if (!maintenant.isBefore(echeance)) {
            int n = jdbc.update("update daily_report_notice set late_notified_at = ? where report_date = ? and late_notified_at is null", Timestamp.from(maintenant), Date.valueOf(jour));
            if (n == 1) {
                List<String> chefs = annuaire.porteurs(Appelant.GERER_GRILLE).stream().map(AnnuairePort.Personne::id).toList();
                avis(chefs, "AVERTISSEMENT", equipe.isEmpty() ? "RAPPORT_QUOTIDIEN_SANS_VACATION" : "RAPPORT_QUOTIDIEN_EN_RETARD", jour);
                emis.add("RETARD");
            }
        }
        return emis;
    }

    private void avis(List<String> destinataires, String niveau, String objet, LocalDate jour) {
        if (destinataires.isEmpty()) {
            return;
        }
        Map<String, Object> n = new LinkedHashMap<>();
        n.put("destinataires", destinataires);
        n.put("niveau", niveau);
        n.put("objet", objet);
        n.put("objetId", jour.toString());
        n.put("reference", jour.toString());
        n.put("le", quarts.maintenant().toString());
        n.put("lien", "/des/exploitation/rapports?quotidien=" + jour);
        outbox.publish("DailyReport", java.util.UUID.nameUUIDFromBytes(("quotidien-" + jour).getBytes()), "monitoring.notification-requested.v1", n);
    }
}
