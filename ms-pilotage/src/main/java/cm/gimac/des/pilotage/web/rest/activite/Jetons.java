package cm.gimac.des.pilotage.web.rest.activite;

import cm.gimac.des.pilotage.application.port.in.Appelant;
import cm.gimac.des.pilotage.domain.exception.PilotageException;
import cm.gimac.des.pilotage.infrastructure.security.ServiceTokenAuthorizationManager;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/** Lecture du jeton : l'appelant est le sujet ({@code sub}) ; ses permissions sont celles du jeton. */
final class Jetons {

    /** Permissions de Mon activité (docs/lot1/mon-activite.md §4), contrôlées route par route. */
    static final String ACTIVITE = "hasAuthority('PIL_ACTIVITY_OWN')";
    static final String EQUIPE = "hasAuthority('PIL_TEAM_READ')";
    static final String CREATION_INTERNE = "hasAuthority('INTERNAL_PILOTAGE_TASK_CREATE')";

    private Jetons() {}

    static Appelant appelant(Authentication auth) {
        if (auth instanceof JwtAuthenticationToken jwt && jwt.getToken().getSubject() != null && !jwt.getToken().getSubject().isBlank()) {
            String nom = jwt.getToken().getClaimAsString("name");
            if (nom == null || nom.isBlank()) {
                nom = jwt.getToken().getClaimAsString("preferred_username");
            }
            Set<String> permissions = auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toUnmodifiableSet());
            return new Appelant(jwt.getToken().getSubject(), nom, permissions);
        }
        throw PilotageException.interdit("PIL_JETON_UTILISATEUR_EXIGE", "Jeton utilisateur exigé");
    }

    /** Client de service appelant (azp du jeton client_credentials). */
    static String client(Authentication auth) {
        if (auth instanceof JwtAuthenticationToken jwt) {
            return ServiceTokenAuthorizationManager.clientDeService(jwt.getToken()).orElseThrow(() ->
                PilotageException.interdit("PIL_JETON_SERVICE_EXIGE", "Jeton de service exigé")
            );
        }
        throw PilotageException.interdit("PIL_JETON_SERVICE_EXIGE", "Jeton de service exigé");
    }
}
