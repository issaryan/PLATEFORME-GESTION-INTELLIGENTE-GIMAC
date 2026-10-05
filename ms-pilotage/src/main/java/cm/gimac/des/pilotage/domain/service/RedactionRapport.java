package cm.gimac.des.pilotage.domain.service;

import cm.gimac.des.pilotage.domain.activite.Periode;
import cm.gimac.des.pilotage.domain.activite.StatutActivite;
import cm.gimac.des.pilotage.domain.activite.TypeRapport;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

/**
 * Contenu prérempli d'un rapport (§2.4), en markdown, à partir des activités et des tâches de la
 * période. Quotidien : titre, « Réalisé » (activités faites, avec leur durée), « En cours » (activités
 * commencées ou encore à faire), « Reporté », « Tâches terminées », « Prévu demain » (activités du jour
 * ouvré suivant). Hebdomadaire : les mêmes rubriques regroupées par jour, plus le total des heures.
 * Rien n'est enregistré : l'auteur modifie ensuite librement ce texte.
 */
public final class RedactionRapport {

    /** Activité de la période, réduite à ce que le rapport en dit. */
    public record Ligne(LocalDate jour, int ordre, String titre, StatutActivite statut, Integer dureeMinutes, LocalDate reporteeAu) {}

    /** Tâche terminée par l'auteur, au jour de sa clôture (fuseau du GIMAC). */
    public record TacheTerminee(LocalDate jour, String titre) {}

    static final String NEANT = "- Néant";
    private static final Locale FR = Locale.FRENCH;
    private static final DateTimeFormatter JOUR_LONG = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", FR);

    private RedactionRapport() {}

    /**
     * @param activites activités de la période
     * @param taches    tâches terminées dans la période
     * @param prevues   activités du jour ouvré suivant la période
     * @param suivant   jour ouvré suivant la période
     */
    public static String rediger(Periode periode, List<Ligne> activites, List<TacheTerminee> taches, List<Ligne> prevues, LocalDate suivant) {
        List<Ligne> triees = activites.stream().sorted(Comparator.comparing(Ligne::jour).thenComparingInt(Ligne::ordre)).toList();
        StringBuilder md = new StringBuilder();
        if (periode.type() == TypeRapport.QUOTIDIEN) {
            md.append("# Rapport quotidien du ").append(jour(periode.debut())).append("\n\n");
            rubriques(md, "## ", triees, taches);
            md.append("## Prévu demain (").append(jour(suivant)).append(")\n\n");
        } else {
            md
                .append("# Rapport hebdomadaire, semaine ")
                .append(periode.semaine())
                .append(" (du ")
                .append(jour(periode.debut()))
                .append(" au ")
                .append(jour(periode.fin()))
                .append(")\n\n");
            int total = triees.stream().filter(l -> l.statut() == StatutActivite.FAITE && l.dureeMinutes() != null).mapToInt(Ligne::dureeMinutes).sum();
            md.append("**Total des heures : ").append(total == 0 ? "0 h" : duree(total)).append("**\n\n");
            boolean unJour = false;
            for (LocalDate j = periode.debut(); !j.isAfter(periode.fin()); j = j.plusDays(1)) {
                LocalDate jour = j;
                List<Ligne> duJour = triees.stream().filter(l -> l.jour().equals(jour)).toList();
                List<TacheTerminee> tachesDuJour = taches.stream().filter(t -> t.jour().equals(jour)).toList();
                if (duJour.isEmpty() && tachesDuJour.isEmpty()) {
                    continue;
                }
                unJour = true;
                md.append("## ").append(majuscule(jour(jour))).append("\n\n");
                rubriques(md, "### ", duJour, tachesDuJour);
            }
            if (!unJour) {
                md.append("Aucune activité ni tâche terminée sur la semaine.\n\n");
            }
            md.append("## Prévu (").append(jour(suivant)).append(")\n\n");
        }
        liste(md, prevues.stream().sorted(Comparator.comparingInt(Ligne::ordre)).filter(l -> l.statut() != StatutActivite.ANNULEE).map(Ligne::titre).toList());
        return md.toString().stripTrailing() + "\n";
    }

    private static void rubriques(StringBuilder md, String niveau, List<Ligne> lignes, List<TacheTerminee> taches) {
        md.append(niveau).append("Réalisé\n\n");
        liste(md, filtre(lignes, l -> l.statut() == StatutActivite.FAITE).stream().map(l -> l.titre() + (l.dureeMinutes() == null || l.dureeMinutes() == 0 ? "" : " (" + duree(l.dureeMinutes()) + ")")).toList());
        md.append(niveau).append("En cours\n\n");
        liste(
            md,
            filtre(lignes, l -> l.statut() == StatutActivite.EN_COURS || l.statut() == StatutActivite.A_FAIRE)
                .stream()
                .map(l -> l.titre() + (l.statut() == StatutActivite.A_FAIRE ? " (non commencée)" : ""))
                .toList()
        );
        md.append(niveau).append("Reporté\n\n");
        liste(md, filtre(lignes, l -> l.statut() == StatutActivite.REPORTEE).stream().map(l -> l.titre() + (l.reporteeAu() == null ? "" : " (reporté au " + jour(l.reporteeAu()) + ")")).toList());
        md.append(niveau).append("Tâches terminées\n\n");
        liste(md, taches.stream().map(TacheTerminee::titre).toList());
    }

    private static List<Ligne> filtre(List<Ligne> lignes, Predicate<Ligne> p) {
        return lignes.stream().filter(p).toList();
    }

    private static void liste(StringBuilder md, List<String> elements) {
        if (elements.isEmpty()) {
            md.append(NEANT).append("\n\n");
            return;
        }
        elements.forEach(e -> md.append("- ").append(e.replace('\n', ' ')).append('\n'));
        md.append('\n');
    }

    /** 90 donne « 1 h 30 », 45 donne « 45 min », 120 donne « 2 h ». */
    public static String duree(int minutes) {
        int h = minutes / 60;
        int m = minutes % 60;
        if (h == 0) {
            return m + " min";
        }
        return m == 0 ? h + " h" : h + " h " + String.format("%02d", m);
    }

    static String jour(LocalDate jour) {
        return JOUR_LONG.format(jour);
    }

    private static String majuscule(String s) {
        return s.isEmpty() ? s : s.substring(0, 1).toUpperCase(FR) + s.substring(1);
    }
}
