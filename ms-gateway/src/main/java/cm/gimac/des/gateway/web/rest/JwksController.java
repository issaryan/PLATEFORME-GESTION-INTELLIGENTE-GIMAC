package cm.gimac.des.gateway.web.rest;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;

import com.nimbusds.jose.jwk.JWKSet;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Expose la clé publique RSA de la Gateway au format JWKS (RFC 7517).
 * Les microservices utilisent cet endpoint pour valider les JWT internes RS256.
 * La clé publique n'est pas un secret : ce endpoint est accessible sans authentification.
 */
// Émetteur du jeton interne, de sa clé et de son JWKS : mode jeton fantôme, ou seul jeton réduit de la sandbox (D-134).
@ConditionalOnExpression("${gimac.securite.jeton-fantome.actif:false} or ${gimac.securite.jeton-interne.actif:false}")
@RestController
@RequestMapping("/api/internal")
public class JwksController {

    private final Map<String, Object> jwks;

    public JwksController(JWKSet internalJwkSet) {
        this.jwks = internalJwkSet.toJSONObject();
    }

    @GetMapping("/jwks")
    public Mono<Map<String, Object>> jwks() {
        return Mono.just(jwks);
    }
}
