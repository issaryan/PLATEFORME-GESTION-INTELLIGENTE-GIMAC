package cm.gimac.des.homologation.web.rest.lot1;

import cm.gimac.des.homologation.domain.JsonTexte;
import cm.gimac.des.homologation.domain.ReferenceOperation;
import cm.gimac.des.homologation.domain.ReferentialVersion;
import cm.gimac.des.homologation.domain.enumeration.EtatVersionReferentiel;
import cm.gimac.des.homologation.domain.enumeration.NatureCouverture;
import cm.gimac.des.homologation.domain.enumeration.Partie;
import cm.gimac.des.homologation.domain.enumeration.Segment;
import cm.gimac.des.homologation.domain.enumeration.TypeProjet;
import cm.gimac.des.homologation.domain.service.ReferentialCompletenessChecker;
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
        UUID versionActiveId,
        int nombreOperations,
        List<String> ordreTopologique,
        List<String> actionsPermises
    ) {
        public static VersionVue de(ReferentialVersion v) {
            return de(v, v.getEtat() == EtatVersionReferentiel.PUBLIEE ? v.getId() : null);
        }

        /** {@code versionActiveId} : version publiée en vigueur du même type (comparaison, impact de la publication). */
        public static VersionVue de(ReferentialVersion v, UUID versionActiveId) {
            return new VersionVue(
                v.getId(),
                v.getTypeProjet(),
                v.getNumero(),
                v.getLibelle(),
                v.getEtat(),
                v.getDateCreation(),
                v.getDatePublication(),
                v.getVersionSourceId(),
                versionActiveId,
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

    /**
     * Restitution du contrôle à blanc (HOM-02) pour DES-12 : acyclicité, ordre de référence, cycle, incohérences
     * de prédécesseurs (inconnu, segment), attributs manquants à la publication (HOM-01 A3) ; les anomalies brutes
     * du validateur restent servies.
     */
    public record ControleVue(
        boolean acyclique,
        List<String> ordreTopologique,
        List<String> cycle,
        List<Incoherence> incoherences,
        List<ReferentialCompletenessChecker.MissingAttributes> manquants,
        List<ReferentialGraphValidator.Anomalie> anomalies,
        boolean conforme
    ) {
        public record Incoherence(String code, String predecesseur, String motif) {}

        public static ControleVue de(ReferentialGraphValidator.Rapport r, List<ReferentialCompletenessChecker.MissingAttributes> manquants) {
            List<String> cycle = r
                .anomalies()
                .stream()
                .filter(a -> a.type() == ReferentialGraphValidator.TypeAnomalie.CYCLE)
                .findFirst()
                .map(ReferentialGraphValidator.Anomalie::codes)
                .orElse(List.of());
            List<Incoherence> incoherences = r
                .anomalies()
                .stream()
                .filter(a -> a.type() != ReferentialGraphValidator.TypeAnomalie.CYCLE)
                .map(a -> new Incoherence(a.codes().get(0), a.codes().size() > 1 ? a.codes().get(1) : "", a.motif()))
                .toList();
            return new ControleVue(cycle.isEmpty(), r.ordre(), cycle, incoherences, manquants, r.anomalies(), r.conforme() && manquants.isEmpty());
        }
    }

    public record MigrationRequest(@NotNull List<UUID> liaisonsRetenues) {}

    public record MigrationVue(UUID id, UUID versionSourceId, UUID versionCibleId, String etat, List<String> liaisonsRetenues) {}

    /** Événement de l'historique d'une version (chronologie de DES-12). */
    public record EvenementVue(String event, Instant at, String actor, String correlationId) {}

    /** Graphe par couches pour la vue DagView de DES-12 (HOM-02 A3). */
    public record GrapheVue(List<Noeud> noeuds, List<Arc> arcs, List<ReferentialGraphValidator.Anomalie> anomalies) {
        public record Noeud(String code, String libelle, Segment segment, Integer couche) {}

        public record Arc(String de, String vers) {}
    }
}
