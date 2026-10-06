package cm.gimac.des.iam.domain.service;

import cm.gimac.des.iam.domain.IamException;
import cm.gimac.des.iam.domain.pouvoir.ActionPouvoir;
import cm.gimac.des.iam.domain.pouvoir.Portee;
import cm.gimac.des.iam.domain.pouvoir.Pouvoirs;
import cm.gimac.des.iam.domain.pouvoir.PouvoirsProfil;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * Règles de la matrice des pouvoirs (D-81, docs/lot1/pouvoirs-hierarchiques.md §1 et §3), service de
 * domaine pur.
 *
 * <ul>
 *   <li>Un compte qui détient plusieurs profils a, pour chaque action, la portée la plus large ; ses
 *       profils demandables sont la réunion de ceux de ses profils.</li>
 *   <li>Un profil demandable est un profil attribuable et jamais un profil privilégié : un chef ne peut
 *       pas faire attribuer un profil d'administration (pas d'escalade), même si la matrice le portait.</li>
 * </ul>
 */
public final class ReglesPouvoirs {

    private ReglesPouvoirs() {}

    /**
     * Pouvoirs d'un compte d'après ses profils : portée maximale par action ; profils demandables
     * réunis, restreints aux profils attribuables non privilégiés.
     */
    public static Pouvoirs calculer(Collection<String> profils, Map<String, PouvoirsProfil> matrice, Set<String> attribuables, Set<String> privilegies) {
        Map<ActionPouvoir, Portee> actions = new EnumMap<>(ActionPouvoir.class);
        Set<String> demandables = new TreeSet<>();
        for (String profil : profils) {
            PouvoirsProfil ligne = matrice.get(profil);
            if (ligne == null) {
                continue;
            }
            ligne.actions().forEach((a, p) -> actions.merge(a, p, Portee::max));
            demandables.addAll(ligne.profilsDemandables());
        }
        demandables.removeIf(p -> !attribuables.contains(p) || privilegies.contains(p));
        return new Pouvoirs(actions, demandables);
    }

    /**
     * Vérifie une ligne de la matrice avant enregistrement : profils demandables connus (400
     * IAM_PROFILE_UNKNOWN) et non privilégiés (422 ORG_PROFIL_NON_DEMANDABLE).
     */
    public static void verifierLigne(PouvoirsProfil ligne, Set<String> attribuables, Set<String> privilegies) {
        Set<String> inconnus = new TreeSet<>(ligne.profilsDemandables());
        inconnus.removeAll(attribuables);
        if (!inconnus.isEmpty()) {
            throw IamException.invalide("IAM_PROFILE_UNKNOWN", "Profil inconnu du catalogue", Map.of("profils", List.copyOf(inconnus)));
        }
        Set<String> interdits = new TreeSet<>(ligne.profilsDemandables());
        interdits.retainAll(privilegies);
        if (!interdits.isEmpty()) {
            throw IamException.nonTraitable("ORG_PROFIL_NON_DEMANDABLE", "Un profil privilégié n'est jamais demandable", Map.of("profils", List.copyOf(interdits)));
        }
    }

    /** Refuse (403 ORG_HORS_PERIMETRE) une action que la portée du demandeur ne couvre pas. */
    public static void exigerCouverture(boolean couvert, ActionPouvoir action) {
        if (!couvert) {
            throw IamException.interdit("ORG_HORS_PERIMETRE", "Hors du périmètre de l'action " + action.name());
        }
    }

    /**
     * Rattachement d'un subordonné par son chef (D-81 §3, action RATTACHEMENT_MODIFIER), après contrôle de la
     * portée : jamais soi-même (403 IAM_SELF_ADMINISTRATION) ; cible couverte par le périmètre de l'action ;
     * nouvelle unité égale à l'unité actuelle de la cible ou parmi les unités dirigées par le chef
     * (sous-unités comprises) ; nouveau n+1 égal au n+1 actuel, au chef ou à un compte de son périmètre.
     * Sinon 403 ORG_HORS_PERIMETRE. Les règles habituelles du rattachement s'appliquent ensuite.
     *
     * @param chefId compte qui agit
     * @param cibleId compte dont le rattachement change
     * @param uniteActuelle unité actuelle de la cible (null si aucune)
     * @param responsableActuel n+1 actuel de la cible (null si aucun)
     * @param unite unité demandée, normalisée (null : aucune)
     * @param responsableId n+1 demandé, normalisé (null : aucun)
     * @param couverts comptes du périmètre de l'action pour le chef
     * @param unitesDirigees unités dirigées par le chef, sous-unités comprises
     */
    public static void verifierRattachementSubordonne(
        String chefId,
        String cibleId,
        String uniteActuelle,
        String responsableActuel,
        String unite,
        String responsableId,
        Set<String> couverts,
        Collection<String> unitesDirigees
    ) {
        if (chefId.equals(cibleId)) {
            throw IamException.interdit("IAM_SELF_ADMINISTRATION", "Un compte ne modifie pas son propre rattachement");
        }
        exigerCouverture(couverts.contains(cibleId), ActionPouvoir.RATTACHEMENT_MODIFIER);
        exigerCouverture(Objects.equals(unite, uniteActuelle) || (unite != null && unitesDirigees.contains(unite)), ActionPouvoir.RATTACHEMENT_MODIFIER);
        exigerCouverture(
            Objects.equals(responsableId, responsableActuel) || (responsableId != null && (responsableId.equals(chefId) || couverts.contains(responsableId))),
            ActionPouvoir.RATTACHEMENT_MODIFIER
        );
    }

    /** Refuse (422 ORG_PROFIL_NON_DEMANDABLE) des profils hors des profils demandables du demandeur. */
    public static void exigerDemandables(Collection<String> profils, Set<String> demandables) {
        Set<String> hors = new TreeSet<>(profils);
        hors.removeAll(demandables);
        if (!hors.isEmpty()) {
            throw IamException.nonTraitable("ORG_PROFIL_NON_DEMANDABLE", "Profil non demandable", Map.of("profils", List.copyOf(hors)));
        }
    }
}
