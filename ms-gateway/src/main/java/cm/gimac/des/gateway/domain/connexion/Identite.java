package cm.gimac.des.gateway.domain.connexion;

import java.time.Instant;
import java.util.Map;

/**
 * Jeton d'identité validé (signature par le JWKS du fournisseur, émetteur, audience, échéance) et
 * claims utiles du jeton d'accès (rôles, permissions, institution), lus eux aussi après validation.
 */
public record Identite(String valeur, Instant emisA, Instant expireA, Map<String, Object> claims, Map<String, Object> claimsAcces) {
    public Identite {
        claims = Map.copyOf(claims);
        claimsAcces = Map.copyOf(claimsAcces);
    }

    public String sujet() {
        return texte("sub");
    }

    /** Claim du jeton d'identité, sinon du jeton d'accès ; chaîne vide s'il est absent. */
    public String texte(String nom) {
        Object v = claims.get(nom);
        if (v == null) {
            v = claimsAcces.get(nom);
        }
        return v == null ? "" : v.toString();
    }

    @Override
    public String toString() {
        return "Identite[sub=" + sujet() + "]";
    }
}
