package cm.gimac.des.homologation.application.port.in;

import cm.gimac.des.homologation.domain.EtapeReferentiel;
import cm.gimac.des.homologation.domain.ReferentialVersion;
import cm.gimac.des.homologation.domain.parcours.CouleurEtape;
import cm.gimac.des.homologation.domain.parcours.EtapeCode;
import java.util.List;
import java.util.UUID;

/**
 * Étapes configurables d'un brouillon du référentiel (D-89, docs/lot1/referentiel-droits-etapes.md §2) : ajout,
 * renommage, couleur, canonique, ordre et retrait d'une étape vide. Version figée : 409 HOM_REFERENTIAL_FROZEN.
 */
public interface GererEtapesReferentielUseCase {
    EtapeReferentiel ajouter(UUID versionId, String code, String libelle, CouleurEtape couleur, EtapeCode canonique, Integer ordre);

    EtapeReferentiel modifier(UUID versionId, String code, String libelle, CouleurEtape couleur, EtapeCode canonique);

    void supprimer(UUID versionId, String code);

    /** Nouvel ordre ; rend la version (étapes dans le nouvel ordre). */
    ReferentialVersion ordonner(UUID versionId, List<String> codes);
}
