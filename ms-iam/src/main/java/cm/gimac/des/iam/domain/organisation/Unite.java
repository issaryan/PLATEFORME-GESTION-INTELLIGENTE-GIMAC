package cm.gimac.des.iam.domain.organisation;

/**
 * Unité organisationnelle du GIMAC (D-76, docs/lot1/mon-activite.md §1.1) : code stable et unique,
 * libellé, type, unité parente (nulle pour une direction) et état. Une unité inactive n'est plus
 * proposée au rattachement ; les rattachements existants restent lisibles.
 *
 * @param responsableId responsable de l'unité (D-81, docs/lot1/pouvoirs-hierarchiques.md §2) : compte
 *     interne actif, ou nul ; il dirige l'unité et ses sous-unités
 */
public record Unite(String code, String libelle, TypeUnite type, String parent, boolean active, String responsableId) {
    /** Unité sans responsable. */
    public Unite(String code, String libelle, TypeUnite type, String parent, boolean active) {
        this(code, libelle, type, parent, active, null);
    }

    /** Même unité avec un autre responsable. */
    public Unite avecResponsable(String nouveauResponsable) {
        return new Unite(code, libelle, type, parent, active, nouveauResponsable);
    }
}
