package cm.gimac.des.homologation.web.rest.lot1;

import cm.gimac.des.homologation.domain.JsonTexte;
import cm.gimac.des.homologation.domain.ReferenceOperation;
import cm.gimac.des.homologation.domain.ReferentialVersion;
import cm.gimac.des.homologation.domain.enumeration.EtatVersionReferentiel;
import cm.gimac.des.homologation.domain.enumeration.NatureCouverture;
import cm.gimac.des.homologation.domain.enumeration.Partie;
import cm.gimac.des.homologation.domain.enumeration.Segment;
import cm.gimac.des.homologation.domain.enumeration.TypeProjet;
import cm.gimac.des.homologation.domain.service.ReferentialGraphValidator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Représentations REST du référentiel (HOM-01, HOM-02). */
public final class ReferentialViews {

    private ReferentialViews() {}

    public record CreateDraftRequest(@NotNull TypeProjet typeProjet, @NotBlank @Size(max = 200) String libelle, UUID versionSourceId) {}

    public record VersionVue(
        UUID id,
        TypeProjet typeProjet,
        Integer numero,
        String libelle,
        EtatVersionReferentiel etat,
        Instant dateCreation,
        Instant datePublication,
        UUID versionSourceId,
        int nombreOperations,
        List<String> ordreTopologique,
        List<String> actionsPermises
    ) {
        public static VersionVue de(ReferentialVersion v) {
            return new VersionVue(
                v.getId(),
                v.getTypeProjet(),
                v.getNumero(),
                v.getLibelle(),
                v.getEtat(),
                v.getDateCreation(),
                v.getDatePublication(),
                v.getVersionSourceId(),
                v.getOperations().size(),
                v.ordreTopologiqueListe(),
                v.actionsPermises()
            );
        }
    }

    public record OperationVue(
        UUID id,
        String code,
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
        public static OperationVue de(ReferenceOperation o) {
            return new OperationVue(
                o.getId(),
                o.getCode(),
                o.getLibelle(),
                o.getPhase(),
                o.getSegment(),
                o.getPartie(),
                o.getServiceCode(),
                o.getNatureCouverture(),
                JsonTexte.objet(o.getPreuveAttendue()),
                o.getIdScenario(),
                o.predecesseursListe(),
                o.getFonctionResponsable(),
                JsonTexte.objet(o.getGrille())
            );
        }
    }

    /** Graphe par couches pour la vue DagView de DES-12 (HOM-02 A3). */
    public record GrapheVue(List<Noeud> noeuds, List<Arc> arcs, List<ReferentialGraphValidator.Anomalie> anomalies) {
        public record Noeud(String code, String libelle, Segment segment, Integer couche) {}

        public record Arc(String de, String vers) {}
    }
}
