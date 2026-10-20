package cm.gimac.des.monitoring.application.usecase;

import cm.gimac.des.monitoring.application.port.in.Appelant;
import cm.gimac.des.monitoring.application.port.out.AnnuairePort;
import cm.gimac.des.monitoring.domain.Shift;
import cm.gimac.des.monitoring.domain.enumeration.ShiftSlot;
import cm.gimac.des.monitoring.infrastructure.messaging.outbox.OutboxPublisher;
import cm.gimac.des.monitoring.infrastructure.persistance.RapportMonitoring;
import cm.gimac.des.monitoring.infrastructure.persistance.RapportsMonitoringRepository;
import cm.gimac.des.monitoring.infrastructure.persistance.VacationsRepository;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Rappel et signalement du rapport quotidien de 8 h (D-116, docs/lot1/astreintes-monitoring.md §10.2) : à partir de
 * l'heure de rappel (7 h 30), les membres de la vacation de nuit sont rappelés une fois s'il n'est pas déposé ; après
 * l'échéance, les porteurs de MON_ROSTER_MANAGE sont prévenus une fois. Table daily_report_notice.
 */
@Service
public class AvisRapportQuotidien {

    private final RapportsMonitoringRepository rapports;
    private final VacationsRepository vacations;
    private final RapportsMonitoringService suivi;
    private final AnnuairePort annuaire;
    private final Quarts quarts;
    private final OutboxPublisher outbox;
    private final JdbcTemplate jdbc;
    private final LocalTime rappel;

    public AvisRapportQuotidien(
        RapportsMonitoringRepository rapports,
        VacationsRepository vacations,
        RapportsMonitoringService suivi,
        AnnuairePort annuaire,
        Quarts quarts,
        OutboxPublisher outbox,
        JdbcTemplate jdbc,
        @Value("${gimac.monitoring.rapport-quotidien.rappel:07:30}") LocalTime rappel
    ) {
        this.rapports = rapports;
        this.vacations = vacations;
        this.suivi = suivi;
        this.annuaire = annuaire;
        this.quarts = quarts;
        this.outbox = outbox;
        this.jdbc = jdbc;
        this.rappel = rappel;
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
        Shift nuit = vacations.findFirstByShiftDateAndSlot(jour, ShiftSlot.NUIT).orElse(null);
        Instant echeance = suivi.echeance(jour);
        Instant heureRappel = aujourdhui.atTime(rappel).atZone(quarts.reglages().fuseau()).toInstant();
        jdbc.update("insert into daily_report_notice (report_date) values (?) on conflict do nothing", Date.valueOf(jour));
        if (nuit != null && !maintenant.isBefore(heureRappel) && maintenant.isBefore(echeance)) {
            int n = jdbc.update("update daily_report_notice set reminder_sent_at = ? where report_date = ? and reminder_sent_at is null", Timestamp.from(maintenant), Date.valueOf(jour));
            if (n == 1) {
                avis(new ArrayList<>(Quarts.membres(nuit)), "URGENT", "RAPPORT_QUOTIDIEN_ATTENDU", jour);
                emis.add("RAPPEL");
            }
        }
        if (!maintenant.isBefore(echeance)) {
            int n = jdbc.update("update daily_report_notice set late_notified_at = ? where report_date = ? and late_notified_at is null", Timestamp.from(maintenant), Date.valueOf(jour));
            if (n == 1) {
                List<String> chefs = annuaire.porteurs(Appelant.GERER_GRILLE).stream().map(AnnuairePort.Personne::id).toList();
                avis(chefs, "AVERTISSEMENT", nuit == null ? "RAPPORT_QUOTIDIEN_SANS_VACATION" : "RAPPORT_QUOTIDIEN_EN_RETARD", jour);
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
