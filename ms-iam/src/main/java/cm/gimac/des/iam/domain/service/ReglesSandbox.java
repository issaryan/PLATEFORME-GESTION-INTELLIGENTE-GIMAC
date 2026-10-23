package cm.gimac.des.iam.domain.service;

import cm.gimac.des.iam.domain.IamException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.regex.Pattern;

/**
 * Permissions de la Sandbox GIMAC dans la matrice de MS-IAM (D-115, docs/lot1/sandbox-existant-adapte.md §2, US-EXI-02),
 * service de domaine pur.
 *
 * <ul>
 *   <li>Format {@code SBX_<MODULE>_<FONCTION>} : douze modules (HAB, RAC, REF, SCN, DON, SIM, MOB, ISO, CAR, CER, TRA,
 *       LEC) et cinq fonctions (ADMINISTRATION, SIMULATION, EXECUTION, VALIDATION, CONSULTATION), CA-SBX-GIMAC-02
 *       UC-INS-04.</li>
 *   <li>RG-05 : la validation des verdicts ({@code SBX_*_VALIDATION}) reste au GIMAC ; elle n'est jamais portée par un
 *       profil externe (portée institution ou partenaire). Refus : 422 IAM_SBX_VALIDATION_RESERVEE, sur la composition
 *       d'un profil externe comme sur le passage en externe d'un profil qui la porte.</li>
 * </ul>
 */
public final class ReglesSandbox {

    public static final String PREFIXE = "SBX_";
    /** Douze modules de la matrice complète (cinq fonctions chacun). */
    public static final List<String> MODULES = List.of("HAB", "RAC", "REF", "SCN", "DON", "SIM", "MOB", "ISO", "CAR", "CER", "TRA", "LEC");
    public static final List<String> FONCTIONS = List.of("ADMINISTRATION", "SIMULATION", "EXECUTION", "VALIDATION", "CONSULTATION");
    /**
     * Treizième module (D-121, D-119) : supervision de la sandbox depuis la plateforme, avec la seule consultation,
     * attribuée par profil, aucun profil livré ne la recevant d'office.
     */
    public static final String SUPERVISION = "SBX_SUP_CONSULTATION";
    /** Soixante et une permissions : les soixante de la matrice complète et la supervision. */
    public static final List<String> PERMISSIONS = java.util.stream.Stream.concat(
        MODULES.stream().flatMap(m -> FONCTIONS.stream().map(f -> PREFIXE + m + "_" + f)),
        java.util.stream.Stream.of(SUPERVISION)
    ).toList();
    private static final Pattern FORMAT = Pattern.compile(
        "^SBX_((" + String.join("|", MODULES) + ")_(" + String.join("|", FONCTIONS) + ")|SUP_CONSULTATION)$"
    );

    private ReglesSandbox() {}

    /** Permission de la sandbox (préfixe SBX_), quel que soit son format. */
    public static boolean sandbox(String permission) {
        return permission != null && permission.startsWith(PREFIXE);
    }

    /** Permission SBX_<MODULE>_<FONCTION> conforme. */
    public static boolean conforme(String permission) {
        return permission != null && FORMAT.matcher(permission).matches();
    }

    /** Fonction de validation de verdict d'un module de la sandbox. */
    public static boolean validation(String permission) {
        return conforme(permission) && permission.endsWith("_VALIDATION");
    }

    /**
     * Refuse une composition externe portant une validation de verdict (RG-05) : 422 IAM_SBX_VALIDATION_RESERVEE, avec
     * les permissions en cause.
     */
    public static void verifierValidationReservee(boolean externe, Collection<String> permissions) {
        if (!externe || permissions == null) {
            return;
        }
        TreeSet<String> enCause = new TreeSet<>();
        permissions.stream().filter(ReglesSandbox::validation).forEach(enCause::add);
        if (!enCause.isEmpty()) {
            throw IamException.nonTraitable(
                "IAM_SBX_VALIDATION_RESERVEE",
                "La validation des verdicts de la sandbox reste aux profils du GIMAC",
                Map.of("permissions", List.copyOf(enCause))
            );
        }
    }

    /** Permissions retenues par le filtre de préfixe (absent ou blanc : toutes), dans leur ordre. */
    public static List<String> filtrer(List<String> permissions, String prefixe) {
        if (prefixe == null || prefixe.isBlank()) {
            return permissions;
        }
        String p = prefixe.strip().toUpperCase(java.util.Locale.ROOT);
        return permissions.stream().filter(x -> x.startsWith(p)).toList();
    }
}
