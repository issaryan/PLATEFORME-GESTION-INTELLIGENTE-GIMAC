package cm.gimac.des.gateway.infrastructure.connexion;

import cm.gimac.des.gateway.domain.connexion.CauseEchec;
import cm.gimac.des.gateway.domain.connexion.ClientConnexion;
import cm.gimac.des.gateway.domain.connexion.EchecConnexion;
import cm.gimac.des.gateway.domain.connexion.Identite;
import cm.gimac.des.gateway.domain.connexion.Jetons;
import cm.gimac.des.gateway.domain.connexion.port.FournisseurIdentite;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Keycloak, appelé de serveur à serveur (contrat D-65 §2) : octroi direct sur le point de jetons de
 * l'enregistrement OAuth2 du client (gimac-connexion ou web_app, secret par variable
 * d'environnement), validation du jeton d'identité par le JWKS du domaine (signature, émetteur,
 * audience, échéance), révocation du jeton de rafraîchissement (RFC 7009).
 */
public class KeycloakFournisseurIdentite implements FournisseurIdentite {

    private static final Logger LOG = LoggerFactory.getLogger(KeycloakFournisseurIdentite.class);
    private static final ParameterizedTypeReference<Map<String, Object>> JSON = new ParameterizedTypeReference<>() {};
    /** Message de Keycloak pour un compte désactivé (octroi direct). */
    static final String COMPTE_DESACTIVE = "Account disabled";

    private final ReactiveClientRegistrationRepository enregistrements;
    private final WebClient client;
    private final Duration delai;
    private final Map<String, ReactiveJwtDecoder> decodeurs = new ConcurrentHashMap<>();

    public KeycloakFournisseurIdentite(ReactiveClientRegistrationRepository enregistrements, WebClient client, Duration delai) {
        this.enregistrements = enregistrements;
        this.client = client;
        this.delai = delai;
    }

    @Override
    public Mono<Jetons> octroyer(ClientConnexion clientConnexion, String identifiant, String motDePasse, String code) {
        return enregistrement(clientConnexion.enregistrement()).flatMap(reg -> {
            MultiValueMap<String, String> formulaire = new LinkedMultiValueMap<>();
            formulaire.add("grant_type", "password");
            formulaire.add("username", identifiant);
            formulaire.add("password", motDePasse);
            formulaire.add("scope", String.join(" ", reg.getScopes().isEmpty() ? java.util.Set.of("openid") : reg.getScopes()));
            if (code != null && !code.isBlank()) {
                formulaire.add("otp", code.trim());
            }
            return client
                .post()
                .uri(reg.getProviderDetails().getTokenUri())
                .headers(h -> h.setBasicAuth(encoder(reg.getClientId()), encoder(reg.getClientSecret())))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .accept(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromFormData(formulaire))
                .exchangeToMono(r -> jetons(r, clientConnexion))
                .timeout(delai)
                .onErrorMap(e -> !(e instanceof EchecConnexion), e -> new EchecConnexion(CauseEchec.SERVICE_INDISPONIBLE, "Keycloak injoignable : " + e.getMessage()));
        });
    }

    private Mono<Jetons> jetons(ClientResponse reponse, ClientConnexion clientConnexion) {
        if (reponse.statusCode().is2xxSuccessful()) {
            return reponse
                .bodyToMono(JSON)
                .map(c ->
                    new Jetons(
                        texte(c.get("access_token")),
                        texte(c.get("refresh_token")),
                        texte(c.get("id_token")),
                        c.get("expires_in") instanceof Number n ? n.longValue() : 0L,
                        texte(c.get("scope"))
                    )
                );
        }
        int statut = reponse.statusCode().value();
        return reponse.bodyToMono(JSON).defaultIfEmpty(Map.of()).onErrorReturn(Map.of()).flatMap(c -> Mono.error(echec(statut, c, clientConnexion)));
    }

    /**
     * Octroi à deux facteurs : Keycloak répond 500 (et non 401) quand le compte n'a plus de code TOTP
     * (second facteur réinitialisé entre la lecture de l'état et l'octroi). C'est un refus pour
     * l'utilisateur, qui reprend la connexion et passe par le premier accès.
     */
    static EchecConnexion echec(int statut, Map<String, Object> corps, ClientConnexion clientConnexion) {
        if (clientConnexion == ClientConnexion.DEUX_FACTEURS && statut == 500) {
            return new EchecConnexion(CauseEchec.IDENTIFIANTS_INVALIDES, "Keycloak 500 à l'octroi à deux facteurs : aucun code TOTP configuré");
        }
        return echec(statut, corps);
    }

    /** Traduction d'un refus de Keycloak : jamais la cause précise au navigateur, seulement au journal. */
    static EchecConnexion echec(int statut, Map<String, Object> corps) {
        String erreur = String.valueOf(corps.get("error"));
        String description = String.valueOf(corps.get("error_description"));
        if ("invalid_grant".equals(erreur)) {
            if (COMPTE_DESACTIVE.equalsIgnoreCase(description)) {
                return new EchecConnexion(CauseEchec.COMPTE_INACTIF, "Keycloak : " + description);
            }
            return new EchecConnexion(CauseEchec.IDENTIFIANTS_INVALIDES, "Keycloak : " + description);
        }
        // invalid_client, unauthorized_client, erreur serveur : configuration ou panne, pas l'utilisateur.
        return new EchecConnexion(CauseEchec.SERVICE_INDISPONIBLE, "Keycloak " + statut + " : " + erreur);
    }

    @Override
    public Mono<Identite> valider(ClientConnexion clientConnexion, Jetons jetons) {
        if (jetons.jetonIdentite() == null || jetons.jetonAcces() == null) {
            return Mono.error(new EchecConnexion(CauseEchec.SERVICE_INDISPONIBLE, "réponse de Keycloak sans jeton d'identité ou d'accès"));
        }
        return enregistrement(clientConnexion.enregistrement()).flatMap(reg -> {
            ReactiveJwtDecoder decodeur = decodeur(reg);
            return decodeur
                .decode(jetons.jetonIdentite())
                .zipWith(decodeur.decode(jetons.jetonAcces()))
                .flatMap(paire -> {
                    var identite = paire.getT1();
                    var acces = paire.getT2();
                    if (identite.getAudience() == null || !identite.getAudience().contains(reg.getClientId())) {
                        return Mono.error(new EchecConnexion(CauseEchec.SERVICE_INDISPONIBLE, "jeton d'identité destiné à un autre client"));
                    }
                    if (identite.getSubject() == null || !identite.getSubject().equals(acces.getSubject())) {
                        return Mono.error(new EchecConnexion(CauseEchec.SERVICE_INDISPONIBLE, "jetons d'identité et d'accès de sujets différents"));
                    }
                    return Mono.just(new Identite(jetons.jetonIdentite(), identite.getIssuedAt(), identite.getExpiresAt(), identite.getClaims(), acces.getClaims()));
                })
                .onErrorMap(e -> !(e instanceof EchecConnexion), e -> new EchecConnexion(CauseEchec.SERVICE_INDISPONIBLE, "jeton non valide : " + e.getMessage()));
        });
    }

    /** Décodeur par enregistrement : clés du JWKS mises en cache, émetteur et échéance vérifiés. */
    ReactiveJwtDecoder decodeur(ClientRegistration reg) {
        return decodeurs.computeIfAbsent(reg.getRegistrationId(), id -> {
            NimbusReactiveJwtDecoder d = NimbusReactiveJwtDecoder.withJwkSetUri(reg.getProviderDetails().getJwkSetUri()).webClient(client).build();
            String emetteur = reg.getProviderDetails().getIssuerUri();
            d.setJwtValidator(emetteur == null ? JwtValidators.createDefault() : JwtValidators.createDefaultWithIssuer(emetteur));
            return d;
        });
    }

    @Override
    public Mono<Void> revoquer(String nomEnregistrement, String jetonRafraichissement) {
        if (jetonRafraichissement == null || jetonRafraichissement.isBlank()) {
            return Mono.empty();
        }
        return enregistrement(nomEnregistrement)
            .flatMap(reg -> {
                MultiValueMap<String, String> formulaire = new LinkedMultiValueMap<>();
                formulaire.add("token", jetonRafraichissement);
                formulaire.add("token_type_hint", "refresh_token");
                return client
                    .post()
                    .uri(pointRevocation(reg))
                    .headers(h -> h.setBasicAuth(encoder(reg.getClientId()), encoder(reg.getClientSecret())))
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters.fromFormData(formulaire))
                    .retrieve()
                    .toBodilessEntity()
                    .timeout(delai)
                    .then();
            })
            .onErrorResume(e -> {
                LOG.warn("Révocation du jeton de rafraîchissement ({}) impossible : {}", nomEnregistrement, e.getMessage());
                return Mono.empty();
            });
    }

    /** Point de révocation annoncé par la découverte OIDC, sinon celui de Keycloak sous l'émetteur. */
    static String pointRevocation(ClientRegistration reg) {
        Object annonce = reg.getProviderDetails().getConfigurationMetadata().get("revocation_endpoint");
        if (annonce != null) {
            return annonce.toString();
        }
        return reg.getProviderDetails().getIssuerUri() + "/protocol/openid-connect/revoke";
    }

    private Mono<ClientRegistration> enregistrement(String nom) {
        return enregistrements
            .findByRegistrationId(nom)
            .switchIfEmpty(Mono.error(() -> new EchecConnexion(CauseEchec.SERVICE_INDISPONIBLE, "enregistrement OAuth2 absent : " + nom)));
    }

    /** Identifiant et secret encodés (RFC 6749 §2.3.1) avant l'authentification HTTP Basic. */
    static String encoder(String valeur) {
        return URLEncoder.encode(valeur == null ? "" : valeur, StandardCharsets.UTF_8);
    }

    private static String texte(Object o) {
        return o == null ? null : o.toString();
    }
}
