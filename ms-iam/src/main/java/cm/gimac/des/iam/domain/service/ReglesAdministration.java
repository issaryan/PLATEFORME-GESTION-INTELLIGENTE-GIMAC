package cm.gimac.des.iam.domain.service;

import cm.gimac.des.iam.domain.IamException;
import cm.gimac.des.iam.domain.compte.Acteur;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

/**
 * Règles de l'administration des comptes (D-62), service de domaine pur.
 *
 * <ul>
 *   <li>Séparation des pouvoirs : un administrateur n'agit pas sur son propre compte (profils,
 *       activation, suppression, second facteur) ; il passe par un pair.</li>
 *   <li>Seuls les profils du catalogue sont attribuables ; un profil privilégié (super
 *       administrateur, RSSI) exige IAM_ROLE_PRIVILEGED_GRANT.</li>
 *   <li>La plateforme garde toujours au moins un super administrateur actif.</li>
 *   <li>Seul un correspondant de participant est rattaché à une institution ; un agent du GIMAC
 *       n'en a pas (sinon il serait traité en compte externe : portail, absent de la messagerie).</li>
 * </ul>
 */
public final class ReglesAdministration {

    public static final String SUPER_ADMIN = "ROLE_SUPER_ADMIN";
    public static final String OCTROI_PRIVILEGIE = "IAM_ROLE_PRIVILEGED_GRANT";
    public static final String CORRESPONDANT = "ROLE_CORRESPONDANT";
    private static final Pattern IDENTIFIANT = Pattern.compile("^[a-z][a-z0-9._-]{2,49}$");
    private static final Pattern TELEPHONE = Pattern.compile("^\\+?[0-9 ]{8,20}$");

    private ReglesAdministration() {}

    public static void verifierPasSoiMeme(Acteur acteur, String compteId) {
        if (acteur.id().equals(compteId)) {
            throw IamException.interdit("IAM_SELF_ADMINISTRATION", "Un administrateur n'agit pas sur son propre compte");
        }
    }

    public static void verifierIdentifiant(String identifiant) {
        if (identifiant == null || !IDENTIFIANT.matcher(identifiant).matches()) {
            throw IamException.invalide(
                "IAM_USERNAME_INVALID",
                "Identifiant invalide : minuscules, chiffres, point, tiret ou souligné, 3 à 50 caractères",
                Map.of("identifiant", String.valueOf(identifiant))
            );
        }
    }

    public static void verifierTelephone(String telephone) {
        if (telephone != null && !telephone.isBlank() && !TELEPHONE.matcher(telephone).matches()) {
            throw IamException.invalide("IAM_PHONE_INVALID", "Numéro de téléphone invalide", Map.of("telephone", telephone));
        }
    }

    /**
     * Vérifie les profils demandés : tous au catalogue ; les profils privilégiés ajoutés ou retirés
     * exigent le droit d'octroi privilégié.
     */
    public static void verifierProfils(Acteur acteur, Set<String> actuels, Set<String> demandes, Set<String> catalogue, Set<String> privilegies) {
        Set<String> inconnus = new TreeSet<>(demandes);
        inconnus.removeAll(catalogue);
        if (!inconnus.isEmpty()) {
            throw IamException.invalide("IAM_PROFILE_UNKNOWN", "Profil inconnu du catalogue", Map.of("profils", List.copyOf(inconnus)));
        }
        Set<String> changes = new TreeSet<>(demandes);
        changes.addAll(actuels);
        changes.removeIf(p -> demandes.contains(p) && actuels.contains(p));
        boolean touchePrivilegie = changes.stream().anyMatch(privilegies::contains);
        if (touchePrivilegie && !acteur.peut(OCTROI_PRIVILEGIE)) {
            throw IamException.interdit("IAM_PRIVILEGED_PROFILE", "L'attribution d'un profil privilégié est réservée au super administrateur");
        }
    }

    /**
     * Rattachement à une institution : réservé au correspondant de participant, et obligatoire pour lui.
     * Un compte interne du GIMAC n'a pas d'institution (le GIMAC n'en est pas une) : une valeur saisie
     * par erreur l'enverrait vers le portail et le retirerait de l'annuaire interne de la messagerie.
     */
    public static void verifierRattachement(Set<String> profils, String institutionId) {
        boolean rattache = institutionId != null && !institutionId.isBlank();
        boolean correspondant = profils.contains(CORRESPONDANT);
        if (rattache && profils.stream().anyMatch(p -> !CORRESPONDANT.equals(p))) {
            throw IamException.invalide(
                "IAM_INSTITUTION_INTERNAL_ACCOUNT",
                "Un compte interne du GIMAC n'est rattaché à aucune institution : laisser l'institution vide",
                Map.of("institutionId", institutionId)
            );
        }
        if (correspondant && !rattache) {
            throw IamException.invalide("IAM_INSTITUTION_REQUIRED", "Un correspondant de participant est rattaché à son institution", Map.of());
        }
    }

    /** Refuse de retirer ou de désactiver le dernier super administrateur actif. */
    public static void verifierDernierSuperAdmin(boolean cibleEstSuperAdminActif, long superAdminsActifs) {
        if (cibleEstSuperAdminActif && superAdminsActifs <= 1) {
            throw IamException.conflit("IAM_LAST_SUPER_ADMIN", "La plateforme doit garder au moins un super administrateur actif", Map.of());
        }
    }

    public static void exigerMotif(String motif) {
        if (motif == null || motif.isBlank()) {
            throw IamException.invalide("IAM_REASON_REQUIRED", "Motif obligatoire", Map.of());
        }
    }
}
