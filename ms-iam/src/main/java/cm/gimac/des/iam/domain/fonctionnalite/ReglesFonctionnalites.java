package cm.gimac.des.iam.domain.fonctionnalite;

import cm.gimac.des.iam.domain.IamException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

/**
 * Règles du catalogue des fonctionnalités (D-94, docs/lot1/ecrans-par-configuration.md §2), service de domaine pur.
 *
 * <ul>
 *   <li>Applications : des, console, portail ; l'application d'une fonctionnalité est immuable.</li>
 *   <li>Toute permission citée (prédicat, widgets, droits) existe dans le catalogue des permissions du domaine
 *       (GET /configuration/permissions) ; une permission INTERNAL_* (jetons de service) n'ouvre aucun écran.</li>
 *   <li>Formes : libellé de 1 à 200 caractères ; groupe et module en camelCase (groupe nul : hors barre latérale) ;
 *       icône en minuscules et tirets ; ordre de 0 à 100 000 ; clés de widget et de droit uniques.</li>
 * </ul>
 */
public final class ReglesFonctionnalites {

    public static final Set<String> APPLICATIONS = Set.of("des", "console", "portail");
    public static final int LIBELLE_MAX = 200;
    public static final int ORDRE_MAX = 100_000;
    private static final Pattern CLE = Pattern.compile("^[a-z][A-Za-z0-9]{0,59}$");
    private static final Pattern GROUPE = Pattern.compile("^[a-z][A-Za-z0-9]{0,39}$");
    private static final Pattern ICONE = Pattern.compile("^[a-z0-9][a-z0-9-]{0,39}$");
    private static final Pattern CLE_DROIT = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9_.:-]{0,79}$");

    private ReglesFonctionnalites() {}

    /**
     * Forme d'une fonctionnalité (valeurs de départ comme modifications) : 400 IAM_FONCTIONNALITE_INVALIDE avec le
     * champ en cause.
     */
    public static void verifierForme(Fonctionnalite f) {
        exiger(f.cle() != null && CLE.matcher(f.cle()).matches(), "cle");
        exiger(f.application() != null && APPLICATIONS.contains(f.application()), "application");
        exiger(f.module() != null && GROUPE.matcher(f.module()).matches(), "module");
        exiger(f.libelle() != null && !f.libelle().isBlank() && f.libelle().strip().length() <= LIBELLE_MAX, "libelle");
        exiger(f.groupe() == null || GROUPE.matcher(f.groupe()).matches(), "groupe");
        exiger(f.ordre() >= 0 && f.ordre() <= ORDRE_MAX, "ordre");
        exiger(f.icone() != null && ICONE.matcher(f.icone()).matches(), "icone");
        Set<String> widgets = new HashSet<>();
        for (int i = 0; i < f.widgets().size(); i++) {
            Fonctionnalite.Widget w = f.widgets().get(i);
            exiger(w != null && w.cle() != null && GROUPE.matcher(w.cle()).matches() && widgets.add(w.cle()), "widgets[" + i + "].cle");
        }
        Set<String> droits = new HashSet<>();
        for (int i = 0; i < f.droits().size(); i++) {
            Fonctionnalite.Droit d = f.droits().get(i);
            exiger(d != null && d.cle() != null && CLE_DROIT.matcher(d.cle()).matches() && droits.add(d.cle()), "droits[" + i + "].cle");
            exiger(d.nature() != null, "droits[" + i + "].nature");
            exiger(d.permission() != null && !d.permission().isBlank(), "droits[" + i + "].permission");
            exiger(d.libelle() == null || d.libelle().length() <= LIBELLE_MAX, "droits[" + i + "].libelle");
        }
    }

    /**
     * Modification d'une fonctionnalité connue : forme, application inchangée (422
     * IAM_FONCTIONNALITE_APPLICATION_IMMUABLE), permissions connues (400 IAM_PERMISSION_UNKNOWN, détail
     * permissions), aucune permission interne (422 IAM_FONCTIONNALITE_PERMISSION_INTERNE).
     */
    public static void verifierModification(Fonctionnalite avant, Fonctionnalite apres, Collection<String> permissionsConnues) {
        verifierForme(apres);
        if (!avant.application().equals(apres.application())) {
            throw IamException.nonTraitable(
                "IAM_FONCTIONNALITE_APPLICATION_IMMUABLE",
                "L'application d'une fonctionnalité ne change pas",
                Map.of("avant", avant.application(), "apres", apres.application())
            );
        }
        Set<String> citees = permissionsCitees(apres);
        Set<String> internes = new TreeSet<>();
        citees.stream().filter(p -> p.startsWith("INTERNAL_")).forEach(internes::add);
        if (!internes.isEmpty()) {
            throw IamException.nonTraitable(
                "IAM_FONCTIONNALITE_PERMISSION_INTERNE",
                "Une permission interne n'ouvre aucun écran",
                Map.of("permissions", List.copyOf(internes))
            );
        }
        Set<String> inconnues = new TreeSet<>(citees);
        inconnues.removeAll(new HashSet<>(permissionsConnues));
        if (!inconnues.isEmpty()) {
            throw IamException.invalide("IAM_PERMISSION_UNKNOWN", "Permission inconnue", Map.of("permissions", List.copyOf(inconnues)));
        }
    }

    /** Toutes les permissions citées : prédicat, prédicats des widgets, permissions des droits. */
    public static Set<String> permissionsCitees(Fonctionnalite f) {
        Set<String> s = new TreeSet<>();
        s.addAll(f.permissions().tous());
        s.addAll(f.permissions().unDe());
        f.widgets().forEach(w -> {
            s.addAll(w.permissions().tous());
            s.addAll(w.permissions().unDe());
        });
        f.droits().forEach(d -> s.add(d.permission()));
        return s;
    }

    /** Vrai si les permissions ouvrent le prédicat : toutes de {@code tous}, l'une de {@code unDe} s'il n'est pas vide. */
    public static boolean satisfait(Fonctionnalite.Predicat p, Collection<String> permissions) {
        return permissions.containsAll(p.tous()) && (p.unDe().isEmpty() || p.unDe().stream().anyMatch(permissions::contains));
    }

    /**
     * Résumés avant et après des seuls champs modifiés, pour le journal (« libelle=… ; ordre=… »), dans l'ordre des
     * champs ; vides si rien ne change.
     */
    public static List<String> differences(Fonctionnalite avant, Fonctionnalite apres) {
        Map<String, Object[]> champs = new java.util.LinkedHashMap<>();
        champs.put("module", new Object[] { avant.module(), apres.module() });
        champs.put("libelle", new Object[] { avant.libelle(), apres.libelle() });
        champs.put("groupe", new Object[] { avant.groupe(), apres.groupe() });
        champs.put("ordre", new Object[] { avant.ordre(), apres.ordre() });
        champs.put("icone", new Object[] { avant.icone(), apres.icone() });
        champs.put("visible", new Object[] { avant.visible(), apres.visible() });
        champs.put("permissions", new Object[] { predicat(avant.permissions()), predicat(apres.permissions()) });
        champs.put("widgets", new Object[] { widgets(avant.widgets()), widgets(apres.widgets()) });
        champs.put("droits", new Object[] { droits(avant.droits()), droits(apres.droits()) });
        List<String> a = new ArrayList<>();
        List<String> b = new ArrayList<>();
        champs.forEach((nom, v) -> {
            if (!Objects.equals(v[0], v[1])) {
                a.add(nom + "=" + v[0]);
                b.add(nom + "=" + v[1]);
            }
        });
        return List.of(String.join(" ; ", a), String.join(" ; ", b));
    }

    private static String predicat(Fonctionnalite.Predicat p) {
        return "tous" + p.tous() + " unDe" + p.unDe();
    }

    private static String widgets(List<Fonctionnalite.Widget> l) {
        Set<String> s = new LinkedHashSet<>();
        l.forEach(w -> s.add(w.cle() + "(" + predicat(w.permissions()) + ")"));
        return s.toString();
    }

    private static String droits(List<Fonctionnalite.Droit> l) {
        Set<String> s = new LinkedHashSet<>();
        l.forEach(d -> s.add(d.nature() + ":" + d.permission() + (d.libelle() == null ? "" : "(" + d.libelle() + ")")));
        return s.toString();
    }

    private static void exiger(boolean condition, String champ) {
        if (!condition) {
            throw IamException.invalide("IAM_FONCTIONNALITE_INVALIDE", "Fonctionnalité invalide : " + champ, Map.of("champ", champ));
        }
    }
}
