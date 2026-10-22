package cm.gimac.des.participant.infrastructure.security;

import jakarta.servlet.http.HttpServletRequest;
import java.util.function.Supplier;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.security.authorization.event.AuthorizationDeniedEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * Journal des tentatives refusées des comptes de service de la Sandbox GIMAC (US-EXI-21, D-120, critère 3) : tout
 * refus d'autorisation (route ou méthode) d'un jeton dont le client (azp ou client_id) commence par « sbx- » est écrit
 * au journal d'audit applicatif « gimac.audit.sandbox », collecté par la supervision. Les autres refus ne sont pas
 * journalisés ici. Le refus lui-même (403, sans effet) est rendu par Spring Security.
 */
public class JournalRefusSandbox {

    static final Logger LOG = LoggerFactory.getLogger("gimac.audit.sandbox");
    public static final String EVENEMENT = "participant.sandbox-write-denied";
    static final String PREFIXE = "sbx-";

    @EventListener
    public void refus(AuthorizationDeniedEvent<?> evenement) {
        String client = client(evenement.getAuthentication());
        if (client == null || !client.startsWith(PREFIXE)) {
            return;
        }
        LOG.warn("{} client={} cible={}", EVENEMENT, client, cible(evenement.getSource()));
    }

    /** Client du jeton (azp, sinon client_id), nul pour un jeton sans client ou une authentification non JWT. */
    static String client(Supplier<Authentication> authentification) {
        Authentication a = authentification == null ? null : authentification.get();
        if (!(a instanceof JwtAuthenticationToken jwt)) {
            return null;
        }
        String azp = jwt.getToken().getClaimAsString("azp");
        return azp != null ? azp : jwt.getToken().getClaimAsString("client_id");
    }

    /** Méthode et chemin d'une requête, ou méthode Java protégée. */
    static String cible(Object source) {
        if (source instanceof HttpServletRequest r) {
            return r.getMethod() + " " + r.getRequestURI();
        }
        if (source instanceof MethodInvocation m) {
            return m.getMethod().getDeclaringClass().getSimpleName() + "." + m.getMethod().getName();
        }
        return String.valueOf(source);
    }
}
