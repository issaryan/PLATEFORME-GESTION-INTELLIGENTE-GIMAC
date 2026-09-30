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
        List<String> actionsPermises
    ) {}

    public record Livrable(String libelleKey, boolean fourni) {}

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
        List<String> actionsPermises
    ) {}

    public record LiaisonResume(
        String id,
        String reference,
        String institutionNom,
        TypeProjet typeProjet,
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
        List<String> actionsPermises
    ) {}

    public record Compteur(String cle, long valeur, String ton, String lien) {}

    public record TableauActeur(Fonction fonction, List<Compteur> compteurs, List<Tache> taches, List<LiaisonResume> liaisons, List<Blocage> blocages) {}

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

    /** Contexte servi à MS-CERTIFICATION (docs/lot1/parcours-backend.md §2). */
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
        String conducteurNom
    ) {}
}
