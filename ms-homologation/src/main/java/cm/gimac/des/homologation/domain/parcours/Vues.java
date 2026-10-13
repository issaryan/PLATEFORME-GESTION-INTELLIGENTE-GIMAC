package cm.gimac.des.homologation.domain.parcours;

import cm.gimac.des.homologation.domain.enumeration.TypeProjet;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Représentations du parcours servies à l'interface, au format de
 * {@code frontend/packages/lot1/src/modele/parcours.ts} : les grandeurs calculées (étape courante,
 * couvertures, SLA, compteurs, actions permises) viennent du service, l'interface les affiche.
 */
public final class Vues {

    private Vues() {}

    public record Operation(
        String id,
        String liaisonId,
        String rang,
        String libelle,
        EtapeCode etape,
        SegmentParcours segment,
        PlateformeExecution plateforme,
        GrainPreuve grain,
        EtatOperationParcours etat,
        Raci raci,
        LocalDate echeance,
        Sla sla,
        LocalDate faiteLe,
        Preuve preuve,
        boolean cle,
        boolean secret,
        boolean valeurReference,
        boolean oubliSansSymptome,
        boolean pivot,
        List<String> predecesseursNonFaits,
        List<String> transitionsPermises,
        List<String> actionsPermises,
        Geste geste,
        Jalon jalon,
        String declencheur,
        List<cm.gimac.des.homologation.domain.parcours.Livrable> livrables,
        List<Signature> signatures,
        String etapeCode,
        VueOperation vue,
        List<LivrableAttendu> livrablesAttendus,
        List<Renvoi> renvois
    ) {}

    /**
     * Livrable attendu d'une opération (D-93 §3, §5) : modèle du référentiel (libellé, nature, déposant, types admis,
     * obligatoire), dépôt éventuel, et {@code peutDeposer} : l'acteur peut le déposer maintenant (RACI imposé).
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record LivrableAttendu(
        String code,
        String libelle,
        NatureLivrable nature,
        Fonction deposant,
        String libelleDeposant,
        List<String> typesPieces,
        boolean obligatoire,
        DepotLivrable depot,
        boolean peutDeposer
    ) {}

    /**
     * Étape réelle de la version du référentiel (D-89, D-93 §5) : code, libellé, ordre, couleur et jeton du design
     * system, étape canonique ; état et grandeurs calculés comme pour les étapes canoniques.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record EtapeVersion(
        String code,
        String libelle,
        int ordre,
        CouleurEtape couleur,
        String jeton,
        EtapeCode canonique,
        String etat,
        int actionsFaites,
        int actionsTotal,
        Personne responsable,
        Personne validateur,
        LocalDate debut,
        LocalDate closeLe,
        LocalDate echeance,
        Sla sla,
        List<Livrable> livrables
    ) {}

    /**
     * Intervenant du dossier lu dans le RACI des opérations (D-93 §5) : fonction, qualités tenues (R, V, C, I, E),
     * personne désignée (ou institution), {@code aAffecter} : la fonction n'est tenue par personne.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record IntervenantDossier(Fonction fonction, String libelle, List<Qualite> qualites, Personne personne, boolean aAffecter) {}

    /**
     * Signature exigée d'une opération (D-91) : attendue ou signée ; {@code attendu} : personne désignée pour la
     * fonction (sinon tout porteur de la fonction) ; {@code suivante} : peut être donnée maintenant.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Signature(
        String code,
        String libelle,
        int ordre,
        Fonction signataire,
        String libelleSignataire,
        List<String> porte,
        String etat,
        boolean suivante,
        Personne attendu,
        String signePar,
        String signeParNom,
        java.time.Instant signeLe,
        String empreinte
    ) {}

    /**
     * Livrable d'une étape : {@code libelleKey} pour les étapes 1 et 2 (tenues par MS-PARTICIPANT) ; pour une opération,
     * son libellé, l'opération (code) et le code du livrable du référentiel (D-93 §5 : plus de clé parcours.op.&lt;rang&gt;).
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Livrable(String libelleKey, boolean fourni, String libelle, String operation, String code, Boolean obligatoire) {
        public Livrable(String libelleKey, boolean fourni) {
            this(libelleKey, fourni, null, null, null, null);
        }
    }

    public record Etape(
        EtapeCode code,
        Integer rang,
        String etat,
        List<Fonction> porteurs,
        Personne responsable,
        Map<String, Boolean> elements,
        int actionsFaites,
        int actionsTotal,
        List<Livrable> livrables,
        Personne validateur,
        LocalDate valideeLe,
        LocalDate debut,
        LocalDate closeLe,
        LocalDate echeance,
        Sla sla
    ) {}

    public record ServicePerimetre(
        String serviceCode,
        String libelle,
        boolean inclus,
        String etat,
        LocalDate entreeEssai,
        LocalDate sortieEssai,
        LocalDate certifieLe,
        Integer avancementSimulateur
    ) {}

    public record Blocage(
        String id,
        String liaisonId,
        String motif,
        LocalDate depuis,
        String prochaineAction,
        Personne responsable,
        LocalDate echeanceLevee,
        LocalDate leveLe,
        EtapeCode etape,
        List<String> transitionsPermises,
        List<String> actionsPermises
    ) {}

    public record Interlocuteur(String nom, String fonction) {}

    public record Coordonnees(String adresse, String port, String pointsTerminaison, LocalDate communiqueesLe) {}

    public record Dossier(
        String id,
        String reference,
        String institutionId,
        String institutionNom,
        TypeProjet typeProjet,
        /** Libellé du type (CER-05) : le portail ne lit pas le catalogue des types ; null : le code fait foi. */
        String typeProjetLibelle,
        String banquePartenaireNom,
        String personnalisateurNom,
        StatutParcours statut,
        EtapeCode etapeCourante,
        Personne conducteur,
        Personne responsableFonctionnel,
        Personne responsableTechnique,
        LocalDate dateDemarrage,
        LocalDate dateCibleProduction,
        Integer versionReferentiel,
        List<ServicePerimetre> perimetre,
        Couverture couvertureCertification,
        Couverture couvertureMiseEnService,
        List<Etape> etapes,
        List<Blocage> blocages,
        List<Interlocuteur> interlocuteursParticipant,
        String liaisonOrigineId,
        Coordonnees coordonneesTechniques,
        List<String> transitionsPermises,
        List<String> actionsPermises,
        List<EtapeVersion> etapesVersion,
        List<IntervenantDossier> intervenants,
        /** Intitulé libre du projet (D-104). */
        String intituleProjet,
        /** Référence du projet dans l'outil d'origine (D-104, reprise) ; null au portail. */
        String referenceOrigine
    ) {}

    public record LiaisonResume(
        String id,
        String reference,
        String institutionNom,
        TypeProjet typeProjet,
        /** Libellé du type (CER-05) ; null : le code fait foi. */
        String typeProjetLibelle,
        StatutParcours statut,
        EtapeCode etapeCourante,
        String conducteurNom,
        Couverture couvertureCertification,
        Couverture couvertureMiseEnService,
        boolean bloquee,
        long tachesEnRetard,
        LocalDate prochaineEcheance,
        LocalDate dateCibleProduction
    ) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Tache(
        String operationId,
        String liaisonId,
        String liaisonReference,
        String institutionNom,
        String rang,
        EtapeCode etape,
        Qualite qualite,
        LocalDate echeance,
        Sla sla,
        boolean oubliSansSymptome,
        String lien,
        List<String> actionsPermises,
        String libelle,
        Boolean aPrendre,
        String signature
    ) {}

    /** Compteur du tableau d'acteur (D-94 §3) : décrit en données ({@code libelle}, {@code fonction} : celle qui l'ouvre). */
    public record Compteur(String cle, long valeur, String ton, String lien, String libelle, String fonction) {
        public Compteur(String cle, long valeur, String ton, String lien) {
            this(cle, valeur, ton, lien, null, null);
        }
    }

    public record TableauActeur(Fonction fonction, List<Compteur> compteurs, List<Tache> taches, List<LiaisonResume> liaisons, List<Blocage> blocages) {}

    /**
     * « Mes actions de certification » (D-98 §5) : opération que la personne doit faire (qualité R, V ou C, signature
     * attendue, tâche à prendre) ou qu'elle suit (conducteur de la liaison, intervenant désigné), avec son état, ses
     * intervenants, son lien direct et les actions qu'elle peut accomplir maintenant.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record MonOperation(
        String operationId,
        String liaisonId,
        String liaisonReference,
        String institutionNom,
        StatutParcours statutLiaison,
        String code,
        String libelle,
        EtapeCode etape,
        String etapeCode,
        EtatOperationParcours etat,
        Qualite qualite,
        Boolean aPrendre,
        String signature,
        Geste geste,
        Jalon jalon,
        String declencheur,
        VueOperation vue,
        Personne responsable,
        Personne validateur,
        LocalDate echeance,
        Sla sla,
        String lien,
        List<String> actionsPermises
    ) {}

    /** Jalon d'une liaison suivie : atteint quand l'opération qui le porte est faite. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record JalonSuivi(Jalon jalon, boolean atteint, LocalDate atteintLe, String operationId) {}

    /** Liaison que la personne conduit ou sur laquelle elle intervient : statut, avancement et jalons. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record LiaisonSuivie(
        String liaisonId,
        String reference,
        String institutionNom,
        TypeProjet typeProjet,
        StatutParcours statut,
        EtapeCode etapeCourante,
        boolean conducteur,
        long operationsFaites,
        long operationsTotal,
        List<JalonSuivi> jalons,
        String lien
    ) {}

    /** Réponse de {@code GET /parcours/mes-operations} : à faire, à suivre, liaisons suivies et leurs jalons. */
    public record MesOperations(List<MonOperation> aAgir, List<MonOperation> aSuivre, List<LiaisonSuivie> liaisons) {}

    public record ServiceNombre(String serviceCode, String libelle, long nombre) {}

    public record ServiceDelai(String serviceCode, String libelle, long delaiMoyenJours) {}

    public record ProjetBloque(String liaisonId, String reference, String institutionNom, String motif, LocalDate depuis, long jours) {}

    public record RepartitionEtape(EtapeCode etape, long nombre) {}

    public record Questions(
        long enCertification,
        List<ServiceNombre> servicesPlusDemandes,
        List<ServiceDelai> servicesPlusLongs,
        Long delaiMoyenAdhesionProductionJours,
        List<ProjetBloque> projetsBloques,
        List<ServiceNombre> lancementsParService12Mois
    ) {}

    public record Pilotage(
        long projetsActifs,
        long projetsBloques,
        long tachesEnRetard,
        long echeancesProches,
        List<RepartitionEtape> repartitionParEtape,
        Long delaiMoyenCertificationJours,
        Integer tauxRespectDelais,
        Questions questions
    ) {}

    public record LigneFiche(String serviceCode, String libelle, Boolean ouvert, String commentaire) {}

    public record FicheSuivi(
        String liaisonId,
        EtatFicheSuivi etat,
        List<LigneFiche> lignes,
        LocalDate renseigneeLe,
        LocalDate constateeLe,
        List<String> transitionsPermises,
        List<String> actionsPermises
    ) {}

    public record CouvertureSimple(Integer taux, int numerateur, int denominateur) {}

    /**
     * Contexte servi à MS-CERTIFICATION (docs/lot1/parcours-backend.md §2) ; {@code modeleTypeProjet} et
     * {@code attestationsExigees} (D-91) portent les comportements du type, que MS-CERTIFICATION ne connaît pas.
     */
    public record ContexteCertification(
        UUID liaisonId,
        String reference,
        UUID institutionId,
        String institutionNom,
        TypeProjet typeProjet,
        boolean fintech,
        StatutParcours statut,
        CouvertureSimple couvertureCertification,
        boolean couvertureConstatee,
        List<String> servicesCouverts,
        UUID conducteurId,
        String conducteurNom,
        ModeleTypeProjet modeleTypeProjet,
        List<TypeAttestation> attestationsExigees
    ) {}
}
