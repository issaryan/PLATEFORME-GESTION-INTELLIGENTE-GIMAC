package cm.gimac.des.iam.domain.fonctionnalite;

import java.time.Instant;
import java.util.List;

/**
 * Fonctionnalité d'une application (D-94, docs/lot1/ecrans-par-configuration.md §2) : vue qui apparaît ou disparaît
 * selon les permissions du jeton, décrite en données et modifiable depuis la plateforme. Le code de l'interface ne
 * garde que la correspondance {@code cle → composant et chemin} ; barre latérale, gardes de route, console, tableau
 * de bord et éditeur de profil par fonctionnalité (D-90) en dérivent.
 *
 * @param cle identifiant stable, unique toutes applications confondues
 * @param application des, console ou portail (immuable)
 * @param module module de l'éditeur de profil par fonctionnalité (D-90), regroupement des droits
 * @param libelle clé de traduction de l'interface (par exemple nav.liaisons) ou texte français
 * @param groupe groupe de la barre latérale ; nul pour une fonctionnalité hors barre latérale (écran ou geste
 *     atteint depuis un autre écran)
 * @param ordre rang dans l'application (barre latérale, puis groupes dans l'ordre de leur première fonctionnalité)
 * @param icone nom d'icône du système de conception
 * @param visible faux : masquée de la barre latérale et du tableau de bord quelles que soient les permissions
 * @param permissions prédicat qui ouvre la fonctionnalité : toutes celles de {@code tous} et, si {@code unDe} n'est
 *     pas vide, l'une au moins de {@code unDe} ; vide : toute session interne
 * @param widgets widgets du tableau de bord apportés, chacun avec son prédicat propre en plus de celui de la
 *     fonctionnalité
 * @param droits actions sur la fonctionnalité, chacune reliée à une permission (D-90)
 * @param modifieLe dernière modification depuis la plateforme ; nulle pour une valeur de départ jamais modifiée
 * @param modifiePar auteur de cette modification, nul de même
 */
public record Fonctionnalite(
    String cle,
    String application,
    String module,
    String libelle,
    String groupe,
    int ordre,
    String icone,
    boolean visible,
    Predicat permissions,
    List<Widget> widgets,
    List<Droit> droits,
    Instant modifieLe,
    Auteur modifiePar
) {
    /** Prédicat de permissions : toutes celles de {@code tous}, l'une au moins de {@code unDe} (si non vide). */
    public record Predicat(List<String> tous, List<String> unDe) {
        public Predicat {
            tous = tous == null ? List.of() : List.copyOf(tous);
            unDe = unDe == null ? List.of() : List.copyOf(unDe);
        }

        public static Predicat aucun() {
            return new Predicat(List.of(), List.of());
        }
    }

    /** Widget du tableau de bord, avec son prédicat propre. */
    public record Widget(String cle, Predicat permissions) {
        public Widget {
            permissions = permissions == null ? Predicat.aucun() : permissions;
        }
    }

    /** Droit (D-90) : action sur la fonctionnalité, reliée à une permission du domaine. */
    public record Droit(String cle, NatureDroit nature, String permission, String libelle) {}

    /** Auteur d'une modification : identifiant et nom affiché du compte. */
    public record Auteur(String id, String nomAffiche) {}

    public Fonctionnalite {
        permissions = permissions == null ? Predicat.aucun() : permissions;
        widgets = widgets == null ? List.of() : List.copyOf(widgets);
        droits = droits == null ? List.of() : List.copyOf(droits);
    }

    /** Même fonctionnalité sans trace de modification (comparaison des valeurs). */
    public Fonctionnalite sansTrace() {
        return new Fonctionnalite(cle, application, module, libelle, groupe, ordre, icone, visible, permissions, widgets, droits, null, null);
    }

    /** Même fonctionnalité avec la trace de sa modification. */
    public Fonctionnalite modifiee(Instant le, String parId, String parNom) {
        return new Fonctionnalite(cle, application, module, libelle, groupe, ordre, icone, visible, permissions, widgets, droits, le, new Auteur(parId, parNom));
    }
}
