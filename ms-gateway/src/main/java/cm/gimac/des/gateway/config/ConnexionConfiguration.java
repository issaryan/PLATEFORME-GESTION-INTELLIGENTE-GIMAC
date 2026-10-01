package cm.gimac.des.gateway.config;

import cm.gimac.des.gateway.audit.GatewayAuditEventPublisher;
import cm.gimac.des.gateway.domain.connexion.port.AnnuaireComptes;
import cm.gimac.des.gateway.domain.connexion.port.FournisseurIdentite;
import cm.gimac.des.gateway.domain.connexion.port.JournalConnexion;
import cm.gimac.des.gateway.domain.connexion.port.LimiteurTentatives;
import cm.gimac.des.gateway.infrastructure.connexion.IamAnnuaireComptes;
import cm.gimac.des.gateway.infrastructure.connexion.JetonServicePasserelle;
import cm.gimac.des.gateway.infrastructure.connexion.JournalConnexionAudit;
import cm.gimac.des.gateway.infrastructure.connexion.KeycloakFournisseurIdentite;
import cm.gimac.des.gateway.infrastructure.connexion.LimiteurTentativesMemoire;
import cm.gimac.des.gateway.infrastructure.connexion.LimiteurTentativesValkey;
import cm.gimac.des.gateway.ratelimit.TokenBucketRateLimiter;
import cm.gimac.des.gateway.security.session.BalayeurSessions;
import cm.gimac.des.gateway.security.session.FermetureSession;
import cm.gimac.des.gateway.security.session.GardeSessionWebFilter;
import cm.gimac.des.gateway.security.session.OuvertureSession;
import cm.gimac.des.gateway.security.session.ParametresSession;
import cm.gimac.des.gateway.security.session.RegistreSessions;
import cm.gimac.des.gateway.service.connexion.ServiceConnexion;
import java.time.Clock;
import java.time.Duration;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.AuthorizationCodeReactiveOAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.ClientCredentialsReactiveOAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.DelegatingReactiveOAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.RefreshTokenReactiveOAuth2AuthorizedClientProvider;
import reactor.core.publisher.Mono;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.web.DefaultReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizedClientRepository;
import org.springframework.security.web.server.context.WebSessionServerSecurityContextRepository;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Assemblage de la connexion sur la plateforme (D-65) et de la fermeture des sessions (§6) :
 * adaptateurs Keycloak et MS-IAM, limitation, journal, ouverture et fermeture des sessions,
 * garde des sessions (filtre placé avant Spring Security) et balayage périodique.
 */
@Configuration
@EnableConfigurationProperties({ ConnexionProperties.class, SessionProperties.class })
public class ConnexionConfiguration {

    static Clock horloge(ObjectProvider<Clock> horloge) {
        return horloge.getIfAvailable(Clock::systemUTC);
    }

    @Bean
    public ParametresSession parametresSession(SessionProperties p) {
        return new ParametresSession(p.getInactiviteMax(), p.getDureeMax());
    }

    @Bean
    public RegistreSessions registreSessions(SessionProperties p) {
        return new RegistreSessions(p.getDureeMax());
    }

    @Bean
    public FournisseurIdentite fournisseurIdentite(ReactiveClientRegistrationRepository enregistrements, ConnexionProperties p) {
        return new KeycloakFournisseurIdentite(enregistrements, WebClient.create(), p.getDelai());
    }

    @Bean
    public AnnuaireComptes annuaireComptes(
        ConnexionProperties p,
        ObjectProvider<ReactiveClientRegistrationRepository> enregistrements,
        ObjectProvider<ReactiveOAuth2AuthorizedClientService> clientsAutorises
    ) {
        JetonServicePasserelle jeton = new JetonServicePasserelle(SupervisionConfiguration.gestionnaire(enregistrements, clientsAutorises));
        return new IamAnnuaireComptes(WebClient.builder().baseUrl(p.getIamUrl()).build(), jeton, p.getDelai());
    }

    /** Valkey (partagé entre instances) si la limitation de débit de la passerelle est active, sinon mémoire. */
    @Bean
    public LimiteurTentatives limiteurTentatives(ConnexionProperties p, ObjectProvider<TokenBucketRateLimiter> valkey, ObjectProvider<Clock> horloge) {
        int n = p.getTentativesParMinute();
        LimiteurTentatives memoire = new LimiteurTentativesMemoire(n, Duration.ofMinutes(1), horloge(horloge));
        TokenBucketRateLimiter limiteur = valkey.getIfAvailable();
        return limiteur == null ? memoire : new LimiteurTentativesValkey(limiteur, memoire, n, n);
    }

    @Bean
    public JournalConnexion journalConnexion(ObjectProvider<GatewayAuditEventPublisher> publication) {
        return new JournalConnexionAudit(publication.getIfAvailable());
    }

    @Bean
    public FermetureSession fermetureSession(
        ReactiveOAuth2AuthorizedClientService clientsAutorises,
        FournisseurIdentite fournisseur,
        RegistreSessions registre,
        ParametresSession parametres,
        ObjectProvider<Clock> horloge
    ) {
        return new FermetureSession(clientsAutorises, fournisseur, registre, parametres, horloge(horloge));
    }

    @Bean
    public OuvertureSession ouvertureSession(
        ReactiveClientRegistrationRepository enregistrements,
        ServerOAuth2AuthorizedClientRepository clientsAutorises,
        FermetureSession fermeture,
        RegistreSessions registre,
        ParametresSession parametres,
        ObjectProvider<Clock> horloge
    ) {
        return new OuvertureSession(
            enregistrements,
            clientsAutorises,
            new WebSessionServerSecurityContextRepository(),
            fermeture,
            registre,
            parametres,
            horloge(horloge)
        );
    }

    @Bean
    public ServiceConnexion serviceConnexion(
        FournisseurIdentite fournisseur,
        AnnuaireComptes annuaire,
        LimiteurTentatives limiteur,
        JournalConnexion journal,
        OuvertureSession ouverture
    ) {
        return new ServiceConnexion(fournisseur, annuaire, limiteur, journal, ouverture);
    }

    @Bean
    public GardeSessionWebFilter gardeSessionWebFilter(
        FermetureSession fermeture,
        RegistreSessions registre,
        ParametresSession parametres,
        SessionProperties p,
        ObjectProvider<Clock> horloge
    ) {
        return new GardeSessionWebFilter(fermeture, registre, parametres, horloge(horloge), p.getTemoin());
    }

    /**
     * Gestionnaire des clients autorisés (TokenRelay, rafraîchissement) : code d'autorisation,
     * rafraîchissement et client_credentials, résultat enregistré dans le dépôt des clients autorisés.
     * Remplace celui que Spring Security 7 installe par défaut, qui recharge l'utilisateur OIDC (appel
     * userinfo) à chaque rafraîchissement et remplace le principal : le principal de la session
     * (identifiant propre à la session, rôles lus à la connexion) reste celui de la connexion ; un
     * changement de droits s'applique à la connexion suivante.
     */
    @Bean
    public ReactiveOAuth2AuthorizedClientManager gatewayAuthorizedClientManager(
        ReactiveClientRegistrationRepository enregistrements,
        ServerOAuth2AuthorizedClientRepository clientsAutorises
    ) {
        var gestionnaire = new DefaultReactiveOAuth2AuthorizedClientManager(enregistrements, clientsAutorises);
        gestionnaire.setAuthorizedClientProvider(fournisseurClientsAutorises());
        return gestionnaire;
    }

    /** Rafraîchissement sans rechargement de l'utilisateur OIDC (gestionnaire de succès neutre). */
    static ReactiveOAuth2AuthorizedClientProvider fournisseurClientsAutorises() {
        RefreshTokenReactiveOAuth2AuthorizedClientProvider rafraichissement = new RefreshTokenReactiveOAuth2AuthorizedClientProvider();
        rafraichissement.setAuthorizationSuccessHandler((client, principal, attributs) -> Mono.empty());
        return new DelegatingReactiveOAuth2AuthorizedClientProvider(
            new AuthorizationCodeReactiveOAuth2AuthorizedClientProvider(),
            rafraichissement,
            new ClientCredentialsReactiveOAuth2AuthorizedClientProvider()
        );
    }

    @Bean(destroyMethod = "close")
    public BalayeurSessions balayeurSessions(FermetureSession fermeture, SessionProperties p) {
        return new BalayeurSessions(fermeture, p.getBalayage());
    }
}
