package cm.gimac.des.gateway.infrastructure.supervision;

import static cm.gimac.des.gateway.infrastructure.supervision.ClientsSupervision.carte;
import static cm.gimac.des.gateway.infrastructure.supervision.ClientsSupervision.liste;
import static cm.gimac.des.gateway.infrastructure.supervision.ClientsSupervision.sansBarreFinale;
import static cm.gimac.des.gateway.infrastructure.supervision.ClientsSupervision.texte;

import cm.gimac.des.gateway.domain.supervision.EnvironnementService;
import cm.gimac.des.gateway.domain.supervision.EtatSante;
import cm.gimac.des.gateway.domain.supervision.FormatPrometheus;
import cm.gimac.des.gateway.domain.supervision.InfoService;
import cm.gimac.des.gateway.domain.supervision.Metriques;
import cm.gimac.des.gateway.domain.supervision.NiveauLogger;
import cm.gimac.des.gateway.domain.supervision.NiveauxJournalisation;
import cm.gimac.des.gateway.domain.supervision.Sante;
import cm.gimac.des.gateway.domain.supervision.ServiceCible;
import cm.gimac.des.gateway.domain.supervision.port.SondeService;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriUtils;
import reactor.core.publisher.Mono;

/**
 * Adaptateur WebClient du port {@link SondeService} : points /management/** des services
 * Spring Boot. Chaque appel porte, s'il est disponible, le jeton de service de la passerelle
 * (client OAuth2 « gateway-supervision ») : sans lui, la santé est lue sans le détail des
 * composants et l'environnement ou les loggers sont refusés par le service.
 */
public class ActuatorWebClientAdapter implements SondeService {

    static final ParameterizedTypeReference<Map<String, Object>> CARTE = new ParameterizedTypeReference<>() {};
    static final String GESTION = "/management";

    private final WebClient client;
    private final Supplier<Mono<String>> jeton;
    private final String gestion;

    /**
     * @param client client HTTP sans adresse de base (les adresses sont absolues)
     * @param jeton  fournisseur du jeton de service ; vide s'il n'est pas disponible
     */
    public ActuatorWebClientAdapter(WebClient client, Supplier<Mono<String>> jeton) {
        this(client, jeton, GESTION);
    }

    public ActuatorWebClientAdapter(WebClient client, Supplier<Mono<String>> jeton, String gestion) {
        this.client = client;
        this.jeton = jeton;
        this.gestion = gestion;
    }

    @Override
    public Mono<Sante> sante(ServiceCible service) {
        return santeA(adresseAvecBase(service, "/health"));
    }

    @Override
    public Mono<Sante> santeExterne(String url) {
        return santeA(url);
    }

    private Mono<Sante> santeA(String url) {
        return requete(HttpMethod.GET, url, null).flatMap(spec ->
            spec.exchangeToMono(reponse ->
                reponse
                    .bodyToMono(CARTE)
                    .onErrorResume(e -> Mono.empty())
                    .defaultIfEmpty(Map.of())
                    .map(corps -> lireSante(corps, reponse.statusCode()))
            )
        );
    }

    /** L'état est lu dans le corps ; à défaut, déduit du statut HTTP (2xx : UP, 503 : DOWN). */
    static Sante lireSante(Map<String, Object> corps, HttpStatusCode statut) {
        EtatSante etat;
        if (corps.get("status") != null) {
            etat = EtatSante.depuis(corps.get("status"));
        } else if (statut.is2xxSuccessful()) {
            etat = EtatSante.UP;
        } else if (statut.value() == 503) {
            etat = EtatSante.DOWN;
        } else {
            etat = EtatSante.INCONNU;
        }
        Map<String, String> composants = new LinkedHashMap<>();
        // Spring Boot : components.{nom}.status
        carte(corps.get("components")).forEach((nom, detail) -> composants.put(nom, String.valueOf(EtatSante.depuis(carte(detail).get("status")))));
        // Keycloak (MicroProfile Health) : checks[].{name, status}
        for (Object verification : liste(corps.get("checks"))) {
            Map<String, Object> v = carte(verification);
            if (v.get("name") != null) {
                composants.put(v.get("name").toString(), String.valueOf(EtatSante.depuis(v.get("status"))));
            }
        }
        return new Sante(etat, composants);
    }

    @Override
    public Mono<InfoService> info(ServiceCible service) {
        return requete(HttpMethod.GET, adresseAvecBase(service, "/info"), null)
            .flatMap(spec -> spec.retrieve().bodyToMono(CARTE))
            .map(ActuatorWebClientAdapter::lireInfo);
    }

    static InfoService lireInfo(Map<String, Object> corps) {
        Map<String, Object> build = carte(corps.get("build"));
        Map<String, Object> git = carte(corps.get("git"));
        Map<String, Object> commit = carte(git.get("commit"));
        Object id = commit.get("id");
        String commitId = id instanceof Map<?, ?> m ? texte(Optional.ofNullable(m.get("abbrev")).orElse(m.get("full"))) : texte(id);
        return new InfoService(
            texte(build.get("version")),
            texte(Optional.ofNullable(build.get("artifact")).orElse(build.get("name"))),
            texte(build.get("time")),
            commitId,
            texte(git.get("branch"))
        );
    }

    @Override
    public Mono<Metriques> metriques(ServiceCible service) {
        return requete(HttpMethod.GET, adresseAvecBase(service, "/prometheus"), null)
            .flatMap(spec ->
                spec
                    .accept(MediaType.TEXT_PLAIN)
                    .retrieve()
                    .bodyToFlux(String.class)
                    .filter(ligne -> FormatPrometheus.nomMetrique(ligne).filter(FormatPrometheus.METRIQUES_EXPLOITATION::contains).isPresent())
                    .collectList()
            )
            .map(lignes -> FormatPrometheus.extraire(FormatPrometheus.analyser(lignes, FormatPrometheus.METRIQUES_EXPLOITATION::contains)));
    }

    @Override
    public Mono<EnvironnementService> environnement(ServiceCible service) {
        return requete(HttpMethod.GET, adresseAvecBase(service, "/env"), null)
            .flatMap(spec -> spec.retrieve().bodyToMono(CARTE))
            .map(ActuatorWebClientAdapter::lireEnvironnement);
    }

    /** Valeur effective d'une propriété : celle de la première source qui la définit (ordre de priorité). */
    static EnvironnementService lireEnvironnement(Map<String, Object> corps) {
        List<String> profils = liste(corps.get("activeProfiles")).stream().map(String::valueOf).toList();
        Map<String, String> proprietes = new LinkedHashMap<>();
        for (Object source : liste(corps.get("propertySources"))) {
            carte(carte(source).get("properties")).forEach((cle, detail) -> {
                if (!proprietes.containsKey(cle)) {
                    proprietes.put(cle, String.valueOf(carte(detail).get("value")));
                }
            });
        }
        return new EnvironnementService(profils, proprietes);
    }

    @Override
    public Mono<NiveauxJournalisation> journalisation(ServiceCible service) {
        return requete(HttpMethod.GET, adresseAvecBase(service, "/loggers"), null)
            .flatMap(spec -> spec.retrieve().bodyToMono(CARTE))
            .map(corps -> lireLoggers(service.nom(), corps));
    }

    static NiveauxJournalisation lireLoggers(String service, Map<String, Object> corps) {
        List<String> niveaux = liste(corps.get("levels")).stream().map(String::valueOf).toList();
        List<NiveauLogger> loggers = new ArrayList<>();
        carte(corps.get("loggers")).forEach((nom, detail) -> {
            Map<String, Object> d = carte(detail);
            loggers.add(new NiveauLogger(nom, texte(d.get("configuredLevel")), texte(d.get("effectiveLevel"))));
        });
        return new NiveauxJournalisation(service, niveaux, loggers);
    }

    @Override
    public Mono<Void> modifierNiveau(ServiceCible service, String logger, String niveau) {
        String url = adresseAvecBase(service, "/loggers/" + UriUtils.encodePathSegment(logger, StandardCharsets.UTF_8));
        // Niveau nul admis : Actuator rétablit alors le niveau hérité (Map.of refuserait null).
        Map<String, String> corps = new HashMap<>();
        corps.put("configuredLevel", niveau);
        return requete(HttpMethod.POST, url, corps)
            .flatMap(spec -> spec.retrieve().toBodilessEntity())
            .then();
    }

    // ------------------------------------------------------------------ Outils

    static String adresse(ServiceCible service, String chemin) {
        return sansBarreFinale(service.url()) + GESTION + chemin;
    }

    private String adresseAvecBase(ServiceCible service, String chemin) {
        return sansBarreFinale(service.url()) + gestion + chemin;
    }

    private Mono<WebClient.RequestHeadersSpec<?>> requete(HttpMethod methode, String url, Object corps) {
        return Mono.defer(jeton)
            .map(Optional::of)
            .onErrorResume(e -> Mono.just(Optional.empty()))
            .defaultIfEmpty(Optional.empty())
            .map(j -> {
                WebClient.RequestBodySpec spec = client.method(methode).uri(URI.create(url));
                j.ifPresent(valeur -> spec.headers(h -> h.setBearerAuth(valeur)));
                return corps == null ? spec : spec.contentType(MediaType.APPLICATION_JSON).bodyValue(corps);
            });
    }
}
