package cm.gimac.des.gateway.security.oauth2;

import com.github.benmanes.caffeine.cache.Cache;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.jwt.Jwt;
import reactor.core.publisher.Mono;

/**
 * Complète un jeton porteur déjà validé (signature, émetteur, audience) avec les informations
 * d'identité du point userinfo du fournisseur d'identité, quand le jeton ne les porte pas.
 *
 * <p>L'enrichissement n'est jamais une condition de l'authentification : les autorités sont lues
 * dans les claims du jeton (claim {@code roles}), comme sur les autres services.
 * <ul>
 *   <li>Jeton de compte de service (octroi client_credentials) : userinfo n'est pas appelé
 *   (Keycloak le refuse, 403, à un jeton sans portée openid) ; le jeton est retenu tel quel.</li>
 *   <li>Jeton d'utilisateur : en cas de refus, d'indisponibilité ou de dépassement du délai de
 *   userinfo, repli sur les claims du jeton, avec un avertissement journalisé ; jamais de 500.</li>
 * </ul>
 */
public class EnrichissementJeton {

    private static final Logger LOG = LoggerFactory.getLogger(EnrichissementJeton.class);

    /** Préfixe des comptes de service de Keycloak (preferred_username = service-account-{client}). */
    public static final String PREFIXE_COMPTE_SERVICE = "service-account-";

    /** Délai maximal de l'appel à userinfo : au-delà, repli sur les claims du jeton. */
    public static final Duration DELAI_USERINFO = Duration.ofSeconds(5);

    private final Function<String, Mono<Map<String, Object>>> userinfo;
    private final Cache<String, Mono<Jwt>> cache;
    private final Duration delai;

    /**
     * @param userinfo lecture du point userinfo avec le jeton porteur donné
     * @param cache    jetons enrichis, par sujet
     * @param delai    délai maximal de la lecture
     */
    public EnrichissementJeton(Function<String, Mono<Map<String, Object>>> userinfo, Cache<String, Mono<Jwt>> cache, Duration delai) {
        this.userinfo = userinfo;
        this.cache = cache;
        this.delai = delai;
    }

    /**
     * Jeton de compte de service : claim {@code client_id} (posé par Keycloak sur l'octroi
     * client_credentials), identifiant {@code service-account-…}, ou sujet absent.
     */
    public static boolean compteDeService(Jwt jwt) {
        String identifiant = jwt.getClaimAsString("preferred_username");
        return (
            (identifiant != null && identifiant.startsWith(PREFIXE_COMPTE_SERVICE)) ||
            jwt.hasClaim("client_id") ||
            jwt.getSubject() == null ||
            jwt.getSubject().isBlank()
        );
    }

    public Mono<Jwt> enrichir(Jwt jwt) {
        // Identité déjà portée par le jeton, ou compte de service : rien à demander.
        if ((jwt.hasClaim("given_name") && jwt.hasClaim("family_name")) || compteDeService(jwt)) {
            return Mono.just(jwt);
        }
        Mono<Jwt> enCache = cache.getIfPresent(jwt.getSubject());
        if (enCache != null) {
            return enCache;
        }
        return Mono.defer(() -> userinfo.apply(jwt.getTokenValue()))
            .timeout(delai)
            .map(informations -> fusionner(jwt, informations))
            .doOnNext(enrichi -> cache.put(jwt.getSubject(), Mono.just(enrichi)))
            .switchIfEmpty(Mono.fromSupplier(() -> repli(jwt, "réponse vide")))
            .onErrorResume(e -> Mono.just(repli(jwt, e.getClass().getSimpleName() + " : " + e.getMessage())));
    }

    private static Jwt repli(Jwt jwt, String cause) {
        LOG.warn("Lecture de userinfo impossible pour le sujet {} ({}) : repli sur les claims du jeton", jwt.getSubject(), cause);
        return jwt;
    }

    static Jwt fusionner(Jwt jwt, Map<String, Object> reponse) {
        Map<String, Object> informations = new HashMap<>(reponse);
        Object nom = informations.get("name");
        if (nom != null) {
            String[] parties = nom.toString().trim().split("\\s+");
            if (parties.length > 0 && !parties[0].isEmpty()) {
                informations.putIfAbsent("given_name", parties[0]);
                informations.putIfAbsent("family_name", String.join(" ", Arrays.copyOfRange(parties, 1, parties.length)));
            }
        }
        // Les claims du jeton, signés, priment sur la réponse de userinfo.
        return Jwt.withTokenValue(jwt.getTokenValue())
            .headers(entetes -> entetes.putAll(jwt.getHeaders()))
            .claims(claims -> {
                informations.forEach((cle, valeur) -> {
                    if (cle != null && valeur != null) {
                        claims.put(cle, valeur);
                    }
                });
                claims.putAll(jwt.getClaims());
            })
            .build();
    }
}
