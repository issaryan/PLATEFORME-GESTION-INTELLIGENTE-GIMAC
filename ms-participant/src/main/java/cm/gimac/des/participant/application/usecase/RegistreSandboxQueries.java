package cm.gimac.des.participant.application.usecase;

import cm.gimac.des.participant.application.port.out.RegistreLecture;
import cm.gimac.des.participant.application.port.out.RegistreLecture.Correspondant;
import cm.gimac.des.participant.application.port.out.RegistreLecture.Identite;
import cm.gimac.des.participant.application.port.out.RegistreLecture.Service;
import cm.gimac.des.participant.application.port.out.VersionsStatutRegistre;
import cm.gimac.des.participant.domain.ParticipantException;
import java.util.List;
import java.util.Map;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lecture du registre officiel par la Sandbox GIMAC (US-EXI-21, D-120), sous les comptes de service sbx-espace,
 * sbx-raccordement et sbx-referentiel (jeton de service, INTERNAL_PARTICIPANT_READ) : fiche d'une institution (code,
 * raison sociale, statut et sa version, correspondants actifs) et nomenclature des services souscrits. Consultation
 * seule : aucune méthode n'écrit.
 */
@org.springframework.stereotype.Service
@Transactional(readOnly = true)
public class RegistreSandboxQueries {

    public static final int TAILLE_MAX = 100;

    /**
     * Fiche lue par SBX-ESPACE à l'ouverture d'un espace, et par SBX-RACCORDEMENT pour les services souscrits de
     * l'institution (« services souscrits lus sur la fiche », CC-SBX-GIMAC-02 algorithme 6.7).
     */
    public record Fiche(
        String codeParticipant,
        String raisonSociale,
        String statut,
        long versionStatut,
        List<Correspondant> correspondants,
        List<String> servicesSouscrits
    ) {}

    /**
     * Correspondance entre l'identifiant d'une institution au registre (claim institution_id de la plateforme) et son code
     * participant (claim institution_id de la sandbox, CC-SBX-GIMAC-02), lue par MS-IAM (D-134). Code absent tant que
     * l'institution n'en a pas reçu.
     */
    public record Correspondance(java.util.UUID institutionId, String codeParticipant) {}

    /** Page des membres du registre (D-140). */
    public record PageMembres(List<RegistreLecture.Membre> elements, long total, int page, int taille) {}

    /** Page de la nomenclature des services. */
    public record PageServices(List<Service> contenu, long total, int page, int taille) {}

    private final RegistreLecture registre;
    private final VersionsStatutRegistre versions;

    public RegistreSandboxQueries(RegistreLecture registre, VersionsStatutRegistre versions) {
        this.registre = registre;
        this.versions = versions;
    }

    /** Fiche de l'institution de ce code ; 404 PAR_INSTITUTION_NOT_FOUND si le code est absent du registre. */
    public Fiche fiche(String codeParticipant) {
        String code = codeParticipant == null ? "" : codeParticipant.trim();
        Identite i = registre.parCode(code).orElseThrow(() -> ParticipantException.notFound("PAR_INSTITUTION_NOT_FOUND", code));
        return new Fiche(i.codeParticipant(), i.raisonSociale(), i.statut(), versions.courante(i.id()), registre.correspondantsActifs(i.id()), registre.servicesSouscrits(i.id()));
    }

    /** Code participant de l'institution de cet identifiant ; 404 PAR_INSTITUTION_NOT_FOUND si l'identifiant est absent. */
    public Correspondance correspondance(java.util.UUID institutionId) {
        Identite i = registre.parId(institutionId).orElseThrow(() -> ParticipantException.notFound("PAR_INSTITUTION_NOT_FOUND", String.valueOf(institutionId)));
        return new Correspondance(i.id(), i.codeParticipant());
    }

    /** Identifiant au registre de l'institution de ce code ; 404 PAR_INSTITUTION_NOT_FOUND si le code est absent. */
    public Correspondance correspondance(String codeParticipant) {
        String code = codeParticipant == null ? "" : codeParticipant.trim();
        Identite i = registre.parCode(code).orElseThrow(() -> ParticipantException.notFound("PAR_INSTITUTION_NOT_FOUND", code));
        return new Correspondance(i.id(), i.codeParticipant());
    }

    /**
     * Institutions dotées d'un code participant (D-140, provision des comptes de test de SBX-DONNEES), triées par code,
     * filtrées par statut ; taille de 1 à 100, statut inconnu : 422 PAR_STATUT_INVALIDE.
     */
    public PageMembres membres(java.util.Collection<String> statuts, int page, int taille) {
        verifierPage(page, taille);
        java.util.Set<String> filtre = new java.util.TreeSet<>();
        for (String s : statuts == null ? java.util.List.<String>of() : statuts) {
            if (s == null || s.isBlank()) {
                continue;
            }
            String v = s.strip();
            if (java.util.Arrays.stream(cm.gimac.des.participant.domain.enumeration.InstitutionStatus.values()).noneMatch(x -> x.name().equals(v))) {
                throw ParticipantException.unprocessable("PAR_STATUT_INVALIDE", "Statut inconnu", Map.of("statut", v));
            }
            filtre.add(v);
        }
        return new PageMembres(registre.membres(filtre, page, taille), registre.nombreMembres(filtre), page, taille);
    }

    private static void verifierPage(int page, int taille) {
        if (page < 0 || taille < 1 || taille > TAILLE_MAX) {
            throw ParticipantException.unprocessable(
                "PAR_PAGINATION_INVALIDE",
                "Page ou taille invalide",
                Map.of("page", page, "taille", taille, "tailleMax", TAILLE_MAX)
            );
        }
    }

    /** Nomenclature des services souscrits, triée par code ; taille de 1 à 100. */
    public PageServices nomenclature(int page, int taille) {
        verifierPage(page, taille);
        return new PageServices(registre.services(page, taille), registre.nombreServices(), page, taille);
    }
}
