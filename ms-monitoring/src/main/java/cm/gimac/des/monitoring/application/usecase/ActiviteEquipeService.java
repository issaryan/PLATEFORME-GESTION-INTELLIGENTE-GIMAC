package cm.gimac.des.monitoring.application.usecase;

import cm.gimac.des.monitoring.application.port.in.Appelant;
import cm.gimac.des.monitoring.application.port.in.Vues.Personne;
import cm.gimac.des.monitoring.domain.OnCallAssignment;
import cm.gimac.des.monitoring.domain.OnCallRoster;
import cm.gimac.des.monitoring.domain.Shift;
import cm.gimac.des.monitoring.domain.enumeration.RosterState;
import cm.gimac.des.monitoring.domain.exception.MonitoringException;
import cm.gimac.des.monitoring.domain.exploitation.Plages;
import cm.gimac.des.monitoring.infrastructure.persistance.AffectationsRepository;
import cm.gimac.des.monitoring.infrastructure.persistance.GrillesRepository;
import cm.gimac.des.monitoring.infrastructure.persistance.RapportMonitoring;
import cm.gimac.des.monitoring.infrastructure.persistance.RapportsMonitoringRepository;
import cm.gimac.des.monitoring.infrastructure.persistance.VacationsRepository;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Activité des personnes programmées (D-116, docs/lot1/astreintes-monitoring.md §10.4), pour les chefs : créneaux des
 * grilles publiées sur la période, rapports déposés, rapports quotidiens déposés, en retard ou manquants.
 */
@Service
@Transactional(readOnly = true)
public class ActiviteEquipeService {

    public record Creneau(LocalDate date, String plage, int niveau, boolean chefDeQuart, UUID vacationId) {}

    public record Activite(
        Personne personne,
        List<Creneau> creneaux,
        int rapports,
        int quotidiensDeposes,
        int quotidiensEnRetard,
        int quotidiensManquants
    ) {}

    private final GrillesRepository grilles;
    private final AffectationsRepository affectations;
    private final VacationsRepository vacations;
    private final RapportsMonitoringRepository rapports;
    private final RapportsMonitoringService suivi;
    private final Quarts quarts;

    public ActiviteEquipeService(
        GrillesRepository grilles,
        AffectationsRepository affectations,
        VacationsRepository vacations,
        RapportsMonitoringRepository rapports,
        RapportsMonitoringService suivi,
        Quarts quarts
    ) {
        this.grilles = grilles;
        this.affectations = affectations;
        this.vacations = vacations;
        this.rapports = rapports;
        this.suivi = suivi;
        this.quarts = quarts;
    }

    private ZoneId zone() {
        return quarts.reglages().fuseau();
    }

    public List<Activite> activite(Appelant a, LocalDate du, LocalDate au, String personne) {
        if (!RapportsMonitoringService.chef(a)) {
            throw MonitoringException.interdit("MON_ACTIVITE_RESERVEE", "Réservé aux chefs (chef de quart ou gestion de la grille)");
        }
        LocalDate aujourdhui = LocalDate.ofInstant(quarts.maintenant(), zone());
        LocalDate fin = au == null ? aujourdhui : au;
        LocalDate debut = du == null ? fin.minusDays(6) : du;
        if (debut.isAfter(fin) || ChronoUnit.DAYS.between(debut, fin) >= RapportsMonitoringService.JOURS_MAX) {
            throw MonitoringException.invalide("du", "Période de 1 à " + RapportsMonitoringService.JOURS_MAX + " jours");
        }
        // Créneaux des grilles publiées (en vigueur) de chaque semaine touchée.
        Map<String, List<Creneau>> parPersonne = new TreeMap<>();
        Set<String> semaines = new LinkedHashSet<>();
        for (LocalDate j = debut; !j.isAfter(fin); j = j.plusDays(1)) {
            semaines.add(Plages.semaineStockee(j));
        }
        Map<String, UUID> vacationParCreneau = new HashMap<>();
        for (Shift v : vacations.findByShiftDateBetweenOrderByStartsAtAsc(debut, fin)) {
            vacationParCreneau.put(v.getShiftDate() + "/" + v.getSlot(), v.getId());
        }
        for (String s : semaines) {
            OnCallRoster g = grilles.findFirstByIsoWeekAndStateOrderByRevisionDesc(s, RosterState.PUBLIEE).orElse(null);
            if (g == null) {
                continue;
            }
            for (OnCallAssignment x : affectations.findByRosterIdOrderByDutyDateAscSlotAscLevelAsc(g.getId())) {
                if (x.getDutyDate().isBefore(debut) || x.getDutyDate().isAfter(fin)) {
                    continue;
                }
                parPersonne
                    .computeIfAbsent(x.getOperatorId().toString(), k -> new ArrayList<>())
                    .add(
                        new Creneau(
                            x.getDutyDate(),
                            x.getSlot().name(),
                            x.getLevel(),
                            Boolean.TRUE.equals(x.getShiftLead()),
                            x.getLevel() == 1 ? vacationParCreneau.get(x.getDutyDate() + "/" + x.getSlot()) : null
                        )
                    );
            }
        }
        // Rapports déposés sur la période, par auteur.
        Map<String, Integer> deposes = new HashMap<>();
        for (RapportMonitoring r : rapports.findByDeposeLeBetween(debut.atStartOfDay(zone()).toInstant(), fin.plusDays(1).atStartOfDay(zone()).toInstant())) {
            deposes.merge(r.getAuteurId().toString(), 1, Integer::sum);
        }
        // Rapports quotidiens : chaque membre de la vacation de nuit en porte la responsabilité.
        Map<String, int[]> quotidiens = new HashMap<>();
        for (RapportsMonitoringService.Quotidien q : suivi.quotidiens(a, debut, fin)) {
            int indice = switch (q.statut()) {
                case "DEPOSE" -> 0;
                case "EN_RETARD" -> q.rapport() != null ? 1 : 2;
                default -> -1;
            };
            if (indice < 0) {
                continue;
            }
            for (Personne p : q.attendus()) {
                quotidiens.computeIfAbsent(p.id(), k -> new int[3])[indice]++;
            }
        }
        Set<String> ids = new LinkedHashSet<>(parPersonne.keySet());
        ids.addAll(deposes.keySet());
        ids.addAll(quotidiens.keySet());
        if (personne != null && !personne.isBlank()) {
            ids.retainAll(Set.of(personne.strip()));
        }
        Map<String, String> noms = quarts.noms(ids);
        List<Activite> l = new ArrayList<>();
        for (String id : ids) {
            int[] q = quotidiens.getOrDefault(id, new int[3]);
            List<Creneau> c = parPersonne.getOrDefault(id, List.of()).stream().sorted(Comparator.comparing(Creneau::date).thenComparing(Creneau::plage)).toList();
            l.add(new Activite(Quarts.personne(id, noms), c, deposes.getOrDefault(id, 0), q[0], q[1], q[2]));
        }
        l.sort(Comparator.comparing((Activite x) -> x.personne().nom().toLowerCase(java.util.Locale.ROOT)));
        return l;
    }
}
