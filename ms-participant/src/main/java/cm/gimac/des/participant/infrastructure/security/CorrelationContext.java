package cm.gimac.des.participant.infrastructure.security;

import java.util.Optional;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * Contexte de corrélation de la requête courante : identifiant de corrélation propagé
 * (REC-GIMAC-01 §6 point 3) et institution du jeton, pour le cloisonnement (RG-59).
 */
@Component
public class CorrelationContext {

    public static final String MDC_KEY = "correlationId";
    public static final String HEADER = "X-Correlation-Id";
    public static final String INSTITUTION_CLAIM = "institution_id";

    /** Identifiant de corrélation courant ; un nouvel identifiant est créé s'il manque. */
    public static String current() {
        String id = MDC.get(MDC_KEY);
        if (id == null || id.isBlank()) {
            id = UUID.randomUUID().toString();
            MDC.put(MDC_KEY, id);
        }
        return id;
    }

    public String correlationId() {
        return current();
    }

    /** Institution portée par le jeton (claim institution_id), absente pour un collaborateur du DES. */
    public Optional<String> institutionId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();
            String value = jwt.getClaimAsString(INSTITUTION_CLAIM);
            return Optional.ofNullable(value).filter(v -> !v.isBlank());
        }
        return Optional.empty();
    }
}
