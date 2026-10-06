package cm.gimac.des.iam.application.port.out;

import cm.gimac.des.iam.domain.compte.PresentationProfil;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/**
 * Modifications des profils par l'administration (D-86, docs/lot1/profils-organisation.md §2), table
 * profil_presentation : libellé et description fixés depuis la console, date et auteur de la dernière
 * modification (libellé, description, composition, création).
 */
public interface PresentationsProfils {
    /** Toutes les traces, par code de profil. */
    Map<String, PresentationProfil> toutes();

    Optional<PresentationProfil> lire(String code);

    /** Libellé et description fixés par l'administration, avec la date et l'auteur. */
    void fixer(String code, String libelle, String description, Instant le, String parId, String parNom);

    /** Modification de la composition ou création : date et auteur, libellé et description inchangés. */
    void toucher(String code, Instant le, String parId, String parNom);

    void supprimer(String code);
}
