package cm.gimac.des.gateway.web.filter;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServerRequest;

/**
 * Adresse source de l'appelant, posée par la passerelle et par elle seule (décision de l'utilisateur du 21/11/2026,
 * recette ISO 20022, AUTH_E05) : tout en-tête X-Adresse-Source reçu du client est retiré sur toutes les routes, puis
 * reposé depuis le pair TCP réel de la connexion. Le port de la passerelle étant publié, un X-Forwarded-For peut être
 * forgé par n'importe quel client ; il n'est donc lu (dernier élément, ajouté par nginx avec
 * $proxy_add_x_forwarded_for) que si le pair est un mandataire de confiance désigné par son nom ou son adresse
 * (gimac.passerelle.adresse-source.mandataires, vide par défaut : personne). Les façades de la Sandbox GIMAC filtrent
 * sur cet en-tête les adresses sources déclarées par chaque participant à SBX-RACCORDEMENT.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AdresseSourceWebFilter implements WebFilter {

    public static final String EN_TETE = "X-Adresse-Source";
    static final Duration RESOLUTION = Duration.ofMinutes(1);

    private final List<String> mandataires;
    private final Clock horloge;
    private final Map<String, Resolu> resolus = new ConcurrentHashMap<>();

    record Resolu(Set<String> adresses, Instant le) {}

    @org.springframework.beans.factory.annotation.Autowired
    public AdresseSourceWebFilter(@Value("${gimac.passerelle.adresse-source.mandataires:}") List<String> mandataires) {
        this(mandataires, Clock.systemUTC());
    }

    AdresseSourceWebFilter(List<String> mandataires, Clock horloge) {
        this.mandataires = mandataires.stream().map(String::trim).filter(m -> !m.isEmpty()).toList();
        this.horloge = horloge;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String adresse = adresse(exchange.getRequest());
        ServerHttpRequest requete = exchange
            .getRequest()
            .mutate()
            .headers(h -> {
                h.remove(EN_TETE);
                if (!adresse.isEmpty()) {
                    h.set(EN_TETE, adresse);
                }
            })
            .build();
        return chain.filter(exchange.mutate().request(requete).build());
    }

    /** Pair TCP réel ; dernier élément de X-Forwarded-For s'il vient d'un mandataire de confiance. */
    String adresse(ServerHttpRequest requete) {
        String pair = texte(pair(requete));
        if (pair.isEmpty() || !deConfiance(pair)) {
            return pair;
        }
        String transmis = requete.getHeaders().getFirst("X-Forwarded-For");
        if (transmis == null || transmis.isBlank()) {
            return pair;
        }
        String[] elements = transmis.split(",");
        String dernier = elements[elements.length - 1].trim();
        return litteral(dernier) ? dernier : pair;
    }

    /** Pair de la connexion : celui de Netty, jamais réécrit par la stratégie des en-têtes transmis. */
    static SocketAddress pair(ServerHttpRequest requete) {
        Object natif;
        try {
            natif = ServerHttpRequestDecorator.getNativeRequest(requete);
        } catch (IllegalStateException e) {
            // Requête sans serveur sous-jacent (requête simulée) : son adresse distante.
            natif = null;
        }
        return natif instanceof HttpServerRequest netty ? netty.connectionRemoteAddress() : requete.getRemoteAddress();
    }

    static String texte(SocketAddress a) {
        if (a instanceof InetSocketAddress i) {
            return i.getAddress() != null ? i.getAddress().getHostAddress() : i.getHostString();
        }
        return "";
    }

    /** Adresse IPv4 ou IPv6 écrite en clair, jamais un nom (aucune résolution d'une valeur venue du réseau). */
    static boolean litteral(String s) {
        return s.matches("\\d{1,3}(\\.\\d{1,3}){3}") || (s.contains(":") && s.matches("[0-9A-Fa-f:.]+"));
    }

    boolean deConfiance(String pair) {
        return mandataires.stream().anyMatch(m -> resoudre(m).contains(pair));
    }

    Set<String> resoudre(String mandataire) {
        Instant maintenant = horloge.instant();
        Resolu r = resolus.get(mandataire);
        if (r == null || r.le().plus(RESOLUTION).isBefore(maintenant)) {
            r = new Resolu(adresses(mandataire), maintenant);
            resolus.put(mandataire, r);
        }
        return r.adresses();
    }

    static Set<String> adresses(String nom) {
        try {
            return Arrays.stream(InetAddress.getAllByName(nom)).map(InetAddress::getHostAddress).collect(Collectors.toUnmodifiableSet());
        } catch (UnknownHostException e) {
            return Set.of();
        }
    }
}
