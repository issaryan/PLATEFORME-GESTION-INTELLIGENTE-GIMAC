package cm.gimac.des.monitoring.application.usecase;

import cm.gimac.des.monitoring.application.port.in.Appelant;
import cm.gimac.des.monitoring.application.port.in.Vues.CreneauVue;
import cm.gimac.des.monitoring.domain.OnCallAssignment;
import cm.gimac.des.monitoring.domain.exception.MonitoringException;
import cm.gimac.des.monitoring.domain.exploitation.ModeleCreneaux;
import cm.gimac.des.monitoring.domain.exploitation.ModeleCreneaux.Creneau;
import cm.gimac.des.monitoring.domain.exploitation.ModeleCreneaux.Occurrence;
import cm.gimac.des.monitoring.domain.exploitation.ModeleCreneaux.Portee;
import cm.gimac.des.monitoring.domain.exploitation.ProblemeGrille.Affectation;
import cm.gimac.des.monitoring.infrastructure.messaging.outbox.OutboxPublisher;
import cm.gimac.des.monitoring.infrastructure.persistance.CreneauxProgrammeJdbc;
import cm.gimac.des.monitoring.infrastructure.persistance.CreneauxProgrammeJdbc.CreneauAffecte;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Modèle des créneaux du programme (D-117, docs/lot1/astreintes-monitoring.md §11) : version en vigueur (modèle par
 * défaut tant qu'aucune n'est enregistrée), modification motivée par la gestion du programme, modèle figé de chaque
 * grille (historique de D-114 pour une grille antérieure) et placement de ses affectations dans le temps.
 */
@Service
@Transactional
public class CreneauxProgramme {

    /** Modèle en vigueur ; version 0, {@code modifieLe} et {@code modifiePar} nuls pour le modèle par défaut. */
    public record EnVigueur(int version, ModeleCreneaux modele, Instant modifieLe, UUID modifiePar) {}

    /** Affectation d'une grille placée dans le temps, avec son créneau. */
    public record Placee(OnCallAssignment entite, Creneau creneau, Affectation affectation) {}

    private final CreneauxProgrammeJdbc jdbc;
    private final Quarts quarts;
    private final OutboxPublisher outbox;

    public CreneauxProgramme(CreneauxProgrammeJdbc jdbc, Quarts quarts, OutboxPublisher outbox) {
        this.jdbc = jdbc;
        this.quarts = quarts;
        this.outbox = outbox;
    }

    private ZoneId zone() {
        return quarts.reglages().fuseau();
    }

    @Transactional(readOnly = true)
    public EnVigueur enVigueur() {
        return jdbc
            .courante()
            .map(v -> new EnVigueur(v.version(), v.modele(), v.creeLe(), v.creePar()))
            .orElseGet(() -> new EnVigueur(0, ModeleCreneaux.parDefaut(), null, null));
    }

    /** Remplace le modèle (MON_ROSTER_MANAGE) : 422 MON_CRENEAUX_INVALIDES avec les erreurs par champ ; motif de 3 à 500 caractères. */
    public EnVigueur modifier(Appelant appelant, ModeleCreneaux modele, String motif) {
        UUID auteur = appelant.uuid().orElseThrow(() -> MonitoringException.interdit("MON_JETON_UTILISATEUR_EXIGE", "Jeton utilisateur exigé"));
        String m = motif == null ? "" : motif.strip();
        if (m.length() < 3 || m.length() > 500) {
            throw MonitoringException.invalide("motif", "Motif de 3 à 500 caractères");
        }
        Map<String, String> erreurs = modele.erreurs();
        if (!erreurs.isEmpty()) {
            throw MonitoringException.regle("MON_CRENEAUX_INVALIDES", "Modèle des créneaux invalide", Map.of("erreurs", erreurs));
        }
        Instant le = quarts.maintenant();
        int v = jdbc.enregistrer(modele, auteur, m, le);
        Map<String, Object> evt = new LinkedHashMap<>();
        evt.put("version", v);
        evt.put("creneaux", modele.creneaux().stream().map(Creneau::code).toList());
        evt.put("motif", m);
        evt.put("par", auteur);
        outbox.publish("RosterSlotModel", UUID.nameUUIDFromBytes(("roster-slot-model-" + v).getBytes()), "monitoring.roster-slots-changed.v1", evt);
        return new EnVigueur(v, modele, le, auteur);
    }

    /** Modèle d'une grille : version figée à sa génération, sinon modèle historique de D-114. */
    @Transactional(readOnly = true)
    public ModeleCreneaux modeleDe(UUID grille) {
        return jdbc.versionDe(grille).flatMap(jdbc::version).orElseGet(ModeleCreneaux::historique);
    }

    /** Version du modèle en vigueur ; le modèle par défaut est enregistré comme version 1 à la première génération. */
    public int versionInitiale(UUID auteur) {
        return jdbc.courante().map(CreneauxProgrammeJdbc.Version::version).orElseGet(() -> jdbc.enregistrer(ModeleCreneaux.parDefaut(), auteur, "Modèle par défaut", quarts.maintenant()));
    }

    /** Version figée d'une grille ; 0 pour une grille antérieure à D-117 (modèle historique). */
    @Transactional(readOnly = true)
    public int versionDe(UUID grille) {
        return jdbc.versionDe(grille).orElse(0);
    }

    public void figer(UUID grille, int version) {
        if (version > 0) {
            jdbc.figer(grille, version);
        }
    }

    public void enregistrer(OnCallAssignment entite, Affectation a) {
        jdbc.affecter(entite.getId(), a.code(), a.debut(), a.fin(), a.semaine());
    }

    /**
     * Affectations placées dans le temps : complément D-117 s'il existe, sinon plage et niveau de D-114 lus dans le
     * modèle historique.
     */
    @Transactional(readOnly = true)
    public List<Placee> placer(ModeleCreneaux modele, List<OnCallAssignment> l) {
        Map<UUID, CreneauAffecte> c = jdbc.creneaux(l.stream().map(OnCallAssignment::getId).toList());
        ModeleCreneaux historique = ModeleCreneaux.historique();
        List<Placee> r = new ArrayList<>();
        for (OnCallAssignment x : l) {
            CreneauAffecte k = c.get(x.getId());
            boolean chef = Boolean.TRUE.equals(x.getShiftLead());
            if (k != null) {
                Creneau cr = modele.creneau(k.code());
                boolean nuit = cr != null ? cr.portee() == Portee.JOUR && cr.passeMinuit() : !k.semaine() && !k.debut().atZone(zone()).toLocalDate().equals(k.fin().minusSeconds(1).atZone(zone()).toLocalDate());
                r.add(new Placee(x, cr, new Affectation(x.getOperatorId(), k.code(), x.getDutyDate(), x.getLevel(), chef, k.debut(), k.fin(), k.semaine(), nuit)));
            } else {
                String code = ModeleCreneaux.codeHistorique(x.getSlot().name(), x.getLevel());
                Occurrence o = historique.occurrence(code, x.getDutyDate(), zone());
                r.add(new Placee(x, historique.creneau(code), Affectation.de(x.getOperatorId(), o, chef)));
            }
        }
        return r;
    }

    private static final java.time.format.DateTimeFormatter HEURE = java.time.format.DateTimeFormatter.ofPattern("HH:mm");

    public static CreneauVue vue(Creneau c) {
        return new CreneauVue(
            c.code(),
            c.libelles(),
            c.jours().stream().sorted().map(DayOfWeek::getValue).toList(),
            c.debut().format(HEURE),
            c.fin().format(HEURE),
            c.portee().name(),
            c.niveau(),
            c.effectifMin(),
            c.effectifMax(),
            c.permissions(),
            c.eligibles().stream().map(UUID::toString).toList(),
            c.chefDeQuart(),
            c.rapportQuotidien(),
            c.heuresComptees(),
            c.ordre(),
            c.actif()
        );
    }

    /** Modèle saisi : 400 MON_REQUETE_INVALIDE sur un champ illisible (heure, jour, portée, identifiant). */
    public static ModeleCreneaux modele(List<CreneauVue> l) {
        List<Creneau> creneaux = new ArrayList<>();
        for (int i = 0; i < l.size(); i++) {
            CreneauVue v = l.get(i);
            String p = "creneaux[" + i + "].";
            if (v == null) {
                throw MonitoringException.invalide("creneaux[" + i + "]", "Créneau vide");
            }
            Set<DayOfWeek> jours = EnumSet.noneOf(DayOfWeek.class);
            for (Integer j : v.jours() == null ? List.<Integer>of() : v.jours()) {
                if (j == null || j < 1 || j > 7) {
                    throw MonitoringException.invalide(p + "jours", "Jours de 1 (lundi) à 7 (dimanche)");
                }
                jours.add(DayOfWeek.of(j));
            }
            Portee portee;
            try {
                portee = Portee.valueOf(v.portee() == null ? "" : v.portee());
            } catch (IllegalArgumentException e) {
                throw MonitoringException.invalide(p + "portee", "Portée JOUR ou SEMAINE");
            }
            List<UUID> eligibles = new ArrayList<>();
            for (String id : v.eligibles() == null ? List.<String>of() : v.eligibles()) {
                try {
                    eligibles.add(UUID.fromString(id));
                } catch (IllegalArgumentException | NullPointerException e) {
                    throw MonitoringException.invalide(p + "eligibles", "Identifiant de compte invalide");
                }
            }
            Map<String, String> libelles = new LinkedHashMap<>();
            (v.libelles() == null ? Map.<String, String>of() : v.libelles()).forEach((k, x) -> {
                if (x != null && !x.isBlank()) {
                    libelles.put(k, x.strip());
                }
            });
            creneaux.add(
                new Creneau(
                    v.code() == null ? null : v.code().strip(),
                    libelles,
                    jours,
                    heure(v.debut(), p + "debut"),
                    heure(v.fin(), p + "fin"),
                    portee,
                    v.niveau(),
                    v.effectifMin(),
                    v.effectifMax(),
                    v.permissions(),
                    eligibles,
                    v.chefDeQuart(),
                    v.rapportQuotidien(),
                    v.heuresComptees(),
                    v.ordre(),
                    v.actif()
                )
            );
        }
        return new ModeleCreneaux(creneaux);
    }

    private static LocalTime heure(String v, String champ) {
        try {
            return LocalTime.parse(v == null ? "" : v.strip());
        } catch (DateTimeParseException e) {
            throw MonitoringException.invalide(champ, "Heure HH:mm");
        }
    }

    public void creneauVacation(UUID vacation, String code) {
        jdbc.creneauVacation(vacation, code);
    }

    @Transactional(readOnly = true)
    public String creneauVacation(UUID vacation) {
        return jdbc.creneauVacation(vacation).orElse(null);
    }
}
