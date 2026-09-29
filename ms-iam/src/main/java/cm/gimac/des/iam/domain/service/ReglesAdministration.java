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
 * </ul>
 */
public final class ReglesAdministration {

    public static final String SUPER_ADMIN = "ROLE_SUPER_ADMIN";
    public static final String OCTROI_PRIVILEGIE = "IAM_ROLE_PRIVILEGED_GRANT";
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
