package cm.gimac.des.monitoring.application.usecase;

import cm.gimac.des.monitoring.application.port.in.Vues.Personne;
import cm.gimac.des.monitoring.application.port.out.AnnuairePort;
import cm.gimac.des.monitoring.domain.AlertSeverityPolicy;
import cm.gimac.des.monitoring.domain.Shift;
import cm.gimac.des.monitoring.domain.enumeration.AlertSeverity;
import cm.gimac.des.monitoring.domain.exploitation.Plages;
import cm.gimac.des.monitoring.domain.exploitation.PolitiqueAlerte;
import cm.gimac.des.monitoring.infrastructure.persistance.PolitiquesRepository;
import cm.gimac.des.monitoring.infrastructure.persistance.VacationsRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Outils partagés des cas d'utilisation : vacation en cours et voisines, membres d'une vacation, noms affichés,
 * politiques d'alerte (table alert_severity_policy, à défaut tableau 6.48).
 */
@Component
public class Quarts {

    private final VacationsRepository vacations;
    private final PolitiquesRepository politiques;
    private final AnnuairePort annuaire;
    private final Reglages reglages;
    private final Clock clock;

    public Quarts(VacationsRepository vacations, PolitiquesRepository politiques, AnnuairePort annuaire, Reglages reglages, Clock clock) {
        this.vacations = vacations;
        this.politiques = politiques;
        this.annuaire = annuaire;
        this.reglages = reglages;
        this.clock = clock;
    }

    public Instant maintenant() {
        return clock.instant();
    }

    public Reglages reglages() {
        return reglages;
    }

    public Optional<Shift> enCours() {
        Instant t = maintenant();
        return vacations.findFirstByStartsAtLessThanEqualAndEndsAtGreaterThanOrderByStartsAtDesc(t, t);
    }

    public Optional<Shift> precedente(Shift v) {
        Plages.Creneau c = Plages.precedent(new Plages.Creneau(v.getShiftDate(), v.getSlot()));
        return vacations.findFirstByShiftDateAndSlot(c.jour(), c.plage());
    }

    public Optional<Shift> suivante(Shift v) {
        Plages.Creneau c = Plages.suivant(new Plages.Creneau(v.getShiftDate(), v.getSlot()));
        return vacations.findFirstByShiftDateAndSlot(c.jour(), c.plage());
    }

    /** Opérateurs d'une vacation (identifiants séparés par des virgules). */
    public static List<String> operateurs(Shift v) {
        if (v.getOperatorIds() == null || v.getOperatorIds().isBlank()) {
            return List.of();
        }
        return Arrays.stream(v.getOperatorIds().split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    public static String joindre(Collection<UUID> ids) {
        return String.join(",", ids.stream().map(UUID::toString).toList());
    }

    /** Opérateurs et chef de quart. */
    public static Set<String> membres(Shift v) {
        Set<String> m = new LinkedHashSet<>(operateurs(v));
        if (v.getShiftLeadId() != null) {
            m.add(v.getShiftLeadId().toString());
        }
        return m;
    }

    public static boolean membre(Shift v, String compte) {
        return compte != null && membres(v).contains(compte);
    }

    public static boolean chef(Shift v, String compte) {
        return compte != null && v.getShiftLeadId() != null && v.getShiftLeadId().toString().equals(compte);
    }

    public Map<String, String> noms(Collection<String> ids) {
        return annuaire.noms(ids.stream().filter(java.util.Objects::nonNull).toList());
    }

    /** Porteurs de MON_ROSTER_PUBLISH (chef du DES et délégataires). */
    public List<String> porteursChefDes() {
        return annuaire.porteurs(cm.gimac.des.monitoring.application.port.in.Appelant.PUBLIER_GRILLE).stream().map(AnnuairePort.Personne::id).toList();
    }

    public static Personne personne(Object id, Map<String, String> noms) {
        if (id == null) {
            return null;
        }
        String s = id.toString();
        return new Personne(s, noms.getOrDefault(s, s));
    }

    public PolitiqueAlerte politique(AlertSeverity gravite) {
        return politiques.findBySeverity(gravite).map(Quarts::depuis).orElse(PolitiqueAlerte.DEFAUTS.get(gravite));
    }

    public List<PolitiqueAlerte> politiques() {
        return Arrays.stream(AlertSeverity.values()).sorted((a, b) -> b.ordinal() - a.ordinal()).map(this::politique).toList();
    }

    private static PolitiqueAlerte depuis(AlertSeverityPolicy p) {
        PolitiqueAlerte d = PolitiqueAlerte.DEFAUTS.get(p.getSeverity());
        return new PolitiqueAlerte(
            p.getSeverity(),
            p.getAckDelay() == null ? d.delai() : p.getAckDelay(),
            p.getNotificationLevel() == null ? d.niveau() : p.getNotificationLevel(),
            Boolean.TRUE.equals(p.getUrgentChannel()),
            p.getTicketPolicy() == null ? d.ticket() : p.getTicketPolicy()
        );
    }
}
