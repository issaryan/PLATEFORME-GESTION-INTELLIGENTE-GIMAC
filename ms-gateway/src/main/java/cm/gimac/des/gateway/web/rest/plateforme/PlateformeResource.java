package cm.gimac.des.gateway.web.rest.plateforme;

import cm.gimac.des.gateway.domain.supervision.ChangementJournalisation;
import cm.gimac.des.gateway.domain.supervision.ConfigurationPlateforme;
import cm.gimac.des.gateway.domain.supervision.EtatAlertes;
import cm.gimac.des.gateway.domain.supervision.EtatServeurs;
import cm.gimac.des.gateway.domain.supervision.InstantanePlateforme;
import cm.gimac.des.gateway.domain.supervision.NiveauxJournalisation;
import cm.gimac.des.gateway.domain.supervision.ServiceInconnuException;
import cm.gimac.des.gateway.security.AuthoritiesConstants;
import cm.gimac.des.gateway.service.exploitation.ExploitationInfrastructureService;
import cm.gimac.des.gateway.service.supervision.SupervisionPlateformeService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.security.Principal;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Supervision de la plateforme pour le super administrateur (D-61) : état des services et des
 * serveurs, alertes, configuration effective, niveaux de journalisation et flux temps réel.
 * La chaîne de sécurité exige une session à deux facteurs sur /api/plateforme/** ; chaque route
 * exige en plus sa permission.
 */
@RestController
@RequestMapping("/api/plateforme")
public class PlateformeResource {

    static final String LECTURE_SUPERVISION = "hasAuthority('" + AuthoritiesConstants.PLATFORM_SUPERVISION_READ + "')";
    static final String LECTURE_CONFIGURATION = "hasAuthority('" + AuthoritiesConstants.PLATFORM_CONFIG_READ + "')";
    static final String MODIFICATION_CONFIGURATION = "hasAuthority('" + AuthoritiesConstants.PLATFORM_CONFIG_EDIT + "')";

    /** Demande de changement de niveau : logger (ou ROOT), niveau (TRACE à OFF, NULL pour hériter), motif obligatoire. */
    public record DemandeNiveau(
        @NotBlank @Size(max = 200) String logger,
        @NotBlank @Size(max = 10) String niveau,
        @NotBlank @Size(min = 5, max = 500) String motif
    ) {}

    /** Événement du flux temps réel portant une opération d'exploitation de l'infrastructure (D-78). */
    static final String EVENEMENT_OPERATION = "operation-infrastructure";

    private final SupervisionPlateformeService supervision;
    private final ObjectProvider<ExploitationInfrastructureService> exploitation;

    public PlateformeResource(SupervisionPlateformeService supervision) {
        this(supervision, null);
    }

    @Autowired
    public PlateformeResource(SupervisionPlateformeService supervision, ObjectProvider<ExploitationInfrastructureService> exploitation) {
        this.supervision = supervision;
        this.exploitation = exploitation;
    }

    /** État de chaque service (santé, latence, construction, métriques), de Keycloak et synthèse. */
    @GetMapping("/services")
    @PreAuthorize(LECTURE_SUPERVISION)
    public Mono<InstantanePlateforme> services() {
        return supervision.instantane();
    }

    /** État des serveurs d'après Prometheus : cibles, hôtes, conteneurs. */
    @GetMapping("/serveurs")
    @PreAuthorize(LECTURE_SUPERVISION)
    public Mono<EtatServeurs> serveurs() {
        return supervision.etatServeurs();
    }

    /** Alertes actives d'Alertmanager. */
    @GetMapping("/alertes")
    @PreAuthorize(LECTURE_SUPERVISION)
    public Mono<EtatAlertes> alertes() {
        return supervision.etatAlertes();
    }

    /** Configuration effective : routes de la passerelle et propriétés de chaque service, secrets masqués. */
    @GetMapping("/configuration")
    @PreAuthorize(LECTURE_CONFIGURATION)
    public Mono<ConfigurationPlateforme> configuration() {
        return supervision.configuration();
    }

    /** Niveaux de journalisation d'un service, filtrés sur un préfixe de logger facultatif. */
    @GetMapping("/journalisation/{service}")
    @PreAuthorize(LECTURE_CONFIGURATION)
    public Mono<NiveauxJournalisation> journalisation(@PathVariable String service, @RequestParam(required = false) String filtre) {
        return supervision.journalisation(service, filtre).onErrorMap(PlateformeResource::traduire);
    }

    /** Change à chaud le niveau d'un logger d'un service ; motif obligatoire, changement journalisé. */
    @PostMapping("/journalisation/{service}")
    @PreAuthorize(MODIFICATION_CONFIGURATION)
    public Mono<ChangementJournalisation> modifierJournalisation(
        @PathVariable String service,
        @Valid @RequestBody DemandeNiveau demande,
        Mono<Principal> utilisateur
    ) {
        return utilisateur
            .map(Principal::getName)
            .defaultIfEmpty("inconnu")
            .flatMap(auteur -> supervision.modifierJournalisation(service, demande.logger(), demande.niveau(), demande.motif(), auteur))
            .onErrorMap(PlateformeResource::traduire);
    }

    /**
     * Flux temps réel (Server-Sent Events) : instantané des services et nombre d'alertes, à intervalle
     * régulier (événement « plateforme ») ; pour qui porte PLATFORM_OPS_READ, opérations d'exploitation
     * de l'infrastructure demandées et terminées (événement « operation-infrastructure », D-78).
     */
    @GetMapping(value = "/flux", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize(LECTURE_SUPERVISION)
    public Flux<ServerSentEvent<Object>> flux(Authentication auth) {
        Flux<ServerSentEvent<Object>> impulsions = supervision
            .flux()
            .map(impulsion -> ServerSentEvent.<Object>builder(impulsion).id(Long.toString(impulsion.sequence())).event("plateforme").build());
        ExploitationInfrastructureService ops = exploitation == null ? null : exploitation.getIfAvailable();
        if (ops == null || !autorise(auth, AuthoritiesConstants.PLATFORM_OPS_READ)) {
            return impulsions;
        }
        Flux<ServerSentEvent<Object>> operations = ops
            .diffusion()
            .map(op -> ServerSentEvent.<Object>builder(op).id(op.id() + "-" + op.resultat()).event(EVENEMENT_OPERATION).build());
        return Flux.merge(impulsions, operations);
    }

    static boolean autorise(Authentication auth, String permission) {
        return auth != null && auth.getAuthorities().stream().anyMatch(a -> permission.equals(a.getAuthority()));
    }

    static Throwable traduire(Throwable e) {
        if (e instanceof ServiceInconnuException) {
            return new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
        if (e instanceof IllegalArgumentException) {
            return new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
        if (e instanceof ResponseStatusException) {
            return e;
        }
        return new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Service injoignable ou en erreur");
    }
}
