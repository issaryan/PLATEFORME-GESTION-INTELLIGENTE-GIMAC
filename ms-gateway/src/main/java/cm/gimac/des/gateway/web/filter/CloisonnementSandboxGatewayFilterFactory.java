package cm.gimac.des.gateway.web.filter;

import cm.gimac.des.gateway.domain.sandbox.ContexteAction;
import cm.gimac.des.gateway.domain.sandbox.RouteSandbox;
import cm.gimac.des.gateway.service.sandbox.CloisonnementSandbox;
import cm.gimac.des.gateway.service.sandbox.CloisonnementSandbox.Demande;
import cm.gimac.des.gateway.service.sandbox.CloisonnementSandbox.Resultat;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.OrderedGatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Filtre « CloisonnementSandbox » des routes de la sandbox (D-120, US-EXI-07), placé après OpaAuthorize et après le
 * relais du jeton : institution effective (propre, ou mandante choisie par l'en-tête {@value #ENTETE_MANDANTE} tant que
 * le mandat est actif), jeton interne réduit relayé à la place du jeton de session quand l'émetteur de la passerelle
 * est actif, quota de débit, décision journalisée. Hors mandat : 404 au format RFC 9457, sans révéler l'existence de
 * la ressource ; quota dépassé : 429.
 */
@Component
public class CloisonnementSandboxGatewayFilterFactory extends AbstractGatewayFilterFactory<Object> {

    /** Institution mandante choisie par un partenaire technique mandaté (sélecteur de la barre supérieure). */
    public static final String ENTETE_MANDANTE = "X-Institution-Mandante";

    /** Après les filtres de la route et le relais du jeton (ordres 1 à n) : le jeton réduit remplace celui relayé. */
    static final int ORDRE = 10_000;

    private static final Map<String, String[]> TITRES = Map.of(
        "fr", new String[] { "Ressource introuvable", "Trop de requêtes" },
        "en", new String[] { "Resource not found", "Too many requests" },
        "es", new String[] { "Recurso no encontrado", "Demasiadas solicitudes" }
    );

    private final CloisonnementSandbox cloisonnement;

    public CloisonnementSandboxGatewayFilterFactory(CloisonnementSandbox cloisonnement) {
        super(Object.class);
        this.cloisonnement = cloisonnement;
    }

    @Override
    public GatewayFilter apply(Object config) {
        return new OrderedGatewayFilter(
            (exchange, chain) -> {
                Optional<RouteSandbox> route = RouteSandbox.lire(OpaAuthorizeGatewayFilterFactory.cheminOrigine(exchange));
                if (route.isEmpty()) {
                    return refuser(exchange, 404);
                }
                return ReactiveSecurityContextHolder.getContext()
                    .map(SecurityContext::getAuthentication)
                    .filter(Authentication::isAuthenticated)
                    .flatMap(auth -> cloisonnement.evaluer(demande(exchange, auth, route.orElseThrow())))
                    .map(Optional::of)
                    .defaultIfEmpty(Optional.empty())
                    .flatMap(r ->
                        r.isPresent() && r.orElseThrow().statut() == 200 ? chain.filter(relayer(exchange, r.orElseThrow())) : refuser(exchange, r.map(Resultat::statut).orElse(404))
                    );
            },
            ORDRE
        );
    }

    static Demande demande(ServerWebExchange exchange, Authentication auth, RouteSandbox route) {
        InetSocketAddress distante = exchange.getRequest().getRemoteAddress();
        List<String> permissions = auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
        return new Demande(
            OpaAuthorizeGatewayFilterFactory.claim(auth, "sub"),
            OpaAuthorizeGatewayFilterFactory.claim(auth, OpaAuthorizeGatewayFilterFactory.INSTITUTION_CLAIM),
            exchange.getRequest().getHeaders().getFirst(ENTETE_MANDANTE),
            route,
            exchange.getRequest().getMethod().name(),
            OpaAuthorizeGatewayFilterFactory.cheminOrigine(exchange),
            permissions,
            OpaAuthorizeGatewayFilterFactory.claim(auth, OpaAuthorizeGatewayFilterFactory.ACR_CLAIM),
            distante == null ? "" : distante.getHostString(),
            jetonOriginal(exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION)),
            exchange.getRequest().getHeaders().getFirst(ContexteAction.ENTETE)
        );
    }

    private static String jetonOriginal(String autorisation) {
        if (autorisation == null || !autorisation.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return "";
        }
        return autorisation.substring(7).trim();
    }

    /** En-tête de choix retiré ; jeton réduit à la place du jeton relayé quand il existe. */
    static ServerWebExchange relayer(ServerWebExchange exchange, Resultat r) {
        return exchange
            .mutate()
            .request(req ->
                req.headers(h -> {
                    h.remove(ENTETE_MANDANTE);
                    h.remove(ContexteAction.ENTETE);
                    r.jeton().ifPresent(h::setBearerAuth);
                })
            )
            .build();
    }

    static Mono<Void> refuser(ServerWebExchange exchange, int statut) {
        String langue = langue(exchange.getRequest().getHeaders().getFirst(HttpHeaders.ACCEPT_LANGUAGE));
        boolean quota = statut == 429;
        String code = quota ? "GW_SBX_QUOTA_DEPASSE" : "GW_SBX_INTROUVABLE";
        String titre = TITRES.get(langue)[quota ? 1 : 0];
        String corps =
            "{\"type\":\"https://gimac.cm/problemes/" + code.toLowerCase(Locale.ROOT).replace('_', '-') + "\",\"title\":\"" + titre + "\",\"status\":" + statut + ",\"code\":\"" + code + "\"}";
        var reponse = exchange.getResponse();
        reponse.setStatusCode(HttpStatus.valueOf(statut));
        reponse.getHeaders().setContentType(MediaType.APPLICATION_PROBLEM_JSON);
        if (quota) {
            reponse.getHeaders().set(HttpHeaders.RETRY_AFTER, "60");
        }
        DataBuffer tampon = reponse.bufferFactory().wrap(corps.getBytes(StandardCharsets.UTF_8));
        return reponse.writeWith(Mono.just(tampon));
    }

    /** Langue du problème : fr par défaut, en ou es selon la première langue acceptée. */
    static String langue(String acceptLanguage) {
        if (acceptLanguage == null || acceptLanguage.isBlank()) {
            return "fr";
        }
        String premiere = acceptLanguage.split(",")[0].trim().toLowerCase(Locale.ROOT);
        String code = premiere.length() >= 2 ? premiere.substring(0, 2) : premiere;
        return TITRES.containsKey(code) ? code : "fr";
    }
}
