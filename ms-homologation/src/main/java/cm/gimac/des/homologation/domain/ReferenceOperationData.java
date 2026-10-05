package cm.gimac.des.homologation.domain;

import cm.gimac.des.homologation.domain.enumeration.NatureCouverture;
import cm.gimac.des.homologation.domain.enumeration.Partie;
import cm.gimac.des.homologation.domain.enumeration.Segment;
import cm.gimac.des.homologation.domain.parcours.EtapeCode;
import cm.gimac.des.homologation.domain.parcours.Fonction;
import cm.gimac.des.homologation.domain.parcours.Geste;
import cm.gimac.des.homologation.domain.parcours.GrainPreuve;
import cm.gimac.des.homologation.domain.parcours.Jalon;
import cm.gimac.des.homologation.domain.parcours.Livrable;
import cm.gimac.des.homologation.domain.parcours.Particularite;
import cm.gimac.des.homologation.domain.parcours.PlateformeExecution;
import cm.gimac.des.homologation.domain.parcours.Position;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;

/**
 * Saisie d'une opération du référentiel (HOM-01 ; modèle du modélisateur, docs/lot1/modelisateur-processus.md
 * §1). Tous les attributs sont facultatifs pour qu'un brouillon se remplisse progressivement ; la complétude et
 * la cohérence sont contrôlées à la publication. Les attributs historiques (phase, partie, service, nature de
 * couverture, preuve attendue, fonction responsable) sont dérivés du modèle quand l'étape est saisie sans eux.
 * {@code code} ne sert qu'à la création ({@code POST /referentiels/{id}/operations}).
 */
public record ReferenceOperationData(
    @Size(max = 300) String libelle,
    @Size(max = 60) String phase,
    Segment segment,
    Partie partie,
    @Size(max = 40) String serviceCode,
    NatureCouverture natureCouverture,
    Map<String, Object> preuveAttendue,
    @Size(max = 80) String idScenario,
    List<String> predecesseurs,
    @Size(max = 60) String fonctionResponsable,
    Map<String, Object> grille,
    @Size(max = 40) String code,
    @Size(max = 4000) String description,
    EtapeCode etape,
    Integer ordre,
    Fonction responsable,
    Fonction validateur,
    List<Fonction> consultes,
    List<Fonction> informes,
    List<Fonction> externes,
    @Min(0) @Max(365) Integer delaiJours,
    GrainPreuve grain,
    PlateformeExecution plateforme,
    Boolean cle,
    Boolean secret,
    Boolean oubliSansSymptome,
    Boolean pivot,
    @Size(max = 120) String valeurReference,
    @Valid List<Livrable> livrables,
    Geste geste,
    Jalon jalon,
    @Size(max = 120) String declencheur,
    Position position,
    Particularite particularite
) {
    /** Saisie historique (HOM-01) : attributs de couverture seulement. */
    public ReferenceOperationData(
        String libelle,
        String phase,
        Segment segment,
        Partie partie,
        String serviceCode,
        NatureCouverture natureCouverture,
        Map<String, Object> preuveAttendue,
        String idScenario,
        List<String> predecesseurs,
        String fonctionResponsable,
        Map<String, Object> grille
    ) {
        this(
            libelle,
            phase,
            segment,
            partie,
            serviceCode,
            natureCouverture,
            preuveAttendue,
            idScenario,
            predecesseurs,
            fonctionResponsable,
            grille,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null
        );
    }

    /** Même saisie, qualités R et V renseignées. */
    public ReferenceOperationData avecQualites(Fonction r, Fonction v) {
        return new ReferenceOperationData(
            libelle,
            phase,
            segment,
            partie,
            serviceCode,
            natureCouverture,
            preuveAttendue,
            idScenario,
            predecesseurs,
            fonctionResponsable,
            grille,
            code,
            description,
            etape,
            ordre,
            r,
            v,
            consultes,
            informes,
            externes,
            delaiJours,
            grain,
            plateforme,
            cle,
            secret,
            oubliSansSymptome,
            pivot,
            valeurReference,
            livrables,
            geste,
            jalon,
            declencheur,
            position,
            particularite
        );
    }
}
