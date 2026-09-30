package cm.gimac.des.certification.web.rest.lot1;

import cm.gimac.des.certification.application.port.in.Acteur;
import cm.gimac.des.certification.domain.CertificationException;
import cm.gimac.des.certification.infrastructure.security.CorrelationContext;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/** Acteur de la requête, lu dans le jeton : sujet, nom affiché, permissions et institution (portail). */
final class ActeurCourant {

    private ActeurCourant() {}

    static Acteur de(Authentication auth) {
        if (!(auth instanceof JwtAuthenticationToken jeton) || jeton.getToken().getSubject() == null) {
            throw CertificationException.interdit("CER_SUBJECT_REQUIRED", "Jeton utilisateur exigé");
        }
        Jwt jwt = jeton.getToken();
        Set<String> permissions = auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet());
        return new Acteur(jwt.getSubject(), nom(jwt), permissions, jwt.getClaimAsString(CorrelationContext.INSTITUTION_CLAIM));
    }

    private static String nom(Jwt jwt) {
        for (String claim : new String[] { "name", "preferred_username" }) {
            String v = jwt.getClaimAsString(claim);
            if (v != null && !v.isBlank()) {
                return v;
            }
        }
        return jwt.getSubject();
    }
}
