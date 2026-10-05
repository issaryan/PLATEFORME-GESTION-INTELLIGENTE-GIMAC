package cm.gimac.des.iam.domain.compte;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Compte d'un utilisateur de la plateforme, tel que le fournisseur d'identité (Keycloak, autorité
 * des identités et des facteurs) le décrit. Aucun mot de passe ni secret n'y figure (RG-65).
 *
 * @param profilComplet vrai quand l'utilisateur a terminé sa configuration de premier accès
 * @param unite code de l'unité organisationnelle de rattachement (la plus fine), nul sans rattachement
 *     (D-76, docs/lot1/mon-activite.md §1.2)
 * @param responsableId identifiant du compte responsable (n+1), nul sans responsable
 */
public record Compte(
    String id,
    String identifiant,
    String prenom,
    String nom,
    String email,
    boolean emailVerifie,
    boolean actif,
    Instant creeLe,
    String telephone,
    String fonction,
    String langue,
    String institutionId,
    boolean profilComplet,
    Set<String> profils,
    List<ActionRequise> actionsRequises,
    boolean secondFacteur,
    String unite,
    String responsableId
) {
    public Compte {
        profils = profils == null ? Set.of() : Set.copyOf(profils);
        actionsRequises = actionsRequises == null ? List.of() : List.copyOf(actionsRequises);
    }

    /** Compte sans rattachement organisationnel (unité et responsable nuls). */
    public Compte(
        String id,
        String identifiant,
        String prenom,
        String nom,
        String email,
        boolean emailVerifie,
        boolean actif,
        Instant creeLe,
        String telephone,
        String fonction,
        String langue,
        String institutionId,
        boolean profilComplet,
        Set<String> profils,
        List<ActionRequise> actionsRequises,
        boolean secondFacteur
    ) {
        this(id, identifiant, prenom, nom, email, emailVerifie, actif, creeLe, telephone, fonction, langue, institutionId, profilComplet, profils, actionsRequises, secondFacteur, null, null);
    }

    /** Compte interne du GIMAC : sans institution (un correspondant porte celle de son participant). */
    public boolean interne() {
        return institutionId == null || institutionId.isBlank();
    }

    /** Même compte avec un autre rattachement organisationnel. */
    public Compte avecRattachement(String nouvelleUnite, String nouveauResponsable) {
        return new Compte(id, identifiant, prenom, nom, email, emailVerifie, actif, creeLe, telephone, fonction, langue, institutionId, profilComplet, profils, actionsRequises, secondFacteur, nouvelleUnite, nouveauResponsable);
    }

    public String nomAffiche() {
        String n = ((prenom == null ? "" : prenom) + " " + (nom == null ? "" : nom)).trim();
        return n.isEmpty() ? identifiant : n;
    }

    /**
     * Étapes restant à faire sur la plateforme (D-65), dans l'ordre de l'assistant : actions exigées,
     * fiche et charte tant que le premier accès n'est pas marqué terminé, second facteur tant qu'aucun
     * code TOTP n'est configuré.
     */
    public List<ActionRequise> etapesRestantes() {
        Set<ActionRequise> e = EnumSet.noneOf(ActionRequise.class);
        e.addAll(actionsRequises);
        if (!profilComplet) {
            e.add(ActionRequise.FICHE);
            e.add(ActionRequise.CHARTE);
        }
        if (!secondFacteur) {
            e.add(ActionRequise.SECOND_FACTEUR);
        }
        return List.copyOf(e);
    }

    /** Le premier accès n'est pas terminé tant qu'une étape reste à faire. */
    public boolean premierAccesEnAttente() {
        return !etapesRestantes().isEmpty();
    }
}
