package cm.gimac.des.iam.domain.sandbox;

import cm.gimac.des.iam.domain.IamException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Suspension du périmètre d'une institution et protection du dernier administrateur (D-119, US-EXI-06, algorithme 6.2
 * du CC de la sandbox), service de domaine pur.
 *
 * <ul>
 *   <li>L'indicateur se superpose à l'état de chaque compte : un compte est effectivement actif s'il est actif et que
 *       son périmètre n'est pas suspendu. Aucun état de compte n'est modifié, d'où la restitution exacte au
 *       rétablissement.</li>
 *   <li>Un compte protégé (dernier administrateur actif, attribut posé par SBX-ESPACE) n'est ni suspendu par
 *       l'institution (409 IAM_SBX_COMPTE_PROTEGE, motif rendu) ni désactivé pour inactivité.</li>
 * </ul>
 */
public final class ReglesPerimetre {

    /** Code participant : lettres, chiffres, tiret et souligné, 64 caractères au plus (UUID compris, D-107). */
    private static final Pattern CODE = Pattern.compile("^[A-Za-z0-9_-]{1,64}$");

    /** Motif rendu au refus de suspendre un compte protégé. */
    public static final String MOTIF_PROTECTION = "Dernier administrateur actif de l'institution, protégé par MS-IAM";

    private ReglesPerimetre() {}

    /** Code de périmètre conforme ; sinon 400 IAM_SBX_PERIMETRE_INVALIDE. */
    public static String code(String code) {
        if (code == null || !CODE.matcher(code.strip()).matches()) {
            throw IamException.invalide("IAM_SBX_PERIMETRE_INVALIDE", "Code de périmètre invalide", Map.of("code", String.valueOf(code)));
        }
        return code.strip();
    }

    /** Compte effectivement actif : actif lui-même et périmètre non suspendu. */
    public static boolean actifEffectif(boolean compteActif, boolean perimetreSuspendu) {
        return compteActif && !perimetreSuspendu;
    }

    /** Refuse la suspension d'un compte protégé par l'institution : 409 IAM_SBX_COMPTE_PROTEGE avec le motif. */
    public static void verifierSuspensionAdmise(boolean protege) {
        if (protege) {
            throw IamException.conflit("IAM_SBX_COMPTE_PROTEGE", MOTIF_PROTECTION, Map.of("motif", MOTIF_PROTECTION));
        }
    }

    /** Refuse un acte de l'institution quand son périmètre est suspendu : 403 IAM_SBX_PERIMETRE_SUSPENDU. */
    public static void verifierPerimetreOuvert(boolean suspendu) {
        if (suspendu) {
            throw IamException.interdit("IAM_SBX_PERIMETRE_SUSPENDU", "Le périmètre de l'institution est suspendu");
        }
    }

    /**
     * Compte inactif : aucune connexion (ou, à défaut, aucune création) depuis {@code jours} jours révolus à
     * {@code maintenant}. Sans aucune date connue, le compte n'est pas jugé inactif.
     */
    public static boolean inactif(Instant derniereConnexion, Instant creeLe, Instant maintenant, int jours) {
        Instant reference = derniereConnexion != null ? derniereConnexion : creeLe;
        return reference != null && !reference.plus(Duration.ofDays(jours)).isAfter(maintenant);
    }
}
