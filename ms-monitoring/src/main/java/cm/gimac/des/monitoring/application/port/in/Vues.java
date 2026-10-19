package cm.gimac.des.monitoring.application.port.in;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Vues rendues par les cas d'utilisation, au format du contrat (docs/lot1/astreintes-monitoring.md). */
public final class Vues {

    private Vues() {}

    public record Personne(String id, String nom) {}

    public record Reference(UUID id, String code) {}

    public record LiaisonVue(
        UUID id,
        String code,
        String libelle,
        String portee,
        UUID institutionId,
        String type,
        String etat,
        Instant depuis,
        Instant derniereMesure,
        Long periodeMesureSecondes,
        Instant maintenanceJusqua,
        Reference plateformeParente,
        long alertesOuvertes
    ) {}

    public record LiaisonCourte(UUID id, String code, String libelle) {}

    public record RacineVue(UUID id, String reference) {}

    public record AcquittementVue(Personne par, Instant le, String commentaire, boolean ticketDemande, boolean horsDelai) {}

    public record AlerteVue(
        UUID id,
        String reference,
        LiaisonCourte liaison,
        String source,
        String regle,
        String gravite,
        String etat,
        Instant premiereOccurrence,
        Instant derniereOccurrence,
        int occurrences,
        long delaiMinutes,
        Instant echeance,
        boolean horsDelai,
        int palierEscalade,
        Instant escaladeLe,
        RacineVue racine,
        long symptomes,
        AcquittementVue acquittement,
        UUID ticketId,
        Instant resolueLe
    ) {}

    public record PolitiqueVue(String gravite, long delaiMinutes, String niveauNotification, boolean canalUrgent, String ticket) {}

    public record Reception(UUID id, String reference, boolean creee) {}

    public record RapportCourt(UUID id, String etat) {}

    public record VacationCourte(UUID id, String reference, RapportCourt rapport) {}

    public record VacationVue(
        UUID id,
        String reference,
        LocalDate date,
        String plage,
        Instant debut,
        Instant fin,
        List<Personne> operateurs,
        Personne chefDeQuart,
        boolean enCours,
        RapportCourt rapport,
        VacationCourte precedente,
        VacationCourte suivante,
        String responsable
    ) {}

    public record VacationEnCours(VacationVue vacation) {}

    public record EntreeVue(
        UUID id,
        long rang,
        Instant le,
        Personne auteur,
        String nature,
        String texte,
        String reference,
        UUID rectifie,
        String empreinte,
        String empreintePrecedente
    ) {}

    public record VerificationVue(boolean intacte, int entrees, String tete) {}

    public record VacationRapport(UUID id, String reference, String plage, LocalDate date) {}

    public record SignatureVue(String role, Personne signataire, Instant le, String defi, String reserves) {}

    public record Horodatage(String serie) {}

    public record Attendus(List<Personne> sortants, List<Personne> entrants, List<Personne> chefs) {}

    public record RapportVue(
        UUID id,
        String reference,
        VacationRapport vacation,
        String etat,
        Map<String, Object> contenu,
        String consignes,
        String empreinteContenu,
        String empreinte,
        String documentRef,
        Horodatage horodatage,
        List<SignatureVue> signatures,
        Instant responsabiliteTransfereeLe,
        Instant scelleLe,
        Attendus attendus
    ) {}

    public record AffectationVue(UUID id, LocalDate date, String plage, int niveau, Personne operateur, boolean chefDeQuart) {}

    public record NonCouverteVue(LocalDate date, String plage, String besoin, int manque, List<String> causes) {}

    public record ViolationVue(String code, String operateurId, LocalDate date, String plage) {}

    public record ChargeVue(Personne operateur, double heures, int nuits, int finsDeSemaine) {}

    public record GrilleVue(
        UUID id,
        String semaine,
        int revision,
        String etat,
        String statutSolveur,
        Long objectif,
        Double ecart,
        Long dureeMs,
        Instant genereeLe,
        Personne genereePar,
        Instant publieeLe,
        Personne publieePar,
        List<LocalDate> jours,
        List<AffectationVue> affectations,
        List<NonCouverteVue> nonCouvertes,
        List<ViolationVue> violations,
        List<ChargeVue> charges
    ) {}

    public record GrilleResume(
        UUID id,
        String semaine,
        int revision,
        String etat,
        String statutSolveur,
        int nonCouvertes,
        Instant genereeLe,
        Instant publieeLe
    ) {}

    public record AffectationCourte(UUID id, LocalDate date, String plage, int niveau) {}

    public record EchangeVue(
        UUID id,
        UUID grilleId,
        String etat,
        Personne demandeur,
        Personne cible,
        AffectationCourte affectation,
        AffectationCourte affectationCible,
        String motif,
        Instant creeLe,
        Instant decideLe,
        Personne decidePar,
        UUID nouvelleGrilleId
    ) {}
}
