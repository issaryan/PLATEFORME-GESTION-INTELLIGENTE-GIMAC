package cm.gimac.des.iam.application.port.out;

import cm.gimac.des.iam.domain.pouvoir.PouvoirsProfil;
import java.util.Map;

/**
 * Matrice des pouvoirs (D-81, docs/lot1/pouvoirs-hierarchiques.md §3), tenue par MS-IAM (tables
 * pouvoir_profil et profil_demandable) et réglée par l'administration. Un profil absent n'a aucun pouvoir.
 */
public interface MatricePouvoirs {
    /** Lignes renseignées, par profil. */
    Map<String, PouvoirsProfil> toutes();

    /** Ligne d'un profil ; AUCUNE partout s'il est absent. */
    PouvoirsProfil lire(String profil);

    /** Remplace la ligne du profil. */
    void enregistrer(PouvoirsProfil ligne);

    /** Retire un profil de la matrice : sa ligne et sa présence parmi les profils demandables des autres. */
    void supprimer(String profil);
}
