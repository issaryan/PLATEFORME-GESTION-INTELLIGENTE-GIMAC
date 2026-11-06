package cm.gimac.des.participant.web.rest.lot1;

import cm.gimac.des.participant.application.usecase.RegistreSandboxQueries;
import cm.gimac.des.participant.application.usecase.RegistreSandboxQueries.Fiche;
import cm.gimac.des.participant.application.usecase.RegistreSandboxQueries.PageServices;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Interface interne du registre pour la Sandbox GIMAC (US-EXI-21, D-120, PAR-13) : jeton de service portant
 * INTERNAL_PARTICIPANT_READ (sbx-espace, sbx-raccordement, sbx-referentiel), jamais relayée par la passerelle.
 * Consultation seule ; toute écriture d'un compte de service est refusée et journalisée (JournalRefusSandbox).
 */
@RestController
@RequestMapping("/api/v1/interne")
public class RegistreSandboxInterneResource {

    private final RegistreSandboxQueries queries;

    public RegistreSandboxInterneResource(RegistreSandboxQueries queries) {
        this.queries = queries;
    }

    /** Fiche d'une institution : code participant, raison sociale, statut, version de statut, correspondants actifs. */
    @GetMapping("/institutions/code/{code}/fiche")
    public Fiche fiche(@PathVariable String code) {
        return queries.fiche(code);
    }

    /** Code participant de l'institution de cet identifiant (D-134, lu par MS-IAM). */
    @GetMapping("/institutions/{id}/correspondance")
    public RegistreSandboxQueries.Correspondance correspondance(@PathVariable java.util.UUID id) {
        return queries.correspondance(id);
    }

    /** Identifiant au registre de l'institution de ce code participant (D-134, lu par MS-IAM). */
    @GetMapping("/institutions/code/{code}/correspondance")
    public RegistreSandboxQueries.Correspondance correspondanceParCode(@PathVariable String code) {
        return queries.correspondance(code);
    }

    /** Institutions dotées d'un code participant, filtrées par statut (paramètre répétable), triées par code (D-140). */
    @GetMapping(path = "/institutions", params = "!codeParticipant")
    public RegistreSandboxQueries.PageMembres membres(
        @RequestParam(name = "statut", required = false) java.util.List<String> statuts,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "50") int taille
    ) {
        return queries.membres(statuts, page, taille);
    }

    /** Nomenclature des services souscrits (abonnements actifs), triée par code, paginée. */
    @GetMapping("/nomenclature/services")
    public PageServices services(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "50") int taille) {
        return queries.nomenclature(page, taille);
    }
}
