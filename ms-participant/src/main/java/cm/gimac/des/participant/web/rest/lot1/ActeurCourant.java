package cm.gimac.des.participant.web.rest.lot1;

import cm.gimac.des.participant.application.port.in.ManageInstitutionUseCase.Acteur;
import cm.gimac.des.participant.domain.ParticipantException;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/** Acteur authentifié, lu dans le jeton : sujet Keycloak (UUID) et nom affiché. */
final class ActeurCourant {

    private ActeurCourant() {}

    static Acteur de(Authentication auth) {
        if (!(auth instanceof JwtAuthenticationToken jwt)) {
            throw ParticipantException.forbidden("PAR_USER_TOKEN_REQUIRED", "Jeton utilisateur exigé");
        }
        Jwt t = jwt.getToken();
        UUID id;
        try {
            id = UUID.fromString(t.getSubject());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw ParticipantException.forbidden("PAR_USER_TOKEN_REQUIRED", "Sujet du jeton invalide");
        }
        String nom = t.getClaimAsString("name");
        if (nom == null || nom.isBlank()) {
            nom = t.getClaimAsString("preferred_username");
        }
        return new Acteur(id, nom);
    }
}
