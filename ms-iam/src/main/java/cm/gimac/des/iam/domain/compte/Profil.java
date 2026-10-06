package cm.gimac.des.iam.domain.compte;

import java.time.Instant;
import java.util.Set;

/**
 * Profil par fonction (rôle composite du realm) et les permissions qui le composent (D-58).
 *
 * @param libelle nom affiché du profil (D-86, docs/lot1/profils-organisation.md §2), attribut gimac_libelle
 *     du rôle ; le code quand il n'est pas renseigné
 * @param cree vrai pour un profil créé par l'administration (D-81, docs/lot1/pouvoirs-hierarchiques.md
 *     §5), supprimable sans titulaire ; faux pour un profil livré avec la plateforme
 * @param modifieLe dernière modification par l'administration (libellé, description, permissions,
 *     création), nulle si le profil n'a jamais été modifié depuis la console
 * @param modifiePar auteur de cette modification, nul de même
 */
public record Profil(
    String code,
    String libelle,
    String description,
    Set<String> permissions,
    boolean privilegie,
    long titulaires,
    boolean cree,
    Instant modifieLe,
    Auteur modifiePar
) {
    /** Auteur d'une modification : identifiant et nom affiché du compte. */
    public record Auteur(String id, String nomAffiche) {}

    public Profil {
        permissions = permissions == null ? Set.of() : Set.copyOf(permissions);
        libelle = libelle == null || libelle.isBlank() ? code : libelle;
    }

    /** Profil sans trace de modification. */
    public Profil(String code, String libelle, String description, Set<String> permissions, boolean privilegie, long titulaires, boolean cree) {
        this(code, libelle, description, permissions, privilegie, titulaires, cree, null, null);
    }

    /** Profil sans libellé (le code en tient lieu). */
    public Profil(String code, String description, Set<String> permissions, boolean privilegie, long titulaires, boolean cree) {
        this(code, null, description, permissions, privilegie, titulaires, cree);
    }

    /** Profil livré avec la plateforme, sans libellé. */
    public Profil(String code, String description, Set<String> permissions, boolean privilegie, long titulaires) {
        this(code, description, permissions, privilegie, titulaires, false);
    }

    /** Même profil, qualifié par le catalogue de MS-IAM (privilégié, créé). */
    public Profil qualifie(boolean estPrivilegie, boolean estCree) {
        return new Profil(code, libelle, description, permissions, estPrivilegie, titulaires, estCree, modifieLe, modifiePar);
    }

    /** Même profil avec la trace de sa dernière modification (absente : inchangé). */
    public Profil modifie(PresentationProfil trace) {
        if (trace == null || trace.modifieLe() == null) {
            return this;
        }
        return new Profil(code, libelle, description, permissions, privilegie, titulaires, cree, trace.modifieLe(), new Auteur(trace.modifieParId(), trace.modifieParNom()));
    }
}
