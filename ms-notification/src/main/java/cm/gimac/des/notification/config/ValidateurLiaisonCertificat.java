package cm.gimac.des.notification.config;

import java.security.MessageDigest;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Map;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

/**
 * Jeton lié au certificat (RFC 8705 §3, RG-25, GW-05) : le jeton interne n'est
 * accepté que si sa revendication {@code cnf.x5t#S256} est l'empreinte du
 * certificat client présenté sur la connexion mTLS qui le porte. Un jeton
 * intercepté ne peut donc pas être rejoué depuis un autre appelant.
 */
final class ValidateurLiaisonCertificat implements OAuth2TokenValidator<Jwt> {

    static final String ATTRIBUT_CERTIFICATS = "jakarta.servlet.request.X509Certificate";

    private static final OAuth2Error REFUS = new OAuth2Error(
        "invalid_token",
        "Jeton non lié au certificat client de la connexion (RFC 8705)",
        null
    );

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jeton) {
        Object cnf = jeton.getClaims().get("cnf");
        if (!(cnf instanceof Map<?, ?> confirmation) || !(confirmation.get("x5t#S256") instanceof String attendue)) {
            return OAuth2TokenValidatorResult.failure(REFUS);
        }
        X509Certificate certificat = certificatClient();
        if (certificat == null) {
            return OAuth2TokenValidatorResult.failure(REFUS);
        }
        return MessageDigest.isEqual(attendue.getBytes(), empreinte(certificat).getBytes())
            ? OAuth2TokenValidatorResult.success()
            : OAuth2TokenValidatorResult.failure(REFUS);
    }

    static String empreinte(X509Certificate certificat) {
        try {
            byte[] condense = MessageDigest.getInstance("SHA-256").digest(certificat.getEncoded());
            return Base64.getUrlEncoder().withoutPadding().encodeToString(condense);
        } catch (Exception e) {
            throw new IllegalStateException("Empreinte x5t#S256 impossible à calculer", e);
        }
    }

    private static X509Certificate certificatClient() {
        RequestAttributes requete = RequestContextHolder.getRequestAttributes();
        if (requete == null) {
            return null;
        }
        Object chaine = requete.getAttribute(ATTRIBUT_CERTIFICATS, RequestAttributes.SCOPE_REQUEST);
        return chaine instanceof X509Certificate[] certificats && certificats.length > 0 ? certificats[0] : null;
    }
}
