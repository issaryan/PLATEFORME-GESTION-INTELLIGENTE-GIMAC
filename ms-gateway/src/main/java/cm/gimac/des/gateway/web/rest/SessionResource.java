package cm.gimac.des.gateway.web.rest;

import cm.gimac.des.gateway.security.session.ParametresSession;
import cm.gimac.des.gateway.security.session.SessionGimac;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.WebSession;
import reactor.core.publisher.Mono;

/**
 * Session de l'utilisateur pour les couches de présentation (D-58, D-61, D-65) : la matrice des
 * rôles (permissions), le niveau d'assurance du jeton obtenu à la connexion, la fin d'inactivité
 * prévue et l'état restreint du premier accès. L'interface n'affiche que les tableaux de bord et les
 * actions que ces permissions ouvrent ; la passerelle (OPA) et chaque service refont le contrôle à
 * chaque appel.
 */
@RestController
@RequestMapping("/api")
public class SessionResource {

    /**
     * Vue de la session : jamais de jeton, seulement ce que l'interface doit savoir. « expireA » :
     * fin d'inactivité prévue (ISO-8601), bornée par la durée maximale ; nulle hors session de la
     * plateforme. « restreinte » : premier accès en cours (acr 1).
     */
    public record SessionVue(
        String sujet,
        String nom,
        String identifiant,
        List<String> roles,
        List<String> permissions,
        String institutionId,
        String acr,
        boolean doubleFacteur,
        String expireA,
        boolean restreinte
    ) {}

    private final Set<String> niveauxDoubleFacteur;
    private final ParametresSession parametres;
    private final Clock horloge;

    public SessionResource(Set<String> niveauxDoubleFacteur) {
        this(niveauxDoubleFacteur, new ParametresSession(Duration.ofMinutes(30), Duration.ofHours(10)), Clock.systemUTC());
    }

    public SessionResource(Set<String> niveauxDoubleFacteur, ParametresSession parametres, Clock horloge) {
        this.niveauxDoubleFacteur = Set.copyOf(niveauxDoubleFacteur);
        this.parametres = parametres;
        this.horloge = horloge;
    }

    @Autowired
    public SessionResource(
        @Value("${gimac.securite.double-facteur.niveaux-acr:2,3}") Set<String> niveauxDoubleFacteur,
        @Value("${gimac.session.inactivite-max:PT30M}") Duration inactiviteMax,
        @Value("${gimac.session.duree-max:PT10H}") Duration dureeMax,
        ObjectProvider<Clock> horloge
    ) {
        this(niveauxDoubleFacteur, new ParametresSession(inactiviteMax, dureeMax), horloge.getIfAvailable(Clock::systemUTC));
    }

    @GetMapping("/session")
    public Mono<SessionVue> session(Authentication auth, WebSession session) {
        return Mono.just(vue(auth, session));
    }

    /** Activité explicite (« Rester connecté ») : la fin d'inactivité est repoussée. */
    @PostMapping("/session/prolongation")
    public Mono<ResponseEntity<Void>> prolonger(WebSession session) {
        SessionGimac.prolonger(session, horloge.instant());
        return Mono.just(ResponseEntity.noContent().build());
    }

    SessionVue vue(Authentication auth, WebSession session) {
        Map<String, Object> claims = claims(auth);
        List<String> autorites = auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).sorted().toList();
        String acr = texte(claims.get("acr"));
        return new SessionVue(
            texte(claims.get("sub")),
            texte(claims.getOrDefault("name", claims.get("preferred_username"))),
            texte(claims.get("preferred_username")),
            autorites.stream().filter(a -> a.startsWith("ROLE_")).toList(),
            autorites.stream().filter(a -> !a.startsWith("ROLE_")).toList(),
            texte(claims.get("institution_id")),
            acr,
            niveauxDoubleFacteur.contains(acr),
            session == null ? null : SessionGimac.expireA(session, parametres).map(Object::toString).orElse(null),
            session != null && SessionGimac.restreinte(session)
        );
    }

    static Map<String, Object> claims(Authentication auth) {
        if (auth instanceof JwtAuthenticationToken jwt) {
            return jwt.getToken().getClaims();
        }
        if (auth.getPrincipal() instanceof OidcUser oidc) {
            return oidc.getClaims();
        }
        return Map.of();
    }

    private static String texte(Object o) {
        return o == null ? "" : o.toString();
    }
}
