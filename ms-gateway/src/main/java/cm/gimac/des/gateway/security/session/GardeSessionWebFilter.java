package cm.gimac.des.gateway.security.session;

import cm.gimac.des.gateway.domain.connexion.CauseEchec;
import java.time.Clock;
import java.time.Instant;
import org.springframework.core.Ordered;
import org.springframework.http.HttpCookie;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Garde des sessions de la plateforme (contrat D-65 §2 et §6), placée avant la chaîne de sécurité :
 * <ul>
 * <li>session échue (30 min sans activité, 10 h de durée) : jetons révoqués chez Keycloak, session
 * supprimée, 401 CNX_SESSION_EXPIREE ; une nouvelle connexion reste possible sur /api/connexion ;</li>
 * <li>témoin désignant une session déjà fermée pour échéance : 401 CNX_SESSION_EXPIREE ;</li>
 * <li>session restreinte (premier accès) : seuls les chemins de l'assistant sont servis, 403
 * CNX_SESSION_RESTREINTE ailleurs ;</li>
 * <li>toute autre requête prolonge la session, sauf les appels de fond (X-Gimac-Fond: 1, canal
 * poussé /bff/{couche}/live).</li>
 * </ul>
 */
public class GardeSessionWebFilter implements WebFilter, Ordered {

    /** Avant Spring Security (-100) : la session est jugée avant d'y lire l'authentification. */
    public static final int ORDRE = -150;

    private final FermetureSession fermeture;
    private final RegistreSessions registre;
    private final ParametresSession parametres;
    private final Clock horloge;
    private final String nomTemoin;

    public GardeSessionWebFilter(
        FermetureSession fermeture,
        RegistreSessions registre,
        ParametresSession parametres,
        Clock horloge,
        String nomTemoin
    ) {
        this.fermeture = fermeture;
        this.registre = registre;
        this.parametres = parametres;
        this.horloge = horloge;
        this.nomTemoin = nomTemoin;
    }

    @Override
    public int getOrder() {
        return ORDRE;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String chemin = exchange.getRequest().getPath().pathWithinApplication().value();
        if (sondePublique(chemin)) {
            return chain.filter(exchange);
        }
        boolean connexion = "/api/connexion".equals(chemin) || chemin.startsWith("/api/connexion/");
        return exchange
            .getSession()
            .flatMap(session -> {
                Instant maintenant = horloge.instant();
                if (!SessionGimac.geree(session)) {
                    HttpCookie temoin = exchange.getRequest().getCookies().getFirst(nomTemoin);
                    if (!connexion && temoin != null && registre.estExpiree(temoin.getValue())) {
                        return ReponseProbleme.ecrire(exchange, CauseEchec.SESSION_EXPIREE);
                    }
                    return chain.filter(exchange);
                }
                if (SessionGimac.expiree(session, maintenant, parametres)) {
                    if (connexion) {
                        // Nouvelle connexion : l'ancienne session est vidée, l'ouverture changera son identifiant.
                        return fermeture.vider(session).then(chain.filter(exchange));
                    }
                    return fermeture.fermer(session, true).then(ReponseProbleme.ecrire(exchange, CauseEchec.SESSION_EXPIREE));
                }
                if (SessionGimac.restreinte(session) && !SessionGimac.admisEnSessionRestreinte(exchange.getRequest().getMethod(), chemin)) {
                    return ReponseProbleme.ecrire(exchange, CauseEchec.SESSION_RESTREINTE);
                }
                if (!SessionGimac.appelDeFond(chemin, exchange.getRequest().getHeaders().getFirst(SessionGimac.EN_TETE_FOND))) {
                    SessionGimac.prolonger(session, maintenant);
                }
                return chain.filter(exchange);
            });
    }

    /** Sondes publiques de santé : aucune session n'est lue ni créée. */
    static boolean sondePublique(String chemin) {
        return "/management/health".equals(chemin) || chemin.startsWith("/management/health/") || "/management/info".equals(chemin);
    }
}
