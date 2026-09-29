package cm.gimac.des.gateway.web.rest;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * Session de l'utilisateur pour les couches de présentation (D-58, D-61) : la matrice des rôles
 * (permissions) et le niveau d'assurance du jeton obtenu à la connexion. L'interface n'affiche que
 * les tableaux de bord et les actions que ces permissions ouvrent ; la passerelle (OPA) et chaque
 * service refont le contrôle à chaque appel.
 */
@RestController
@RequestMapping("/api")
public class SessionResource {

    /** Vue de la session : jamais de jeton, seulement ce que l'interface doit savoir. */
    public record SessionVue(
        String sujet,
        String nom,
        String identifiant,
        List<String> roles,
        List<String> permissions,
        String institutionId,
        String acr,
        boolean doubleFacteur
    ) {}

    private final Set<String> niveauxDoubleFacteur;

    public SessionResource(@Value("${gimac.securite.double-facteur.niveaux-acr:2,3}") Set<String> niveauxDoubleFacteur) {
        this.niveauxDoubleFacteur = Set.copyOf(niveauxDoubleFacteur);
    }

    @GetMapping("/session")
    public Mono<SessionVue> session(Authentication auth) {
        Map<String, Object> claims = claims(auth);
        List<String> autorites = auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).sorted().toList();
        String acr = texte(claims.get("acr"));
        return Mono.just(
            new SessionVue(
                texte(claims.get("sub")),
                texte(claims.getOrDefault("name", claims.get("preferred_username"))),
                texte(claims.get("preferred_username")),
                autorites.stream().filter(a -> a.startsWith("ROLE_")).toList(),
                autorites.stream().filter(a -> !a.startsWith("ROLE_")).toList(),
                texte(claims.get("institution_id")),
                acr,
                niveauxDoubleFacteur.contains(acr)
            )
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
