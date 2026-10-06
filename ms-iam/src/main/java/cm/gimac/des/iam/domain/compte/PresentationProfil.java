package cm.gimac.des.iam.domain.compte;

import java.time.Instant;

/**
 * Présentation d'un profil (D-86, docs/lot1/profils-organisation.md §2) : libellé et description.
 *
 * <p>Lue dans le fournisseur d'identité (attribut gimac_libelle et description du rôle), elle ne porte que
 * le libellé et la description ; libellé nul s'il n'est pas renseigné.
 *
 * <p>Tenue par MS-IAM (table profil_presentation), elle garde la dernière modification faite par
 * l'administration : libellé et description modifiés (nuls si seule la composition a changé), date et
 * auteur. MS-IAM la réapplique au fournisseur d'identité au démarrage si un réimport du domaine l'a
 * écrasée.
 */
public record PresentationProfil(String code, String libelle, String description, Instant modifieLe, String modifieParId, String modifieParNom) {
    /** Présentation lue dans le fournisseur d'identité. */
    public static PresentationProfil lue(String code, String libelle, String description) {
        return new PresentationProfil(code, libelle, description, null, null, null);
    }

    /** Libellé et description fixés par l'administration (sinon, seule la composition a été modifiée). */
    public boolean fixeeParAdministration() {
        return libelle != null;
    }
}
