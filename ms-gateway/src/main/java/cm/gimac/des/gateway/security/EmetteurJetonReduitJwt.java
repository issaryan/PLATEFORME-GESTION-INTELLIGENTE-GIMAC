package cm.gimac.des.gateway.security;

import cm.gimac.des.gateway.domain.sandbox.JetonReduit;
import cm.gimac.des.gateway.domain.sandbox.port.EmetteurJetonReduit;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jwt.JWTClaimsSet;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Jeton interne réduit signé par l'émetteur unique de la passerelle (RS256, kid, issuer gimac-gateway, cinq minutes,
 * lié au certificat mTLS quand il existe) : sujet, institution_id effective, institution_propre, roles réduits aux
 * permissions de la sandbox, acr et audience du seul service visé.
 */
public class EmetteurJetonReduitJwt implements EmetteurJetonReduit {

    private static final Logger LOG = LoggerFactory.getLogger(EmetteurJetonReduitJwt.class);

    private final EmetteurJwtInterne emetteur;

    public EmetteurJetonReduitJwt(EmetteurJwtInterne emetteur) {
        this.emetteur = emetteur;
    }

    @Override
    public Optional<String> emettre(JetonReduit j) {
        JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder()
            .subject(j.sujet())
            .jwtID(UUID.randomUUID().toString())
            .audience(j.audience())
            .claim("roles", j.permissions())
            .claim("acr", j.acr());
        if (!j.institution().isBlank()) {
            claims.claim("institution_id", j.institution());
        }
        if (!j.institutionPropre().isBlank() && !j.institutionPropre().equals(j.institution())) {
            claims.claim("institution_propre", j.institutionPropre());
        }
        try {
            return Optional.of(emetteur.emettre(claims));
        } catch (JOSEException | RuntimeException e) {
            LOG.error("Jeton interne réduit non émis pour {} : {}", j.audience(), e.getMessage());
            return Optional.empty();
        }
    }
}
