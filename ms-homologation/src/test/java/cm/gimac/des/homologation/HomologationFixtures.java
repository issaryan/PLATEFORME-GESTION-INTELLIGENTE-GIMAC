package cm.gimac.des.homologation;

import cm.gimac.des.homologation.application.port.in.ManageReferentialUseCase;
import cm.gimac.des.homologation.domain.ReferenceOperationData;
import cm.gimac.des.homologation.domain.ReferenceOperationSamples;
import cm.gimac.des.homologation.domain.ReferentialVersion;
import cm.gimac.des.homologation.domain.ReferentialVersionSamples;
import cm.gimac.des.homologation.domain.enumeration.Segment;
import cm.gimac.des.homologation.domain.enumeration.TypeProjet;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Jeux de données des tests d'intégration du lot 1, écrits par les cas d'utilisation réels. */
@Component
public class HomologationFixtures {

    private final ManageReferentialUseCase referentiels;
    private final cm.gimac.des.homologation.repository.ReferentialVersionRepository versions;

    public HomologationFixtures(ManageReferentialUseCase referentiels, cm.gimac.des.homologation.repository.ReferentialVersionRepository versions) {
        this.referentiels = referentiels;
        this.versions = versions;
    }

    /** Brouillon complet et acyclique (amont, branche, aval), validé en base. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public UUID brouillonComplet(TypeProjet type) {
        String p = ReferentialVersionSamples.prefixe(type);
        ReferentialVersion v = referentiels.creerBrouillon(type, "Référentiel de recette " + type, null);
        referentiels.enregistrerOperation(v.getId(), p + "-AMONT-01", ReferenceOperationSamples.complete(Segment.TRONC_COMMUN_AMONT).data());
        referentiels.enregistrerOperation(v.getId(), p + "-BR-01", ReferenceOperationSamples.complete(Segment.BRANCHE).avecPredecesseurs(p + "-AMONT-01").data());
        referentiels.enregistrerOperation(v.getId(), p + "-AVAL-01", ReferenceOperationSamples.complete(Segment.TRONC_COMMUN_AVAL).avecPredecesseurs(p + "-BR-01").data());
        return v.getId();
    }

    /**
     * Version publiée en vigueur pour le type (cadrage, étape 3) : réutilise celle qui existe, sinon publie la
     * version 1 fidèle à l'annexe A du CR (D-77), comme la reprise des données le fait en exploitation.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public synchronized UUID referentielPublie(TypeProjet type) {
        return versions.findFirstByTypeProjetAndEtat(type, cm.gimac.des.homologation.domain.enumeration.EtatVersionReferentiel.PUBLIEE)
            .map(ReferentialVersion::getId)
            .orElseGet(() -> referentiels.publier(brouillonAnnexeA(type)).getId());
    }

    /** Brouillon fidèle à l'annexe A du CR en vigueur (v1.7). */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public UUID brouillonAnnexeA(TypeProjet type) {
        ReferentialVersion v = referentiels.creerBrouillon(type, "Annexe A du CR-CERT-GIMAC-01 v1.7 (jeu d'essai)", null);
        cm.gimac.des.homologation.domain.service.ConversionAnnexeA.enVigueur(type).forEach(o -> referentiels.enregistrerOperation(v.getId(), o.code(), o.data()));
        return v.getId();
    }

    /** Version publiée minimale (amont, branche, aval) : simulation et migration des liaisons (HOM-03). */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public UUID referentielMinimalPublie(TypeProjet type) {
        return referentiels.publier(brouillonComplet(type)).getId();
    }

    /** Brouillon dont chaque opération désigne la suivante comme prédécesseur, la dernière désignant la première (HOM-02 D). */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public UUID brouillonAvecCycle(TypeProjet type, String a, String b, String c) {
        ReferentialVersion v = referentiels.creerBrouillon(type, "Référentiel cyclique", null);
        referentiels.enregistrerOperation(v.getId(), a, ReferenceOperationSamples.complete(Segment.BRANCHE).avecPredecesseurs(b).data());
        referentiels.enregistrerOperation(v.getId(), b, ReferenceOperationSamples.complete(Segment.BRANCHE).avecPredecesseurs(c).data());
        referentiels.enregistrerOperation(v.getId(), c, ReferenceOperationSamples.complete(Segment.BRANCHE).avecPredecesseurs(a).data());
        return v.getId();
    }

    /** Brouillon dont une opération n'indique ni la partie ni la preuve attendue. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public UUID brouillonIncomplet(TypeProjet type, String code) {
        ReferentialVersion v = referentiels.creerBrouillon(type, "Référentiel incomplet", null);
        referentiels.enregistrerOperation(v.getId(), code, ReferenceOperationSamples.complete(Segment.BRANCHE).sansPartie().sansPreuveAttendue().data());
        return v.getId();
    }

    /** Brouillon délégataire : une opération amont dépend d'une opération de branche. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public UUID brouillonSegmentIncoherent() {
        ReferentialVersion v = referentiels.creerBrouillon(TypeProjet.DELEGATAIRE, "Référentiel incohérent", null);
        referentiels.enregistrerOperation(v.getId(), "DL-BR-02", ReferenceOperationSamples.complete(Segment.BRANCHE).data());
        referentiels.enregistrerOperation(v.getId(), "DL-AMONT-03", ReferenceOperationSamples.complete(Segment.TRONC_COMMUN_AMONT).avecPredecesseurs("DL-BR-02").data());
        return v.getId();
    }

    public String operationJson(String code) {
        return """
            {"libelle":"Signature de la convention","phase":"Adhésion","segment":"TRONC_COMMUN_AMONT",
             "partie":"PARTICIPANT","serviceCode":"TRANSVERSE","natureCouverture":"AUCUNE",
             "preuveAttendue":{"ETABLISSEMENT_PAIEMENT":"CONVENTION_SIGNEE"},"predecesseurs":[],
             "fonctionResponsable":"DIRECTION"}
            """;
    }

    public ReferenceOperationData operation(Segment segment) {
        return ReferenceOperationSamples.complete(segment).data();
    }
}
