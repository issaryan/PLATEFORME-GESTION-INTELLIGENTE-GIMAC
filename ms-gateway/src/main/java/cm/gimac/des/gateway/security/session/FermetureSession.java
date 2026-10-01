package cm.gimac.des.gateway.security.session;

import cm.gimac.des.gateway.domain.connexion.port.FournisseurIdentite;
import java.time.Clock;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService;
import org.springframework.web.server.WebSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Fermeture d'une session (déconnexion, échéance, nouvelle connexion) : jeton de rafraîchissement
 * révoqué chez Keycloak, client autorisé oublié, session invalidée ou vidée. Le balayage ferme les
 * sessions échues sans attendre une requête de l'utilisateur (contrat D-65 §6).
 */
public class FermetureSession {

    private static final Logger LOG = LoggerFactory.getLogger(FermetureSession.class);

    private final ReactiveOAuth2AuthorizedClientService clientsAutorises;
    private final FournisseurIdentite fournisseur;
    private final RegistreSessions registre;
    private final ParametresSession parametres;
    private final Clock horloge;

    public FermetureSession(
        ReactiveOAuth2AuthorizedClientService clientsAutorises,
        FournisseurIdentite fournisseur,
        RegistreSessions registre,
        ParametresSession parametres,
        Clock horloge
    ) {
        this.clientsAutorises = clientsAutorises;
        this.fournisseur = fournisseur;
        this.registre = registre;
        this.parametres = parametres;
        this.horloge = horloge;
    }

    /** Session close : jetons révoqués puis session invalidée ; « echue » : identifiant retenu comme expiré. */
    public Mono<Void> fermer(WebSession session, boolean echue) {
        registre.retirer(session);
        if (echue) {
            registre.marquerExpiree(session.getId());
        }
        return revoquer(session).then(Mono.defer(session::invalidate));
    }

    /** Session conservée mais vidée (avant une nouvelle connexion) : jetons révoqués, attributs effacés. */
    public Mono<Void> vider(WebSession session) {
        registre.retirer(session);
        return revoquer(session).then(Mono.fromRunnable(() -> session.getAttributes().clear()));
    }

    /** Révocation du jeton de rafraîchissement et oubli du client autorisé ; n'échoue jamais. */
    Mono<Void> revoquer(WebSession session) {
        String enregistrement = session.getAttribute(SessionGimac.ENREGISTREMENT);
        String principal = session.getAttribute(SessionGimac.PRINCIPAL);
        if (enregistrement == null || principal == null) {
            return Mono.empty();
        }
        return clientsAutorises
            .<OAuth2AuthorizedClient>loadAuthorizedClient(enregistrement, principal)
            .flatMap(client ->
                client.getRefreshToken() == null
                    ? Mono.<Void>empty()
                    : fournisseur.revoquer(enregistrement, client.getRefreshToken().getTokenValue())
            )
            .then(Mono.defer(() -> clientsAutorises.removeAuthorizedClient(enregistrement, principal)))
            .onErrorResume(e -> {
                LOG.warn("Fermeture de session : révocation incomplète ({})", e.getMessage());
                return Mono.empty();
            });
    }

    /** Balayage : ferme les sessions échues ou déjà expirées dans le magasin ; nombre de sessions fermées. */
    public Mono<Integer> balayer() {
        var maintenant = horloge.instant();
        AtomicInteger fermees = new AtomicInteger();
        return Flux.fromIterable(registre.sessions())
            .filter(s -> s.isExpired() || !SessionGimac.geree(s) || SessionGimac.expiree(s, maintenant, parametres))
            .concatMap(s -> fermer(s, SessionGimac.geree(s)).doOnSuccess(v -> fermees.incrementAndGet()).onErrorResume(e -> Mono.empty()))
            .then(Mono.fromSupplier(fermees::get));
    }
}
