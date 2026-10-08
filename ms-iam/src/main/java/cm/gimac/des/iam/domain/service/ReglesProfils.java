package cm.gimac.des.iam.domain.service;

import cm.gimac.des.iam.domain.IamException;
import cm.gimac.des.iam.domain.compte.Acteur;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

/**
 * Règles des profils créés par l'administration (D-81, docs/lot1/pouvoirs-hierarchiques.md §5), service
 * de domaine pur.
 *
 * <ul>
 *   <li>Code {@code ^ROLE_[A-Z][A-Z0-9_]{2,40}$}, unique.</li>
 *   <li>Une permission INTERNAL_* (jetons de service) est interdite à un profil de personne (422).</li>
 *   <li>Une permission IAM_* ou PLATFORM_* rend le profil privilégié : créer, recomposer, supprimer ou
 *       attribuer un tel profil est réservé au détenteur de IAM_ROLE_PRIVILEGED_GRANT.</li>
 *   <li>Tout profil créé reçoit la messagerie, Mon activité et les notifications.</li>
 *   <li>Libellé de 1 à 80 caractères, description de 0 à 500 (D-86, docs/lot1/profils-organisation.md §2) ;
 *       le code est immuable.</li>
 *   <li>D-94 (docs/lot1/ecrans-par-configuration.md) : un profil est privilégié dès qu'il porte une permission
 *       IAM_* ou PLATFORM_*, livré ou créé ; il est externe (compte d'institution, portail) selon son attribut
 *       gimac_externe, ou à défaut s'il porte une permission du portail (*_PORTAL_*) ; un compte porte l'octroi
 *       privilégié quand l'un de ses profils compose IAM_ROLE_PRIVILEGED_GRANT. Aucun nom de profil.</li>
 * </ul>
 */
public final class ReglesProfils {

    /** Permissions ajoutées à tout profil créé (comme aux profils livrés). */
    public static final Set<String> PERMISSIONS_COMMUNES = Set.of("COL_MESSAGE_READ", "COL_MESSAGE_SEND", "PIL_ACTIVITY_OWN", "NOT_INBOX_READ");
    private static final Pattern CODE = Pattern.compile("^ROLE_[A-Z][A-Z0-9_]{2,40}$");

    private ReglesProfils() {}

    public static void verifierCode(String code) {
        if (code == null || !CODE.matcher(code).matches()) {
            throw IamException.invalide("IAM_PROFIL_CODE_INVALIDE", "Code de profil invalide : ROLE_ suivi de majuscules, chiffres ou souligné", Map.of("code", String.valueOf(code)));
        }
    }

    /** Repère d'une permission du portail des participants (HOM_PORTAL_*, CER_PORTAL_*…). */
    public static final String REPERE_PORTAIL = "_PORTAL_";

    /** Un profil portant une permission IAM_* ou PLATFORM_* est privilégié. */
    public static boolean privilegie(Collection<String> permissions) {
        return permissions.stream().anyMatch(p -> p.startsWith("IAM_") || p.startsWith("PLATFORM_"));
    }

    /**
     * Profil externe (D-94) : la valeur de son attribut gimac_externe quand il est posé ; à défaut (attribut
     * absent), règle de repli : le profil porte une permission du portail (*_PORTAL_*).
     */
    public static boolean externe(Boolean attribut, Collection<String> permissions) {
        if (attribut != null) {
            return attribut;
        }
        return permissions != null && permissions.stream().anyMatch(p -> p.contains(REPERE_PORTAIL));
    }

    /**
     * Profils dont la composition porte cette permission (D-94 §3, porteurs d'une permission) ; vide pour une
     * permission nulle ou blanche.
     */
    public static Set<String> profilsPortant(String permission, Map<String, Set<String>> compositions) {
        Set<String> s = new TreeSet<>();
        if (permission == null || permission.isBlank()) {
            return s;
        }
        compositions.forEach((profil, permissions) -> {
            if (permissions != null && permissions.contains(permission)) {
                s.add(profil);
            }
        });
        return s;
    }

    /** Permissions d'un compte : réunion des compositions de ses profils (profil inconnu : aucune). */
    public static Set<String> permissionsDe(Collection<String> profils, Map<String, Set<String>> compositions) {
        Set<String> s = new TreeSet<>();
        profils.forEach(p -> s.addAll(compositions.getOrDefault(p, Set.of())));
        return s;
    }

    /** Le compte porte l'octroi des profils privilégiés (IAM_ROLE_PRIVILEGED_GRANT) par l'un de ses profils. */
    public static boolean porteOctroiPrivilegie(Collection<String> profils, Map<String, Set<String>> compositions) {
        return profils.stream().anyMatch(p -> compositions.getOrDefault(p, Set.of()).contains(ReglesAdministration.OCTROI_PRIVILEGIE));
    }

    /**
     * Vérifie la composition d'un profil de personne : aucune permission INTERNAL_* (422
     * IAM_PROFIL_PERMISSION_INTERNE), permissions connues (400 IAM_PERMISSION_UNKNOWN), droit d'octroi
     * privilégié pour une composition privilégiée (403 IAM_PRIVILEGED_PROFILE).
     */
    public static void verifierComposition(Acteur acteur, Collection<String> permissions, Collection<String> connues) {
        Set<String> internes = new TreeSet<>();
        permissions.stream().filter(p -> p.startsWith("INTERNAL_")).forEach(internes::add);
        if (!internes.isEmpty()) {
            throw IamException.nonTraitable("IAM_PROFIL_PERMISSION_INTERNE", "Une permission interne n'est jamais attribuée à un profil de personne", Map.of("permissions", List.copyOf(internes)));
        }
        Set<String> inconnues = new TreeSet<>(permissions);
        inconnues.removeAll(connues);
        if (!inconnues.isEmpty()) {
            throw IamException.invalide("IAM_PERMISSION_UNKNOWN", "Permission inconnue", Map.of("permissions", List.copyOf(inconnues)));
        }
        if (privilegie(permissions) && !acteur.peut(ReglesAdministration.OCTROI_PRIVILEGIE)) {
            throw IamException.interdit("IAM_PRIVILEGED_PROFILE", "Un profil privilégié est réservé au super administrateur");
        }
    }

    /** Libellé d'un profil (D-86, docs/lot1/profils-organisation.md §2) : 1 à 80 caractères. */
    public static final int LIBELLE_MAX = 80;
    /** Description d'un profil (D-86 §2) : 0 à 500 caractères. */
    public static final int DESCRIPTION_MAX = 500;

    /**
     * Libellé normalisé (espaces de tête et de fin retirés), 1 à 80 caractères ; sinon 400
     * IAM_PROFIL_INVALIDE (champ libelle).
     */
    public static String libelle(String libelle) {
        String l = libelle == null ? "" : libelle.strip();
        if (l.isEmpty() || l.length() > LIBELLE_MAX) {
            throw IamException.invalide("IAM_PROFIL_INVALIDE", "Libellé obligatoire, " + LIBELLE_MAX + " caractères au plus", Map.of("champ", "libelle"));
        }
        return l;
    }

    /**
     * Description normalisée (nulle : vide), 500 caractères au plus, obligatoire si demandé ; sinon 400
     * IAM_PROFIL_INVALIDE (champ description).
     */
    public static String description(String description, boolean obligatoire) {
        String d = description == null ? "" : description.strip();
        if ((obligatoire && d.isEmpty()) || d.length() > DESCRIPTION_MAX) {
            throw IamException.invalide("IAM_PROFIL_INVALIDE", (obligatoire ? "Description obligatoire, " : "Description de ") + DESCRIPTION_MAX + " caractères au plus", Map.of("champ", "description"));
        }
        return d;
    }

    /** Permissions d'un profil créé : celles demandées et les permissions communes. */
    public static Set<String> avecCommunes(Collection<String> permissions) {
        Set<String> s = new TreeSet<>(permissions);
        s.addAll(PERMISSIONS_COMMUNES);
        return s;
    }
}
