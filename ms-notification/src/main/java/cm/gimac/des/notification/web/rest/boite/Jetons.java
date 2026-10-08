package cm.gimac.des.notification.web.rest.boite;

import cm.gimac.des.notification.domain.exception.NotificationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/** Lecture du jeton : le compte appelant est le sujet ({@code sub}), identifiant IAM. */
final class Jetons {

    /** Permission de la boîte de notifications (§5) : tous les profils internes. */
    static final String BOITE = "hasAuthority('NOT_INBOX_READ')";

    private Jetons() {}

    /** Claim de l'institution d'un compte de participant (portail) ; absent pour un membre du GIMAC. */
    static final String INSTITUTION = "institution_id";

    /**
     * Le lecteur est un compte d'institution (claim {@code institution_id} non vide) : ses réponses ne portent
     * aucune donnée interne du GIMAC (PI-GIMAC-01, projections du portail).
     */
    static boolean compteInstitution(Authentication auth) {
        if (auth instanceof JwtAuthenticationToken jwt) {
            String institution = jwt.getToken().getClaimAsString(INSTITUTION);
            return institution != null && !institution.isBlank();
        }
        return false;
    }

    static String compte(Authentication auth) {
        if (auth instanceof JwtAuthenticationToken jwt && jwt.getToken().getSubject() != null && !jwt.getToken().getSubject().isBlank()) {
            return jwt.getToken().getSubject();
        }
        throw NotificationException.interdit("NOT_JETON_UTILISATEUR_EXIGE", "Jeton utilisateur exigé");
    }
}
