package cm.gimac.des.homologation.web.rest.lot1;

import cm.gimac.des.homologation.domain.parcours.Acteur;
import cm.gimac.des.homologation.infrastructure.security.CorrelationContext;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/** Acteur de la requête, lu dans le jeton : sujet, nom affiché, matrice des rôles et institution du portail. */
final class ActeurCourant {

    private ActeurCourant() {}

    static Acteur de(Authentication auth) {
        Set<String> permissions = auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet());
        UUID id = null;
        String nom = null;
        String institution = null;
        if (auth instanceof JwtAuthenticationToken jwt) {
            Jwt t = jwt.getToken();
            id = uuid(t.getSubject());
            nom = t.getClaimAsString("name") != null ? t.getClaimAsString("name") : t.getClaimAsString("preferred_username");
            institution = t.getClaimAsString(CorrelationContext.INSTITUTION_CLAIM);
        }
        return new Acteur(id, nom, permissions, institution);
    }

    private static UUID uuid(String s) {
        try {
            return s == null ? null : UUID.fromString(s);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
