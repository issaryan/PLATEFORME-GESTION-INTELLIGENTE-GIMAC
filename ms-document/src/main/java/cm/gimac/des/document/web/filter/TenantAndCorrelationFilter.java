package cm.gimac.des.document.web.filter;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Intercepte les requêtes REST pour injecter le contexte d'isolation (Tenant)
 * et de traçabilité (Correlation ID) dans les logs et le flux d'exécution.
 * Essentiel pour la corrélation inter-services (INF-18).
 */
@ConditionalOnProperty(name = "gimac.document.correlation.actif", havingValue = "true")
@Component
public class TenantAndCorrelationFilter extends OncePerRequestFilter {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    private static final String MDC_CORRELATION_ID = "correlation_id";
    private static final String MDC_TENANT_ID = "tenant_id";
    private static final String MDC_USER = "user";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        try {
            // 1. Gestion du Correlation ID : on le récupère ou on le génère s'il est absent
            String correlationId = request.getHeader(CORRELATION_ID_HEADER);
            if (correlationId == null || correlationId.isBlank()) {
                correlationId = UUID.randomUUID().toString();
            }
            MDC.put(MDC_CORRELATION_ID, correlationId);

            // On renvoie le correlation_id dans la réponse pour que le client (ex:
            // Frontend) puisse l'afficher en cas d'erreur
            response.setHeader(CORRELATION_ID_HEADER, correlationId);

            // 2. Extraction du Tenant ID & Utilisateur depuis le JWT (Keycloak)
            String tenantId = "SYSTEM";
            String user = "ANONYMOUS";

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
                if (jwt.hasClaim("tenant_id")) {
                    tenantId = jwt.getClaimAsString("tenant_id");
                }
                if (jwt.hasClaim("preferred_username")) {
                    user = jwt.getClaimAsString("preferred_username");
                }
            }

            MDC.put(MDC_TENANT_ID, tenantId);
            MDC.put(MDC_USER, user);

            // 3. Poursuite de la chaîne de requêtes
            filterChain.doFilter(request, response);

        } finally {
            // Nettoyage impératif pour éviter les fuites de contexte entre les threads
            // (ThreadLocal)
            MDC.clear();
        }
    }
}