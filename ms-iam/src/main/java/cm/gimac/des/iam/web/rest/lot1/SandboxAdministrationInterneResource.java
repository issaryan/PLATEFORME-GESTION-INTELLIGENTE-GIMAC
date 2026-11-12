package cm.gimac.des.iam.web.rest.lot1;

import cm.gimac.des.iam.application.usecase.AdministrationDelegueeService;
import cm.gimac.des.iam.application.usecase.AdministrationDelegueeService.NouveauCollaborateur;
import cm.gimac.des.iam.domain.IamException;
import cm.gimac.des.iam.domain.compte.Acteur;
import cm.gimac.des.iam.domain.compte.Compte;
import cm.gimac.des.iam.domain.service.ReglesAdministrationDeleguee;
import cm.gimac.des.iam.web.rest.lot1.AdministrationDelegueeResource.CreationRequest;
import cm.gimac.des.iam.web.rest.lot1.AdministrationDelegueeResource.EtatRequest;
import cm.gimac.des.iam.web.rest.lot1.AdministrationDelegueeResource.RolesRequest;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Administration déléguée des comptes d'une institution appelée par SBX-ESPACE avec son compte de service (D-144) :
 * mêmes actes et mêmes règles que /api/v1/administration-deleguee (D-115, US-EXI-03), sous INTERNAL_IAM_SBX_SAGA. Le
 * jeton réduit que la passerelle remet à SBX-ESPACE n'a pour audience que SBX-ESPACE (D-134) et n'est jamais relayé :
 * l'institution ({@code codeParticipant}) et l'auteur ({@code acteur}, compte de l'administrateur de l'institution)
 * passent en paramètres, après vérification du jeton, de la permission et du second facteur par SBX-ESPACE. Chaque
 * acte reste journalisé au nom de l'auteur. Contrat : contrats/iam-administration-deleguee-interne.json.
 */
@RestController
@RequestMapping("/api/v1/interne/sandbox/administration-deleguee")
@PreAuthorize("hasAuthority('INTERNAL_IAM_SBX_SAGA')")
public class SandboxAdministrationInterneResource {

    static final String CLE = "Idempotency-Key";
    /** Code participant admis pour la sandbox (même règle que les périmètres), jamais un identifiant au registre. */
    private static final Pattern CODE = Pattern.compile("^[A-Za-z0-9_-]{1,64}$");
    private static final Pattern UUID_TEXTE = Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

    /** Compte rendu à SBX-ESPACE : la fiche de l'administration déléguée et le code participant de son institution. */
    public record CompteDelegue(
        String id,
        String identifiant,
        String prenom,
        String nom,
        String email,
        boolean actif,
        String institutionId,
        String codeParticipant,
        Set<String> profils,
        boolean secondFacteur,
        List<String> actionsRequises
    ) {
        static CompteDelegue de(Compte c, String code) {
            return new CompteDelegue(
                c.id(),
                c.identifiant(),
                c.prenom(),
                c.nom(),
                c.email(),
                c.actif(),
                c.institutionId(),
                code,
                new java.util.TreeSet<>(c.profils()),
                c.secondFacteur(),
                c.actionsRequises().stream().map(Enum::name).toList()
            );
        }
    }

    public record PageDelegue(List<CompteDelegue> contenu, long total, int page, int taille) {}

    public record CreationDeleguee(CompteDelegue compte, boolean invitationEnvoyee, boolean rejouee) {}

    private final AdministrationDelegueeService service;

    public SandboxAdministrationInterneResource(AdministrationDelegueeService service) {
        this.service = service;
    }

    /** Institution désignée par son code participant : une autre forme est refusée (400), jamais devinée. */
    static String code(String codeParticipant) {
        String c = codeParticipant == null ? "" : codeParticipant.strip();
        if (!CODE.matcher(c).matches() || UUID_TEXTE.matcher(c).matches()) {
            throw IamException.invalide("IAM_SBX_INSTITUTION_INVALIDE", "Code participant attendu", Map.of());
        }
        return c;
    }

    /** Auteur de l'acte : le compte de l'administrateur de l'institution vérifié par SBX-ESPACE, jamais le compte de service. */
    static Acteur acteur(String acteur, String nom) {
        if (acteur == null || acteur.isBlank()) {
            throw IamException.invalide("IAM_SBX_ACTEUR_REQUIS", "Auteur de l'acte attendu", Map.of());
        }
        String id = acteur.strip();
        return new Acteur(id, nom == null || nom.isBlank() ? id : nom.strip(), Set.of(ReglesAdministrationDeleguee.ADMINISTRATION));
    }

    @GetMapping("/roles")
    public Set<String> roles() {
        return service.roles();
    }

    @GetMapping("/comptes")
    public PageDelegue comptes(
        @RequestParam(name = "codeParticipant") String codeParticipant,
        @RequestParam(name = "page", defaultValue = "0") int page,
        @RequestParam(name = "taille", defaultValue = "20") int taille
    ) {
        String code = code(codeParticipant);
        var p = service.lister(code, page, taille);
        return new PageDelegue(p.contenu().stream().map(c -> CompteDelegue.de(c, code)).toList(), p.total(), p.page(), p.taille());
    }

    @GetMapping("/comptes/{id}")
    public CompteDelegue compte(@PathVariable String id, @RequestParam(name = "codeParticipant") String codeParticipant) {
        String code = code(codeParticipant);
        return CompteDelegue.de(service.lire(code, id), code);
    }

    @PostMapping("/comptes")
    public ResponseEntity<CreationDeleguee> creer(
        @RequestParam(name = "codeParticipant") String codeParticipant,
        @RequestParam(name = "acteur") String acteur,
        @RequestParam(name = "acteurNom", required = false) String acteurNom,
        @Valid @RequestBody CreationRequest r,
        @RequestHeader(name = CLE, required = false) String cle
    ) {
        String code = code(codeParticipant);
        var c = service.creer(
            acteur(acteur, acteurNom),
            code,
            new NouveauCollaborateur(r.identifiant().strip(), r.prenom().strip(), r.nom().strip(), r.email().strip(), r.telephone(), r.fonction(), r.roles()),
            cle
        );
        return ResponseEntity.status(c.rejouee() ? HttpStatus.OK : HttpStatus.CREATED)
            .location(URI.create("/api/v1/interne/sandbox/administration-deleguee/comptes/" + c.compte().id()))
            .body(new CreationDeleguee(CompteDelegue.de(c.compte(), code), c.invitationEnvoyee(), c.rejouee()));
    }

    @PutMapping("/comptes/{id}/roles")
    public CompteDelegue roles(
        @PathVariable String id,
        @RequestParam(name = "codeParticipant") String codeParticipant,
        @RequestParam(name = "acteur") String acteur,
        @RequestParam(name = "acteurNom", required = false) String acteurNom,
        @Valid @RequestBody RolesRequest r,
        @RequestHeader(name = CLE, required = false) String cle
    ) {
        String code = code(codeParticipant);
        return CompteDelegue.de(service.definirRoles(acteur(acteur, acteurNom), code, id, r.roles(), r.motif(), cle), code);
    }

    @PutMapping("/comptes/{id}/etat")
    public CompteDelegue etat(
        @PathVariable String id,
        @RequestParam(name = "codeParticipant") String codeParticipant,
        @RequestParam(name = "acteur") String acteur,
        @RequestParam(name = "acteurNom", required = false) String acteurNom,
        @Valid @RequestBody EtatRequest r,
        @RequestHeader(name = CLE, required = false) String cle
    ) {
        String code = code(codeParticipant);
        return CompteDelegue.de(service.definirActif(acteur(acteur, acteurNom), code, id, r.actif(), r.motif(), cle), code);
    }
}
