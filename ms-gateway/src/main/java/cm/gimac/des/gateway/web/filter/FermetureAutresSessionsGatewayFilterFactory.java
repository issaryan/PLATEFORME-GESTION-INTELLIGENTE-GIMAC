package cm.gimac.des.gateway.web.filter;

import cm.gimac.des.gateway.security.session.SessionGimac;
import cm.gimac.des.gateway.service.appareils.ServiceAppareils;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Filtre « FermetureAutresSessions » de la route « Mon compte » (docs/lot1/mon-compte.md §2) : quand
 * MS-IAM accepte un changement de mot de passe demandé avec « fermerAutresSessions », il le signale
 * par l'en-tête X-Gimac-Fermer-Autres-Sessions ; la passerelle, porteuse des sessions, ferme alors les
 * autres sessions du compte (jetons de rafraîchissement révoqués) avant de répondre. L'en-tête n'est
 * jamais transmis au navigateur, et n'est suivi que sur POST /bff/{couche}/iam/moi/mot-de-passe réussi.
 */
@Component
public class FermetureAutresSessionsGatewayFilterFactory extends AbstractGatewayFilterFactory<Object> {

    public static final String EN_TETE = "X-Gimac-Fermer-Autres-Sessions";
    private static final Logger LOG = LoggerFactory.getLogger(FermetureAutresSessionsGatewayFilterFactory.class);
    private static final Pattern MOT_DE_PASSE = Pattern.compile("^/bff/(des|portail|console)/iam/moi/mot-de-passe$");

    private final ObjectProvider<ServiceAppareils> appareils;

    public FermetureAutresSessionsGatewayFilterFactory(ObjectProvider<ServiceAppareils> appareils) {
        super(Object.class);
        this.appareils = appareils;
    }

    @Override
    public GatewayFilter apply(Object config) {
        return (exchange, chain) -> {
            boolean changement =
                HttpMethod.POST.equals(exchange.getRequest().getMethod()) && MOT_DE_PASSE.matcher(exchange.getRequest().getPath().value()).matches();
            exchange.getResponse().beforeCommit(() -> traiter(exchange, changement));
            return chain.filter(exchange);
        };
    }

    Mono<Void> traiter(ServerWebExchange exchange, boolean changement) {
        HttpHeaders entetes = exchange.getResponse().getHeaders();
        String valeur = entetes.getFirst(EN_TETE);
        if (valeur == null) {
            return Mono.empty();
        }
        entetes.remove(EN_TETE);
        HttpStatusCode statut = exchange.getResponse().getStatusCode();
        ServiceAppareils service = appareils.getIfAvailable();
        if (!changement || !"1".equals(valeur) || statut == null || !statut.is2xxSuccessful() || service == null) {
            return Mono.empty();
        }
        return exchange
            .getSession()
            .flatMap(session -> {
                String sujet = SessionGimac.texte(session, SessionGimac.SUJET);
                return sujet == null ? Mono.<Integer>empty() : service.fermerAutres(sujet, session);
            })
            .doOnNext(n -> LOG.info("Mot de passe changé : {} autre(s) session(s) du compte fermée(s)", n))
            .onErrorResume(e -> {
                LOG.warn("Mot de passe changé : fermeture des autres sessions incomplète ({})", e.getMessage());
                return Mono.empty();
            })
            .then();
    }
}
