package cm.gimac.des.homologation.web.rest.lot1;

import cm.gimac.des.homologation.domain.EtapeReferentiel;
import cm.gimac.des.homologation.domain.JsonTexte;
import cm.gimac.des.homologation.domain.ReferenceOperation;
import cm.gimac.des.homologation.domain.ReferentialVersion;
import cm.gimac.des.homologation.domain.enumeration.EtatVersionReferentiel;
import cm.gimac.des.homologation.domain.enumeration.NatureCouverture;
import cm.gimac.des.homologation.domain.enumeration.Partie;
import cm.gimac.des.homologation.domain.enumeration.TypeProjet;
import cm.gimac.des.homologation.domain.service.ReferentialCompletenessChecker;
import cm.gimac.des.homologation.domain.service.ReferentialGraphValidator;
import cm.gimac.des.homologation.domain.service.VisibiliteReferentiel;
import cm.gimac.des.homologation.domain.parcours.CouleurEtape;
import cm.gimac.des.homologation.domain.parcours.EtapeCode;
import cm.gimac.des.homologation.domain.parcours.Fonction;
import cm.gimac.des.homologation.domain.parcours.Geste;
import cm.gimac.des.homologation.domain.parcours.GrainPreuve;
import cm.gimac.des.homologation.domain.parcours.Jalon;
import cm.gimac.des.homologation.domain.parcours.Livrable;
import cm.gimac.des.homologation.domain.parcours.Particularite;
import cm.gimac.des.homologation.domain.parcours.PlateformeExecution;
import cm.gimac.des.homologation.domain.parcours.Position;
import cm.gimac.des.homologation.domain.parcours.SegmentParcours;
import cm.gimac.des.homologation.domain.parcours.SignatureOperation;
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

    /** Brouillon ou dérivation ; {@code piloteId} facultatif : le créateur pilote par défaut (D-77, §5). */
    public record CreateDraftRequest(@NotNull TypeProjet typeProjet, @NotBlank @Size(max = 200) String libelle, UUID versionSourceId, UUID piloteId) {}

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
        List<String> actionsPermises,
        UUID piloteId,
        VisibiliteReferentiel.Filtre filtre
    ) {
        public static VersionVue de(ReferentialVersion v) {
            return de(v, v.getEtat() == EtatVersionReferentiel.PUBLIEE ? v.getId() : null);
        }

        /** {@code versionActiveId} : version publiée en vigueur du même type (comparaison, impact de la publication). */
        public static VersionVue de(ReferentialVersion v, UUID versionActiveId) {
            return de(v, versionActiveId, VisibiliteReferentiel.Vue.complete(v.getOperations()));
        }

        /** Lecteur concerné (D-89) : nombre d'opérations et ordre de référence réduits à ses opérations visibles. */
        public static VersionVue de(ReferentialVersion v, UUID versionActiveId, VisibiliteReferentiel.Vue vue) {
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
                vue.complete() ? v.getOperations().size() : vue.visibles().size(),
                vue.complete() ? v.ordreTopologiqueListe() : v.ordreTopologiqueListe().stream().filter(vue::visible).toList(),
                v.actionsPermises(),
                v.getPiloteId(),
                vue.filtre()
            );
        }
    }

    /**
     * Opération du référentiel (D-77, §1) : modèle complet du modélisateur (segment renommé AMONT, BRANCHE, AVAL ;
     * successeurs déduits des prédécesseurs) et attributs historiques de couverture (HOM-01).
     */
    public record OperationVue(
        UUID id,
        String code,
        String libelle,
        String description,
        String etape,
        String phase,
        SegmentParcours segment,
        Integer ordre,
        Fonction responsable,
        Fonction validateur,
        List<Fonction> consultes,
        List<Fonction> informes,
        List<Fonction> externes,
        List<String> predecesseurs,
        List<String> successeurs,
        Integer delaiJours,
        Integer delaiMaxJours,
        GrainPreuve grain,
        PlateformeExecution plateforme,
        boolean cle,
        boolean secret,
        boolean oubliSansSymptome,
        boolean pivot,
        String valeurReference,
        List<Livrable> livrables,
        Geste geste,
        Jalon jalon,
        String declencheur,
        Position position,
        Particularite particularite,
        Partie partie,
        String serviceCode,
        NatureCouverture natureCouverture,
        Map<String, Object> preuveAttendue,
        String idScenario,
        String fonctionResponsable,
        Map<String, Object> grille,
        boolean fantome,
        List<SignatureOperation> signatures,
        cm.gimac.des.homologation.domain.parcours.VueOperation vue,
        Boolean consultesRecoiventTache
    ) {
        /**
         * Fantôme (D-89) : prédécesseur ou successeur direct d'une opération qui concerne le lecteur, réduit à son
         * code et à son libellé pour garder la chronologie lisible.
         */
        public static OperationVue fantome(ReferenceOperation o) {
            return new OperationVue(
                null, o.getCode(), o.getLibelle(), null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                false, false, false, false, null, null, null, null, null, null, null, null, null, null, null, null, null, null, true, null, null, null
            );
        }

        public static OperationVue de(ReferenceOperation o) {
            List<ReferenceOperation> soeurs = o.getVersion() == null ? List.of(o) : o.getVersion().getOperations();
            return de(o, soeurs.stream().filter(x -> x.predecesseursListe().contains(o.getCode())).map(ReferenceOperation::getCode).sorted().toList());
        }

        public static OperationVue de(ReferenceOperation o, List<String> successeurs) {
            return new OperationVue(
                o.getId(),
                o.getCode(),
                o.getLibelle(),
                o.getDescription(),
                o.getEtape(),
                o.getPhase(),
                o.getSegment() == null ? null : o.getSegment().parcours(),
                o.getOrdre(),
                o.getResponsable(),
                o.getValidateur(),
                o.getConsultes(),
                o.getInformes(),
                o.getExternes(),
                o.predecesseursListe(),
                successeurs,
                o.getDelaiJours(),
                o.getDelaiMaxJours(),
                o.getGrain(),
                o.getPlateforme(),
                o.isCle(),
                o.isSecret(),
                o.isOubliSansSymptome(),
                o.isPivot(),
                o.getValeurReference(),
                o.getLivrables(),
                o.getGeste(),
                o.getJalon(),
                o.getDeclencheur(),
                o.getPosition(),
                o.getParticularite(),
                o.getPartie(),
                o.getServiceCode(),
                o.getNatureCouverture(),
                JsonTexte.objet(o.getPreuveAttendue()),
                o.getIdScenario(),
                o.getFonctionResponsable(),
                JsonTexte.objet(o.getGrille()),
                false,
                o.getSignatures(),
                o.vueEffective(),
                o.isConsultesRecoiventTache()
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
                .map(a -> new Incoherence(a.codes().isEmpty() ? "" : a.codes().get(0), a.codes().size() > 1 ? a.codes().get(1) : "", a.motif()))
                .toList();
            return new ControleVue(cycle.isEmpty(), r.ordre(), cycle, incoherences, manquants, r.anomalies(), r.conforme() && manquants.isEmpty());
        }
    }

    public record MigrationRequest(@NotNull List<UUID> liaisonsRetenues) {}

    public record MigrationVue(UUID id, UUID versionSourceId, UUID versionCibleId, String etat, List<String> liaisonsRetenues) {}

    /** Événement de l'historique d'une version (chronologie de DES-12). */
    public record EvenementVue(String event, Instant at, String actor, String correlationId) {}

    /**
     * Graphe étendu du modélisateur (D-77, §4) : chaque nœud porte l'opération complète (champs à plat), sa couche
     * de Kahn, son début et sa fin au plus tôt en jours depuis le démarrage, son appartenance au chemin critique et
     * les types d'anomalies qui le concernent ; arcs (marqués critiques), anomalies, chemin critique et durée totale.
     */
    public record GrapheVue(
        UUID versionId,
        TypeProjet typeProjet,
        Integer numero,
        EtatVersionReferentiel etat,
        UUID piloteId,
        List<Noeud> noeuds,
        List<Arc> arcs,
        List<ReferentialGraphValidator.Anomalie> anomalies,
        List<ReferentialCompletenessChecker.MissingAttributes> manquants,
        CheminCritique cheminCritique,
        List<String> actionsPermises,
        List<EtapeVue> etapes,
        VisibiliteReferentiel.Filtre filtre
    ) {
        public record Noeud(
            @com.fasterxml.jackson.annotation.JsonUnwrapped OperationVue operation,
            Integer couche,
            Integer debutAuPlusTot,
            Integer finAuPlusTot,
            Integer finAuPlusTotMax,
            boolean critique,
            List<String> anomalies
        ) {}

        public record Arc(String de, String vers, boolean critique) {}

        public record CheminCritique(List<String> codes, int dureeJours) {}
    }

    /** Élément d'un catalogue : code, libellé français et précisions facultatives. */
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    public record Entree(String code, String libelle, List<String> profils, Boolean externe, Boolean dedie, Integer rang, Boolean actif) {
        Entree(String code, String libelle, List<String> profils, Boolean externe, Boolean dedie, Integer rang) {
            this(code, libelle, profils, externe, dedie, rang, null);
        }

        static Entree de(String code, String libelle) {
            return new Entree(code, libelle, null, null, null, null, null);
        }
    }

    /**
     * Étape d'une version (D-89, §2) : code, libellé, ordre, couleur (jeton du design system), étape canonique
     * saisie ({@code null} : rattachée à celle de l'étape précédente) et canonique retenue pour la progression ;
     * {@code protegee} : liée à une canonique porteuse d'un jalon ou du verrou 2 ; {@code supprimable} : vide et
     * non protégée, sur un brouillon.
     */
    public record EtapeVue(
        String code,
        String libelle,
        int ordre,
        CouleurEtape couleur,
        String jeton,
        EtapeCode canonique,
        EtapeCode canoniqueEffective,
        boolean protegee,
        long nombreOperations,
        boolean supprimable
    ) {
        public static EtapeVue de(EtapeReferentiel e, ReferentialVersion v) {
            long n = v.nombreOperations(e.getCode());
            return new EtapeVue(
                e.getCode(),
                e.getLibelle(),
                e.getOrdre(),
                e.getCouleur(),
                e.getCouleur().jeton(),
                e.getCanonique(),
                v.canoniqueDe(e.getCode()).orElse(null),
                e.protegee(),
                n,
                v.getEtat() == EtatVersionReferentiel.BROUILLON && n == 0 && !e.protegee()
            );
        }

        public static List<EtapeVue> de(ReferentialVersion v) {
            return v.etapes().stream().map(e -> de(e, v)).toList();
        }
    }

    /** Création ({@code code} exigé) ou modification d'une étape ; {@code ordre} : position (1 = première), à la fin par défaut. */
    public record EtapeRequest(
        @Size(max = 40) String code,
        @NotBlank @Size(max = 120) String libelle,
        CouleurEtape couleur,
        EtapeCode canonique,
        Integer ordre
    ) {}

    /** Nouvel ordre des étapes : chaque code de la version une fois. */
    public record OrdreEtapesRequest(@NotNull List<@NotBlank String> codes) {}

    /** Étape canonique du parcours (CR) : rang, protection (jalon ou verrou 2). */
    public record EtapeCanoniqueVue(String code, String libelle, Integer rang, boolean protegee) {}

    /** Couleur proposée pour une étape : code, libellé, variable CSS du design system. */
    public record CouleurVue(String code, String libelle, String jeton) {}

    public record Pilote(String id, String nom) {}

    /**
     * Catalogue du modélisateur (D-77, §4) : fonctions (libellés, profils, externes), gestes (dédiés ou non),
     * jalons dans leur ordre, déclencheurs connus, étapes, segments, plateformes, grains, natures de livrable,
     * particularités, types de pièces usuels et pilotes possibles d'un brouillon.
     */
    public record CatalogueVue(
        List<Entree> fonctions,
        List<Entree> gestes,
        List<Entree> jalons,
        List<Entree> declencheurs,
        List<Entree> etapes,
        List<Entree> segments,
        List<Entree> plateformes,
        List<Entree> grains,
        List<Entree> naturesLivrable,
        List<Entree> particularites,
        List<String> typesPieces,
        List<Pilote> pilotes,
        List<EtapeCanoniqueVue> etapesCanoniques,
        List<CouleurVue> couleurs,
        @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL) List<EtapeVue> etapesVersion,
        List<Entree> modelesTypeProjet,
        List<Entree> typesProjet,
        List<Entree> vuesOperation
    ) {}

    public record PositionRequest(@NotBlank String code, double x, double y) {}

    public record ArcRequest(@NotBlank String de, @NotBlank String vers, boolean present) {}

    public record PiloteRequest(@NotNull UUID compteId) {}
}
