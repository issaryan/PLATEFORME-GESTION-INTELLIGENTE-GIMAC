package cm.gimac.des.homologation.domain;

import cm.gimac.des.homologation.domain.enumeration.NatureCouverture;
import cm.gimac.des.homologation.domain.enumeration.Partie;
import cm.gimac.des.homologation.domain.enumeration.Segment;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;

/**
 * Saisie d'une opération du référentiel (HOM-01). Tous les attributs sont facultatifs pour qu'un
 * brouillon se remplisse progressivement ; la complétude est contrôlée à la publication.
 */
public record ReferenceOperationData(
    @Size(max = 200) String libelle,
    @Size(max = 60) String phase,
    Segment segment,
    Partie partie,
    @Size(max = 40) String serviceCode,
    NatureCouverture natureCouverture,
    Map<String, Object> preuveAttendue,
    @Size(max = 80) String idScenario,
    List<String> predecesseurs,
    @Size(max = 60) String fonctionResponsable,
    Map<String, Object> grille
) {}
