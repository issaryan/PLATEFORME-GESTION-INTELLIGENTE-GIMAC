package cm.gimac.des.monitoring.application.usecase;

import cm.gimac.des.monitoring.application.port.in.Appelant;
import cm.gimac.des.monitoring.application.port.in.Page;
import cm.gimac.des.monitoring.application.port.in.Vues.Personne;
import cm.gimac.des.monitoring.application.port.out.PiecesPort;
import cm.gimac.des.monitoring.domain.OnCallAssignment;
import cm.gimac.des.monitoring.domain.Shift;
import cm.gimac.des.monitoring.domain.enumeration.RosterState;
import cm.gimac.des.monitoring.domain.enumeration.ShiftSlot;
import cm.gimac.des.monitoring.domain.exception.MonitoringException;
import cm.gimac.des.monitoring.infrastructure.messaging.outbox.OutboxPublisher;
import cm.gimac.des.monitoring.infrastructure.persistance.AffectationsRepository;
import cm.gimac.des.monitoring.infrastructure.persistance.EnvoiRapport;
import cm.gimac.des.monitoring.infrastructure.persistance.EnvoisRapportRepository;
import cm.gimac.des.monitoring.infrastructure.persistance.RapportMonitoring;
import cm.gimac.des.monitoring.infrastructure.persistance.RapportMonitoring.Nature;
import cm.gimac.des.monitoring.infrastructure.persistance.RapportsMonitoringRepository;
import cm.gimac.des.monitoring.infrastructure.persistance.VacationsRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Rapports de monitoring et d'astreinte (D-116, docs/lot1/astreintes-monitoring.md §10.1, §10.2) : dépôt rattaché au
 * programme (vacation, affectation ou journée pour le rapport quotidien), lecture par l'auteur, ses destinataires et les
 * chefs, envoi au supérieur ou à la personne choisie, suivi du rapport quotidien de 8 h.
 */
@Service
@Transactional
public class RapportsMonitoringService {

    /** Rapport tel que servi. */
    public record RapportVue(
        UUID id,
        String nature,
        String titre,
        String resume,
        LocalDate date,
        VacationCourte vacation,
        AffectationCourte affectation,
        Personne auteur,
        Instant deposeLe,
        Instant echeance,
        boolean enRetard,
        Document document,
        List<EnvoiVue> envois
    ) {}

    public record VacationCourte(UUID id, String reference, String plage, LocalDate date) {}

    public record AffectationCourte(UUID id, LocalDate date, String plage, int niveau) {}

    public record Document(String id, String nom, long taille, String type) {}

    public record EnvoiVue(Personne destinataire, Instant le, String message) {}

    /** Fichier d'un rapport à restituer. */
    public record Fichier(String nom, String type, byte[] contenu) {}

    /** Suivi du rapport quotidien d'une journée. */
    public record Quotidien(LocalDate date, VacationCourte vacation, List<Personne> attendus, RapportCourt rapport, Instant echeance, String statut) {}

    public record RapportCourt(UUID id, Instant deposeLe, boolean enRetard) {}

    static final int JOURS_MAX = 92;

    private final RapportsMonitoringRepository rapports;
    private final EnvoisRapportRepository envois;
    private final VacationsRepository vacations;
    private final AffectationsRepository affectations;
    private final PiecesPort pieces;
    private final Quarts quarts;
    private final OutboxPublisher outbox;
    private final LocalTime echeanceQuotidien;

    public RapportsMonitoringService(
        RapportsMonitoringRepository rapports,
        EnvoisRapportRepository envois,
        VacationsRepository vacations,
        AffectationsRepository affectations,
        PiecesPort pieces,
        Quarts quarts,
        OutboxPublisher outbox,
        @Value("${gimac.monitoring.rapport-quotidien.echeance:08:00}") LocalTime echeanceQuotidien
    ) {
        this.rapports = rapports;
        this.envois = envois;
        this.vacations = vacations;
        this.affectations = affectations;
        this.pieces = pieces;
        this.quarts = quarts;
        this.outbox = outbox;
        this.echeanceQuotidien = echeanceQuotidien;
    }

    private ZoneId zone() {
        return quarts.reglages().fuseau();
    }

    /** Chef : MON_SHIFT_LEAD ou MON_ROSTER_MANAGE, voit tous les rapports et l'activité de l'équipe. */
    public static boolean chef(Appelant a) {
        return a.porte(Appelant.CHEF_DE_QUART) || a.porte(Appelant.GERER_GRILLE);
    }

    private static UUID moi(Appelant a) {
        return a.uuid().orElseThrow(() -> MonitoringException.interdit("MON_JETON_UTILISATEUR_EXIGE", "Jeton utilisateur exigé"));
    }

    /** Échéance du rapport quotidien qui porte sur la journée {@code jour} : le lendemain à l'heure paramétrée. */
    public Instant echeance(LocalDate jour) {
        return jour.plusDays(1).atTime(echeanceQuotidien).atZone(zone()).toInstant();
    }

    private static String texte(String v, String champ, int min, int max) {
        String t = v == null ? "" : v.strip();
        if (t.length() < min || t.length() > max) {
            throw MonitoringException.invalide(champ, champ + " de " + min + " à " + max + " caractères");
        }
        return t;
    }

    // ------------------------------------------------------------------ dépôt

    public RapportVue deposer(
        Appelant a,
        String nature,
        UUID vacationId,
        UUID affectationId,
        LocalDate date,
        String titre,
        String resume,
        String documentRef
    ) {
        UUID moi = moi(a);
        Nature n = nature(nature);
        String t = texte(titre, "titre", 3, 160);
        String r = resume == null || resume.isBlank() ? null : texte(resume, "resume", 1, 2000);
        PiecesPort.Piece piece = pieces
            .lire(documentRef)
            .filter(p -> PiecesPort.FINALITE.equals(p.finalite()) && a.id().equals(p.deposant()))
            .orElseThrow(() -> MonitoringException.regle("MON_PIECE_INVALIDE", "Pièce absente, d'une autre finalité ou déposée par un autre compte"));
        boolean chef = chef(a);
        Instant maintenant = quarts.maintenant();
        LocalDate jour = null;
        Instant echeance = null;
        UUID vac = null;
        UUID aff = null;
        switch (n) {
            case QUOTIDIEN -> {
                jour = date == null ? LocalDate.ofInstant(maintenant, zone()).minusDays(1) : date;
                LocalDate j = jour;
                Optional<Shift> nuit = vacations.findFirstByShiftDateAndSlot(j, ShiftSlot.NUIT);
                if (!chef && nuit.filter(v -> Quarts.membre(v, a.id())).isEmpty()) {
                    throw MonitoringException.interdit("MON_RAPPORT_HORS_PROGRAMME", "Le rapport quotidien revient à l'équipe de nuit de cette journée");
                }
                if (rapports.findFirstByNatureAndDate(Nature.QUOTIDIEN, j).isPresent()) {
                    throw MonitoringException.conflit("MON_RAPPORT_QUOTIDIEN_EXISTANT", "Le rapport quotidien de cette journée est déjà déposé");
                }
                echeance = echeance(j);
                vac = nuit.map(Shift::getId).orElse(null);
            }
            case VACATION -> vac = vacation(a, chef, vacationId, true).getId();
            case ASTREINTE -> aff = affectation(a, chef, affectationId, true).getId();
            case FIN, AUTRE -> {
                if (vacationId != null) {
                    vac = vacation(a, chef, vacationId, true).getId();
                }
                if (affectationId != null) {
                    aff = affectation(a, chef, affectationId, true).getId();
                }
            }
        }
        RapportMonitoring rm = new RapportMonitoring(n, t, r, jour, vac, aff, moi, maintenant, echeance, new RapportMonitoring.Piece(piece.id(), piece.nom(), piece.taille(), piece.type()));
        rapports.save(rm);
        Map<String, Object> evt = new LinkedHashMap<>();
        evt.put("rapportId", rm.getId());
        evt.put("nature", n.name());
        evt.put("date", jour);
        evt.put("auteurId", moi);
        evt.put("enRetard", rm.isEnRetard());
        evt.put("documentRef", piece.id());
        outbox.publish("MonitoringReport", rm.getId(), "monitoring.report-deposited.v1", evt);
        return vue(rm);
    }

    private static Nature nature(String nature) {
        try {
            return Nature.valueOf(nature == null ? "" : nature.strip().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw MonitoringException.invalide("nature", "Nature : QUOTIDIEN, VACATION, ASTREINTE, FIN ou AUTRE");
        }
    }

    private Shift vacation(Appelant a, boolean chef, UUID id, boolean requise) {
        if (id == null) {
            throw MonitoringException.invalide("vacationId", "vacationId obligatoire");
        }
        Shift v = vacations.findById(id).orElseThrow(() -> MonitoringException.introuvable("MON_VACATION_INTROUVABLE", id));
        if (!chef && !Quarts.membre(v, a.id())) {
            throw MonitoringException.interdit("MON_RAPPORT_HORS_PROGRAMME", "Réservé aux membres de la vacation");
        }
        return v;
    }

    private OnCallAssignment affectation(Appelant a, boolean chef, UUID id, boolean requise) {
        if (id == null) {
            throw MonitoringException.invalide("affectationId", "affectationId obligatoire");
        }
        OnCallAssignment x = affectations
            .findById(id)
            .filter(y -> y.getRoster().getState() == RosterState.PUBLIEE || y.getRoster().getState() == RosterState.REMPLACEE)
            .orElseThrow(() -> MonitoringException.introuvable("MON_AFFECTATION_INTROUVABLE", id));
        if (!chef && !x.getOperatorId().toString().equals(a.id())) {
            throw MonitoringException.interdit("MON_RAPPORT_HORS_PROGRAMME", "Réservé au titulaire du créneau");
        }
        return x;
    }

    // ------------------------------------------------------------------ lecture

    @Transactional(readOnly = true)
    public Page<RapportVue> lister(Appelant a, String nature, UUID auteur, LocalDate du, LocalDate au, Integer page, Integer taille) {
        UUID moi = moi(a);
        Nature n = nature == null || nature.isBlank() ? null : nature(nature);
        int p = page == null ? 0 : Math.max(0, page);
        int t = taille == null ? 20 : taille;
        if (t < 1 || t > 100) {
            throw MonitoringException.invalide("taille", "Taille de 1 à 100");
        }
        Instant debut = du == null ? null : du.atStartOfDay(zone()).toInstant();
        Instant fin = au == null ? null : au.plusDays(1).atStartOfDay(zone()).toInstant();
        var res = rapports.rechercher(chef(a), moi, n, auteur, debut, fin, PageRequest.of(p, t));
        return new Page<>(res.getContent().stream().map(this::vue).toList(), res.getTotalElements(), res.getNumber(), res.getSize());
    }

    private RapportMonitoring lisible(Appelant a, UUID id) {
        UUID moi = moi(a);
        RapportMonitoring r = rapports.findById(id).orElseThrow(() -> MonitoringException.introuvable("MON_RAPPORT_INTROUVABLE", id));
        if (!chef(a) && !r.getAuteurId().equals(moi) && !envois.existsByRapportIdAndDestinataireId(id, moi)) {
            throw MonitoringException.introuvable("MON_RAPPORT_INTROUVABLE", id);
        }
        return r;
    }

    @Transactional(readOnly = true)
    public RapportVue lire(Appelant a, UUID id) {
        return vue(lisible(a, id));
    }

    @Transactional(readOnly = true)
    public Fichier fichier(Appelant a, UUID id) {
        RapportMonitoring r = lisible(a, id);
        RapportMonitoring.Piece p = r.getPiece();
        return new Fichier(p.nom(), p.type(), pieces.contenu(p.id()));
    }

    // ------------------------------------------------------------------ envoi

    /** L'auteur envoie son rapport à 1 à 20 destinataires (supérieur ou personne choisie), qui sont notifiés. */
    public RapportVue envoyer(Appelant a, UUID id, List<String> destinataires, String message) {
        UUID moi = moi(a);
        RapportMonitoring r = rapports.findById(id).orElseThrow(() -> MonitoringException.introuvable("MON_RAPPORT_INTROUVABLE", id));
        if (!r.getAuteurId().equals(moi)) {
            if (chef(a) || envois.existsByRapportIdAndDestinataireId(id, moi)) {
                throw MonitoringException.interdit("MON_RAPPORT_ENVOI_INTERDIT", "Seul l'auteur envoie son rapport");
            }
            throw MonitoringException.introuvable("MON_RAPPORT_INTROUVABLE", id);
        }
        if (destinataires == null || destinataires.isEmpty() || destinataires.size() > 20) {
            throw MonitoringException.invalide("destinataires", "De 1 à 20 destinataires");
        }
        String m = message == null || message.isBlank() ? null : texte(message, "message", 1, 500);
        Set<UUID> cibles = new LinkedHashSet<>();
        for (String d : destinataires) {
            try {
                cibles.add(UUID.fromString(d));
            } catch (IllegalArgumentException | NullPointerException e) {
                throw MonitoringException.invalide("destinataires", "Identifiant de compte invalide");
            }
        }
        cibles.remove(moi);
        Instant maintenant = quarts.maintenant();
        List<String> nouveaux = new ArrayList<>();
        for (UUID c : cibles) {
            if (!envois.existsByRapportIdAndDestinataireId(id, c)) {
                envois.save(new EnvoiRapport(id, c, maintenant, m));
                nouveaux.add(c.toString());
            }
        }
        if (!nouveaux.isEmpty()) {
            Map<String, Object> n = new LinkedHashMap<>();
            n.put("destinataires", nouveaux);
            n.put("niveau", "INFORMATION");
            n.put("objet", "RAPPORT_RECU");
            n.put("objetId", id.toString());
            n.put("reference", r.getTitre());
            n.put("acteurId", moi.toString());
            n.put("le", maintenant.toString());
            n.put("message", m);
            n.put("lien", "/des/exploitation/rapports?rapport=" + id);
            outbox.publish("MonitoringReport", id, "monitoring.notification-requested.v1", n);
        }
        return vue(r);
    }

    // ------------------------------------------------------------------ rapport quotidien

    /** Suivi des rapports quotidiens du {@code du} au {@code au} (92 jours au plus) ; un opérateur ne voit que les siens. */
    @Transactional(readOnly = true)
    public List<Quotidien> quotidiens(Appelant a, LocalDate du, LocalDate au) {
        LocalDate fin = au == null ? LocalDate.ofInstant(quarts.maintenant(), zone()).minusDays(1) : au;
        LocalDate debut = du == null ? fin.minusDays(6) : du;
        if (debut.isAfter(fin) || ChronoUnit.DAYS.between(debut, fin) >= JOURS_MAX) {
            throw MonitoringException.invalide("du", "Période de 1 à " + JOURS_MAX + " jours");
        }
        boolean chef = chef(a);
        Map<LocalDate, RapportMonitoring> parJour = new LinkedHashMap<>();
        rapports.findByNatureAndDateBetween(Nature.QUOTIDIEN, debut, fin).forEach(r -> parJour.put(r.getDate(), r));
        List<Shift> nuits = vacations.findByShiftDateBetweenOrderByStartsAtAsc(debut, fin).stream().filter(v -> v.getSlot() == ShiftSlot.NUIT).toList();
        Set<String> ids = new HashSet<>();
        nuits.forEach(v -> ids.addAll(Quarts.membres(v)));
        Map<String, String> noms = quarts.noms(ids);
        Instant maintenant = quarts.maintenant();
        List<Quotidien> l = new ArrayList<>();
        for (LocalDate j = fin; !j.isBefore(debut); j = j.minusDays(1)) {
            LocalDate jour = j;
            Shift nuit = nuits.stream().filter(v -> v.getShiftDate().equals(jour)).findFirst().orElse(null);
            if (!chef && (nuit == null || !Quarts.membre(nuit, a.id()))) {
                continue;
            }
            RapportMonitoring r = parJour.get(jour);
            Instant ech = echeance(jour);
            String statut;
            if (r != null) {
                statut = r.isEnRetard() ? "EN_RETARD" : "DEPOSE";
            } else if (nuit == null) {
                statut = "SANS_VACATION";
            } else {
                statut = maintenant.isAfter(ech) ? "EN_RETARD" : "ATTENDU";
            }
            l.add(
                new Quotidien(
                    jour,
                    nuit == null ? null : new VacationCourte(nuit.getId(), nuit.getReference(), nuit.getSlot().name(), nuit.getShiftDate()),
                    nuit == null ? List.of() : Quarts.membres(nuit).stream().map(i -> Quarts.personne(i, noms)).toList(),
                    r == null ? null : new RapportCourt(r.getId(), r.getDeposeLe(), r.isEnRetard()),
                    ech,
                    statut
                )
            );
        }
        return l;
    }

    // ------------------------------------------------------------------ vue

    RapportVue vue(RapportMonitoring r) {
        List<EnvoiRapport> e = envois.findByRapportIdOrderByLeAsc(r.getId());
        Set<String> ids = new HashSet<>();
        ids.add(r.getAuteurId().toString());
        e.forEach(x -> ids.add(x.getDestinataireId().toString()));
        Map<String, String> noms = quarts.noms(ids);
        VacationCourte v = r.getVacationId() == null
            ? null
            : vacations.findById(r.getVacationId()).map(s -> new VacationCourte(s.getId(), s.getReference(), s.getSlot().name(), s.getShiftDate())).orElse(null);
        AffectationCourte af = r.getAffectationId() == null
            ? null
            : affectations.findById(r.getAffectationId()).map(x -> new AffectationCourte(x.getId(), x.getDutyDate(), x.getSlot().name(), x.getLevel())).orElse(null);
        RapportMonitoring.Piece p = r.getPiece();
        return new RapportVue(
            r.getId(),
            r.getNature().name(),
            r.getTitre(),
            r.getResume(),
            r.getDate(),
            v,
            af,
            Quarts.personne(r.getAuteurId(), noms),
            r.getDeposeLe(),
            r.getEcheance(),
            r.isEnRetard(),
            new Document(p.id(), p.nom(), p.taille(), p.type()),
            e.stream().map(x -> new EnvoiVue(Quarts.personne(x.getDestinataireId(), noms), x.getLe(), x.getMessage())).toList()
        );
    }
}
