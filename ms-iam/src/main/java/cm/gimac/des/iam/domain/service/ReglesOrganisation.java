package cm.gimac.des.iam.domain.service;

import cm.gimac.des.iam.domain.IamException;
import cm.gimac.des.iam.domain.compte.Compte;
import cm.gimac.des.iam.domain.organisation.TypeUnite;
import cm.gimac.des.iam.domain.organisation.Unite;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * Règles de l'organisation du GIMAC (D-76, docs/lot1/mon-activite.md §1), service de domaine pur.
 *
 * <ul>
 *   <li>Unité : code {@code ^[A-Z][A-Z0-9_]{1,31}$}, libellé de 1 à 120 caractères, type obligatoire ;
 *       une direction n'a pas de parent ; le parent existe, est actif pour une unité active, et la
 *       hiérarchie est sans cycle ; une unité qui a des sous-unités actives ne peut être désactivée.</li>
 *   <li>Rattachement : un compte d'institution n'a ni unité ni responsable ; l'unité existe et est
 *       active ; le responsable est un compte interne actif, distinct du titulaire, et le titulaire
 *       n'est pas dans la chaîne de ses propres responsables.</li>
 * </ul>
 */
public final class ReglesOrganisation {

    /** Nombre de responsables successifs rendus dans la chaîne d'un compte. */
    public static final int CHAINE_MAX = 10;
    /** Nombre de comptes rendus dans l'équipe (récursive) d'un compte. */
    public static final int EQUIPE_MAX = 500;
    /** Profondeur de parcours de la chaîne pour la détection d'un cycle. */
    static final int PROFONDEUR_MAX = 100;
    private static final Pattern CODE = Pattern.compile("^[A-Z][A-Z0-9_]{1,31}$");

    private ReglesOrganisation() {}

    // ------------------------------------------------------------------ unités

    /** Vérifie les champs d'une unité et la rend normalisée (libellé rogné, parent vide ramené à nul). */
    public static Unite normaliser(Unite u) {
        if (u.code() == null || !CODE.matcher(u.code()).matches()) {
            throw invalide("Code d'unité invalide : majuscules, chiffres ou souligné, 2 à 32 caractères", "code", u.code());
        }
        String libelle = u.libelle() == null ? "" : u.libelle().trim();
        if (libelle.isEmpty() || libelle.length() > 120) {
            throw invalide("Libellé obligatoire, 120 caractères au plus", "libelle", u.libelle());
        }
        if (u.type() == null) {
            throw invalide("Type d'unité obligatoire", "type", null);
        }
        String parent = u.parent() == null || u.parent().isBlank() ? null : u.parent().trim();
        if (u.type() == TypeUnite.DIRECTION && parent != null) {
            throw invalide("Une direction n'a pas d'unité parente", "parent", parent);
        }
        String responsable = u.responsableId() == null || u.responsableId().isBlank() ? null : u.responsableId().trim();
        return new Unite(u.code(), libelle, u.type(), parent, u.active(), responsable);
    }

    /**
     * Vérifie le responsable d'une unité (D-81, docs/lot1/pouvoirs-hierarchiques.md §2) : compte interne
     * actif, sinon 422 ORG_RESPONSABLE_INVALIDE. Nul : l'unité n'a pas de responsable.
     */
    public static void verifierResponsableUnite(String responsableId, Function<String, Optional<Compte>> lecteur) {
        if (responsableId == null) {
            return;
        }
        lecteur.apply(responsableId).filter(r -> r.interne() && r.actif()).orElseThrow(() ->
            IamException.nonTraitable("ORG_RESPONSABLE_INVALIDE", "Le responsable d'une unité doit être un compte interne actif", Map.of("responsableId", responsableId))
        );
    }

    /**
     * Unités dirigées par un compte (§1, §2) : celles dont il est le responsable, et toutes leurs
     * sous-unités ; codes triés, sans doublon.
     */
    public static List<String> unitesDirigees(String compteId, Collection<Unite> catalogue) {
        Set<String> r = new java.util.TreeSet<>();
        java.util.Deque<String> a = new java.util.ArrayDeque<>();
        catalogue.stream().filter(u -> compteId != null && compteId.equals(u.responsableId())).map(Unite::code).forEach(a::add);
        while (!a.isEmpty()) {
            String code = a.poll();
            if (r.add(code)) {
                catalogue.stream().filter(u -> code.equals(u.parent())).map(Unite::code).forEach(a::add);
            }
        }
        return List.copyOf(r);
    }

    /**
     * Une unité et toutes ses sous-unités, actives ou non (D-86, membres d'une unité pour le moteur de
     * processus) ; codes triés, l'unité elle-même comprise, sans cycle.
     */
    public static List<String> sousUnites(String code, Collection<Unite> catalogue) {
        Set<String> r = new java.util.TreeSet<>();
        java.util.Deque<String> a = new java.util.ArrayDeque<>(List.of(code));
        while (!a.isEmpty()) {
            String c = a.poll();
            if (r.add(c)) {
                catalogue.stream().filter(u -> c.equals(u.parent())).map(Unite::code).forEach(a::add);
            }
        }
        return List.copyOf(r);
    }

    /**
     * Vérifie la place d'une unité dans le catalogue (où elle est absente ou dans son état précédent) :
     * parent connu, actif si l'unité l'est, sans cycle ; pas de désactivation avec des sous-unités actives.
     */
    public static void verifierPlace(Unite u, Map<String, Unite> catalogue) {
        if (u.parent() != null) {
            Unite parent = catalogue.get(u.parent());
            if (parent == null || (u.active() && !parent.active())) {
                throw IamException.nonTraitable("ORG_UNITE_INCONNUE", "Unité parente inconnue ou inactive", Map.of("parent", u.parent()));
            }
            Set<String> vus = new HashSet<>();
            for (Unite p = parent; p != null && vus.add(p.code()); p = p.parent() == null ? null : catalogue.get(p.parent())) {
                if (p.code().equals(u.code())) {
                    throw IamException.conflit("ORG_CYCLE", "L'unité ne peut être placée sous l'une de ses sous-unités", Map.of("code", u.code(), "parent", u.parent()));
                }
            }
        }
        if (!u.active()) {
            List<String> actives = sousUnitesActives(u.code(), catalogue.values());
            if (!actives.isEmpty()) {
                throw IamException.conflit("ORG_UNITE_SOUS_UNITES_ACTIVES", "Désactiver d'abord les sous-unités actives", Map.of("sousUnites", actives));
            }
        }
    }

    private static List<String> sousUnitesActives(String code, Collection<Unite> catalogue) {
        return catalogue.stream().filter(x -> x.active() && code.equals(x.parent())).map(Unite::code).sorted().toList();
    }

    /** Unités de la plus haute à la plus fine jusqu'à l'unité donnée ; vide si elle est inconnue. */
    public static List<Unite> chemin(String code, Map<String, Unite> catalogue) {
        List<Unite> l = new ArrayList<>();
        Set<String> vus = new HashSet<>();
        for (Unite u = code == null ? null : catalogue.get(code); u != null && vus.add(u.code()); u = u.parent() == null ? null : catalogue.get(u.parent())) {
            l.add(0, u);
        }
        return l;
    }

    // ------------------------------------------------------------------ rattachement

    /**
     * Vérifie le rattachement demandé pour un compte (§1.2). Le maintien de l'unité actuelle du compte
     * reste admis si elle a été désactivée depuis (ses rattachements restent lisibles).
     *
     * @param lecteur lecture d'un compte par son identifiant
     */
    public static void verifierRattachement(Compte titulaire, String unite, String responsableId, Map<String, Unite> catalogue, Function<String, Optional<Compte>> lecteur) {
        if (!titulaire.interne() && (unite != null || responsableId != null)) {
            throw IamException.nonTraitable("ORG_COMPTE_EXTERNE", "Un compte d'institution n'a ni unité ni responsable", Map.of("id", titulaire.id()));
        }
        if (unite != null) {
            Unite u = catalogue.get(unite);
            if (u == null || (!u.active() && !unite.equals(titulaire.unite()))) {
                throw IamException.nonTraitable("ORG_UNITE_INCONNUE", "Unité inconnue ou inactive", Map.of("unite", unite));
            }
        }
        if (responsableId == null) {
            return;
        }
        Compte responsable = (titulaire.id().equals(responsableId) ? Optional.<Compte>empty() : lecteur.apply(responsableId)).filter(r -> r.interne() && r.actif()).orElseThrow(() ->
            IamException.nonTraitable("ORG_RESPONSABLE_INVALIDE", "Le responsable doit être un compte interne actif, distinct du titulaire", Map.of("responsableId", responsableId))
        );
        Set<String> vus = new HashSet<>();
        Compte c = responsable;
        for (int i = 0; i < PROFONDEUR_MAX && c != null && vus.add(c.id()); i++) {
            String suivant = c.responsableId();
            if (suivant == null) {
                return;
            }
            if (suivant.equals(titulaire.id())) {
                throw IamException.conflit("ORG_CYCLE", "Le titulaire figure dans la chaîne de ses propres responsables", Map.of("responsableId", responsableId));
            }
            c = lecteur.apply(suivant).orElse(null);
        }
    }

    /** Responsables successifs d'un compte (n+1, n+2…), 10 au plus, arrêtés à un cycle ou à un compte absent. */
    public static List<String> chaine(Compte compte, Function<String, Optional<Compte>> lecteur) {
        List<String> l = new ArrayList<>();
        Set<String> vus = new HashSet<>(Set.of(compte.id()));
        String suivant = compte.responsableId();
        while (suivant != null && l.size() < CHAINE_MAX && vus.add(suivant)) {
            l.add(suivant);
            suivant = lecteur.apply(suivant).map(Compte::responsableId).orElse(null);
        }
        return l;
    }

    private static IamException invalide(String message, String champ, String valeur) {
        return IamException.invalide("ORG_UNITE_INVALIDE", message, Map.of("champ", champ, "valeur", String.valueOf(valeur)));
    }
}
