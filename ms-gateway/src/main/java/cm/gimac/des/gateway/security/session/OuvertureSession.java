package cm.gimac.des.gateway.security.session;

import cm.gimac.des.gateway.domain.connexion.ClientConnexion;
import cm.gimac.des.gateway.domain.connexion.EchecConnexion;
import cm.gimac.des.gateway.domain.connexion.CauseEchec;
import cm.gimac.des.gateway.domain.connexion.Identite;
import cm.gimac.des.gateway.domain.connexion.Jetons;
import cm.gimac.des.gateway.security.SecurityUtils;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.web.server.context.ServerSecurityContextRepository;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebSession;
import reactor.core.publisher.Mono;

/**
 * Ouverture de la session après l'octroi direct (contrat D-65 §2, point 7), comme le ferait une
 * connexion OIDC : nouvel identifiant de session (contre la fixation), authentification
 * OAuth2AuthenticationToken dont l'utilisateur est construit sur le jeton d'identité validé,
 * client autorisé (jetons d'accès et de rafraîchissement) enregistré dans le dépôt des clients
 * autorisés, contexte de sécurité dans la session. Le relais TokenRelay, le rafraîchissement,
 * /api/session et /api/logout fonctionnent donc à l'identique.
 */
public class OuvertureSession {

    /**
     * Claim ajouté à l'utilisateur : identifiant propre à la session, nom du principal sous lequel
     * le client autorisé est enregistré. Deux sessions d'un même compte ne partagent ainsi jamais
     * leurs jetons, et la fermeture de l'une ne révoque pas l'autre.
     */
    public static final String LIAISON = "gimac_session";

    /** Claims techniques du jeton d'accès, non repris dans les informations de l'utilisateur. */
    private static final Set<String> CLAIMS_TECHNIQUES = Set.of("exp", "iat", "nbf", "jti", "typ", "aud", "iss", "azp", "auth_time");

    /** Marge du magasin de sessions : la passerelle ferme la session elle-même, avant lui. */
    private static final Duration MARGE_MAGASIN = Duration.ofMinutes(5);

    private final ReactiveClientRegistrationRepository enregistrements;
    private final ServerOAuth2AuthorizedClientRepository clientsAutorises;
    private final ServerSecurityContextRepository contextes;
    private final FermetureSession fermeture;
    private final RegistreSessions registre;
    private final ParametresSession parametres;
    private final Clock horloge;

    public OuvertureSession(
        ReactiveClientRegistrationRepository enregistrements,
        ServerOAuth2AuthorizedClientRepository clientsAutorises,
        ServerSecurityContextRepository contextes,
        FermetureSession fermeture,
        RegistreSessions registre,
        ParametresSession parametres,
        Clock horloge
    ) {
        this.enregistrements = enregistrements;
        this.clientsAutorises = clientsAutorises;
        this.contextes = contextes;
        this.fermeture = fermeture;
        this.registre = registre;
        this.parametres = parametres;
        this.horloge = horloge;
    }

    public Mono<OAuth2AuthenticationToken> ouvrir(
        ServerWebExchange exchange,
        ClientConnexion client,
        Jetons jetons,
        Identite identite,
        boolean restreinte
    ) {
        return enregistrements
            .findByRegistrationId(client.enregistrement())
            .switchIfEmpty(Mono.error(() -> new EchecConnexion(CauseEchec.SERVICE_INDISPONIBLE, "enregistrement absent : " + client.enregistrement())))
            .flatMap(enregistrement ->
                exchange
                    .getSession()
                    .flatMap(session -> {
                        String ancienIdentifiant = session.getId();
                        return fermeture
                            .vider(session)
                            .then(Mono.defer(session::changeSessionId))
                            .then(
                                Mono.defer(() -> {
                                    registre.oublierExpiree(ancienIdentifiant);
                                    Instant maintenant = horloge.instant();
                                    OAuth2AuthenticationToken auth = authentification(identite, client);
                                    OAuth2AuthorizedClient autorise = new OAuth2AuthorizedClient(
                                        enregistrement,
                                        auth.getName(),
                                        jetonAcces(jetons, maintenant),
                                        jetons.jetonRafraichissement() == null ? null : new OAuth2RefreshToken(jetons.jetonRafraichissement(), maintenant)
                                    );
                                    marquer(session, maintenant, restreinte, client, auth.getName());
                                    return clientsAutorises
                                        .saveAuthorizedClient(autorise, auth, exchange)
                                        .then(contextes.save(exchange, new SecurityContextImpl(auth)))
                                        .then(Mono.fromRunnable(() -> registre.enregistrer(session)))
                                        .thenReturn(auth);
                                })
                            );
                    })
            );
    }

    private void marquer(WebSession session, Instant maintenant, boolean restreinte, ClientConnexion client, String principal) {
        Map<String, Object> a = session.getAttributes();
        a.put(SessionGimac.DEBUT, maintenant);
        a.put(SessionGimac.DERNIERE_ACTIVITE, maintenant);
        a.put(SessionGimac.RESTREINTE, restreinte);
        a.put(SessionGimac.ENREGISTREMENT, client.enregistrement());
        a.put(SessionGimac.PRINCIPAL, principal);
        session.setMaxIdleTime(parametres.dureeMax().plus(MARGE_MAGASIN));
    }

    /** Utilisateur OIDC : jeton d'identité validé ; rôles et permissions des deux jetons. */
    static OAuth2AuthenticationToken authentification(Identite identite, ClientConnexion client) {
        Set<GrantedAuthority> autorites = new LinkedHashSet<>(SecurityUtils.extractAuthorityFromClaims(identite.claims()));
        autorites.addAll(SecurityUtils.extractAuthorityFromClaims(identite.claimsAcces()));
        OidcIdToken jetonIdentite = new OidcIdToken(identite.valeur(), identite.emisA(), identite.expireA(), identite.claims());
        Map<String, Object> informations = new HashMap<>();
        identite.claimsAcces().forEach((nom, valeur) -> {
            if (!CLAIMS_TECHNIQUES.contains(nom) && valeur != null) {
                informations.put(nom, valeur);
            }
        });
        informations.put("sub", identite.sujet());
        informations.put(LIAISON, UUID.randomUUID().toString());
        DefaultOidcUser utilisateur = new DefaultOidcUser(autorites, jetonIdentite, new OidcUserInfo(informations), LIAISON);
        return new OAuth2AuthenticationToken(utilisateur, autorites, client.enregistrement());
    }

    static OAuth2AccessToken jetonAcces(Jetons jetons, Instant maintenant) {
        Set<String> portees = jetons.portee() == null || jetons.portee().isBlank()
            ? Set.of()
            : Arrays.stream(jetons.portee().trim().split("\\s+")).collect(Collectors.toSet());
        long duree = jetons.dureeAcces() > 0 ? jetons.dureeAcces() : 60;
        return new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, jetons.jetonAcces(), maintenant, maintenant.plusSeconds(duree), portees);
    }
}
