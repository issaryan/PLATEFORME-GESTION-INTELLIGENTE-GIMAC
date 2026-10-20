package cm.gimac.des.iam.domain.service;

import cm.gimac.des.iam.domain.IamException;
import cm.gimac.des.iam.domain.compte.Compte;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Administration déléguée des comptes d'une institution dans la sandbox (D-115, docs/lot1/sandbox-existant-adapte.md §4
 * US-EXI-03, algorithme 6.1 du CC de la sandbox), service de domaine pur.
 *
 * <ul>
 *   <li>L'administrateur de l'institution (SBX_HAB_ADMINISTRATION) n'agit que dans son institution (institution_id de
 *       son jeton) : un compte d'une autre institution reçoit la même réponse qu'un compte inexistant (404
 *       IAM_ACCOUNT_NOT_FOUND), sans révéler son existence (BNF-02).</li>
 *   <li>Rôles délégables : les profils externes dont la composition porte au moins une permission de la sandbox ; aucun
 *       nom de profil (D-94). Un autre profil : 403 IAM_SBX_ROLE_HORS_PORTEE.</li>
 *   <li>Jamais sur son propre compte (403 IAM_SELF_ADMINISTRATION, règle de la plateforme).</li>
 *   <li>RG-15 : l'institution garde toujours un administrateur actif ; retirer le droit d'administration ou suspendre
 *       le dernier : 409 IAM_SBX_DERNIER_ADMINISTRATEUR.</li>
 * </ul>
 */
public final class ReglesAdministrationDeleguee {

    /** Permission de l'administrateur de l'institution (habilitations, administration). */
    public static final String ADMINISTRATION = "SBX_HAB_ADMINISTRATION";

    private ReglesAdministrationDeleguee() {}

    /** Institution de l'acteur, obligatoire : un compte du GIMAC n'administre pas un espace d'institution (403). */
    public static String institution(String institutionId) {
        if (institutionId == null || institutionId.isBlank()) {
            throw IamException.interdit("IAM_SBX_SANS_INSTITUTION", "L'administration déléguée est réservée à l'administrateur d'une institution");
        }
        return institutionId.strip();
    }

    /** Compte du périmètre de l'institution ; hors périmètre ou inexistant : 404 indistinguable. */
    public static Compte dansPerimetre(Compte compte, String institution, String id) {
        if (compte == null || !institution.equals(compte.institutionId())) {
            throw IamException.introuvable("IAM_ACCOUNT_NOT_FOUND", id);
        }
        return compte;
    }

    /** Profils externes composant au moins une permission de la sandbox. */
    public static Set<String> rolesDelegables(Set<String> externes, Map<String, Set<String>> compositions) {
        Set<String> s = new TreeSet<>();
        for (String p : externes) {
            if (compositions.getOrDefault(p, Set.of()).stream().anyMatch(ReglesSandbox::sandbox)) {
                s.add(p);
            }
        }
        return s;
    }

    /** Rôles demandés tous délégables (403 IAM_SBX_ROLE_HORS_PORTEE avec les rôles refusés) ; au moins un si exigé (400). */
    public static void verifierRoles(Collection<String> demandes, Set<String> delegables, boolean auMoinsUn) {
        if (auMoinsUn && (demandes == null || demandes.isEmpty())) {
            throw IamException.invalide("IAM_SBX_ROLE_REQUIS", "Au moins un rôle de la sandbox", Map.of());
        }
        TreeSet<String> hors = new TreeSet<>(demandes == null ? Set.of() : demandes);
        hors.removeAll(delegables);
        if (!hors.isEmpty()) {
            throw IamException.interdit("IAM_SBX_ROLE_HORS_PORTEE", "Rôle hors de la portée de l'institution", Map.of("profils", List.copyOf(hors)));
        }
    }

    /**
     * Profils du compte après l'administration déléguée : ses profils non délégables restent (correspondant de la
     * plateforme, par exemple), ses rôles de la sandbox sont remplacés par les rôles demandés.
     */
    public static Set<String> profilsApres(Set<String> actuels, Collection<String> roles, Set<String> delegables) {
        Set<String> s = new TreeSet<>(actuels);
        s.removeAll(delegables);
        s.addAll(roles);
        return s;
    }

    /** Le compte administre son institution par l'un de ses profils. */
    public static boolean administrateur(Set<String> profils, Map<String, Set<String>> compositions) {
        return profils.stream().anyMatch(p -> compositions.getOrDefault(p, Set.of()).contains(ADMINISTRATION));
    }

    /**
     * RG-15 : refuse de laisser l'institution sans administrateur actif. {@code perd} : le compte, administrateur actif,
     * cesse de l'être (rôle retiré ou suspension) ; {@code administrateursActifs} : nombre actuel dans l'institution.
     */
    public static void verifierDernierAdministrateur(boolean perd, long administrateursActifs) {
        if (perd && administrateursActifs <= 1) {
            throw IamException.conflit(
                "IAM_SBX_DERNIER_ADMINISTRATEUR",
                "L'institution garde au moins un administrateur actif",
                Map.of("administrateursActifs", administrateursActifs)
            );
        }
    }
}
