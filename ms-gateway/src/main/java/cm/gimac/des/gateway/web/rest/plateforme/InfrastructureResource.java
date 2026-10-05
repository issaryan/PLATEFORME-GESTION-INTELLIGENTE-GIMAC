package cm.gimac.des.gateway.web.rest.plateforme;

import cm.gimac.des.gateway.domain.exploitation.ActeurOperation;
import cm.gimac.des.gateway.domain.exploitation.Charge;
import cm.gimac.des.gateway.domain.exploitation.ExploitationException;
import cm.gimac.des.gateway.domain.exploitation.ExploitationException.Cause;
import cm.gimac.des.gateway.domain.exploitation.Geste;
import cm.gimac.des.gateway.domain.exploitation.JournauxCharge;
import cm.gimac.des.gateway.domain.exploitation.Operation;
import cm.gimac.des.gateway.domain.exploitation.PageOperations;
import cm.gimac.des.gateway.security.AuthoritiesConstants;
import cm.gimac.des.gateway.security.RevendicationsSession;
import cm.gimac.des.gateway.service.exploitation.ExploitationInfrastructureService;
import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Exploitation de l'infrastructure depuis la console (D-78, docs/exploitation/operations-infrastructure.md §3) :
 * contrôleur propre de la passerelle, sans relais vers un service. La chaîne de sécurité exige une
 * session à deux facteurs sur /api/plateforme/** ; chaque route exige sa permission ; chaque geste
 * exige en plus un motif et une assertion FIDO2 (en-tête X-Reauth-Assertion) vérifiée par MS-IAM.
 */
@RestController
@RequestMapping("/api/plateforme/infrastructure")
public class InfrastructureResource {

    static final String LECTURE = "hasAuthority('" + AuthoritiesConstants.PLATFORM_OPS_READ + "')";
    static final String EXECUTION = "hasAuthority('" + AuthoritiesConstants.PLATFORM_OPS_EXECUTE + "')";
    static final String REAUTH = "X-Reauth-Assertion";
    /** Indique l'orchestrateur configuré (DOCKER, KUBERNETES ou AUCUN : exploitation non configurée). */
    static final String ENTETE_ORCHESTRATEUR = "X-Orchestrateur";
    private static final Pattern NOM = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9_.-]{0,99}$");

    /** Corps d'un geste : motif (5 à 500 caractères) ; répliques (0 à 5) pour la mise à l'échelle seulement. */
    public record DemandeGeste(String motif, Integer repliques) {}

    private final ExploitationInfrastructureService exploitation;

    public InfrastructureResource(ExploitationInfrastructureService exploitation) {
        this.exploitation = exploitation;
    }

    /** Charges du périmètre avec les gestes permis ; liste vide et X-Orchestrateur: AUCUN si non configuré. */
    @GetMapping("/charges")
    @PreAuthorize(LECTURE)
    public Mono<ResponseEntity<List<Charge>>> charges() {
        return exploitation.charges().map(l -> ResponseEntity.ok().header(ENTETE_ORCHESTRATEUR, exploitation.type().name()).body(l));
    }

    /** Dernières lignes du journal d'une charge (500 au plus, 30 minutes au plus), masquées. */
    @GetMapping("/charges/{nom}/journaux")
    @PreAuthorize(LECTURE)
    public Mono<JournauxCharge> journaux(
        @PathVariable String nom,
        @RequestParam(name = "lignes", defaultValue = "200") int lignes,
        @RequestParam(name = "depuis", defaultValue = "PT10M") String depuis
    ) {
        return Mono.defer(() -> exploitation.journaux(nomValide(nom), lignes, duree(depuis)));
    }

    /** Historique des opérations, des plus récentes aux plus anciennes. */
    @GetMapping("/operations")
    @PreAuthorize(LECTURE)
    public Mono<PageOperations> operations(
        @RequestParam(name = "page", defaultValue = "0") int page,
        @RequestParam(name = "taille", defaultValue = "20") int taille
    ) {
        return exploitation.operations(page, taille);
    }

    @PostMapping("/charges/{nom}/redemarrage")
    @PreAuthorize(EXECUTION)
    public Mono<ResponseEntity<Operation>> redemarrer(
        @PathVariable String nom,
        @RequestBody(required = false) DemandeGeste corps,
        @RequestHeader(name = REAUTH, required = false) String assertion,
        Authentication auth
    ) {
        return geste(nom, Geste.REDEMARRER, corps, assertion, auth);
    }

    @PostMapping("/charges/{nom}/arret")
    @PreAuthorize(EXECUTION)
    public Mono<ResponseEntity<Operation>> arreter(
        @PathVariable String nom,
        @RequestBody(required = false) DemandeGeste corps,
        @RequestHeader(name = REAUTH, required = false) String assertion,
        Authentication auth
    ) {
        return geste(nom, Geste.ARRETER, corps, assertion, auth);
    }

    @PostMapping("/charges/{nom}/demarrage")
    @PreAuthorize(EXECUTION)
    public Mono<ResponseEntity<Operation>> demarrer(
        @PathVariable String nom,
        @RequestBody(required = false) DemandeGeste corps,
        @RequestHeader(name = REAUTH, required = false) String assertion,
        Authentication auth
    ) {
        return geste(nom, Geste.DEMARRER, corps, assertion, auth);
    }

    @PutMapping("/charges/{nom}/repliques")
    @PreAuthorize(EXECUTION)
    public Mono<ResponseEntity<Operation>> repliques(
        @PathVariable String nom,
        @RequestBody(required = false) DemandeGeste corps,
        @RequestHeader(name = REAUTH, required = false) String assertion,
        Authentication auth
    ) {
        return geste(nom, Geste.ECHELLE, corps, assertion, auth);
    }

    private Mono<ResponseEntity<Operation>> geste(String nom, Geste geste, DemandeGeste corps, String assertion, Authentication auth) {
        return Mono.defer(() -> {
            DemandeGeste c = corps == null ? new DemandeGeste(null, null) : corps;
            var demande = new ExploitationInfrastructureService.Demande(nomValide(nom), geste, c.motif(), c.repliques(), assertion, acteur(auth));
            return exploitation.executer(demande).map(op -> ResponseEntity.status(HttpStatus.ACCEPTED).body(op));
        });
    }

    /** Acteur : sujet du compte (claim sub, celui que MS-IAM attend) et nom affiché. */
    static ActeurOperation acteur(Authentication auth) {
        String sub = RevendicationsSession.claim(auth, "sub");
        String id = sub.isBlank() && auth != null ? auth.getName() : sub;
        String nom = RevendicationsSession.claim(auth, "name");
        if (nom.isBlank()) {
            nom = (RevendicationsSession.claim(auth, "given_name") + " " + RevendicationsSession.claim(auth, "family_name")).strip();
        }
        if (nom.isBlank()) {
            nom = RevendicationsSession.claim(auth, "preferred_username");
        }
        return new ActeurOperation(id, nom.isBlank() ? null : nom);
    }

    static String nomValide(String nom) {
        if (nom == null || !NOM.matcher(nom).matches()) {
            throw new ExploitationException(Cause.CHARGE_INCONNUE);
        }
        return nom;
    }

    static Duration duree(String valeur) {
        try {
            return Duration.parse(valeur);
        } catch (DateTimeParseException e) {
            throw new ExploitationException(Cause.PARAMETRE_INVALIDE, "Durée ISO 8601 attendue (PT10M)");
        }
    }

    /** Refus du contrat : application/problem+json portant le code (champ « code »). */
    @ExceptionHandler(ExploitationException.class)
    public ResponseEntity<Map<String, Object>> refus(ExploitationException e, ServerWebExchange exchange) {
        Cause c = e.causeRefus();
        Map<String, Object> corps = new LinkedHashMap<>();
        corps.put("type", "about:blank");
        HttpStatus statut = HttpStatus.resolve(c.statut());
        corps.put("title", statut == null ? "Error" : statut.getReasonPhrase());
        corps.put("status", c.statut());
        corps.put("detail", e.getMessage());
        corps.put("instance", exchange.getRequest().getPath().value());
        corps.put("code", c.code());
        return ResponseEntity.status(c.statut()).contentType(MediaType.APPLICATION_PROBLEM_JSON).body(corps);
    }
}
