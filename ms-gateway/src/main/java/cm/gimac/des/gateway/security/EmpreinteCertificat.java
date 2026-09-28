package cm.gimac.des.gateway.security;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Base64;

/**
 * Empreinte {@code x5t#S256} d'un certificat X.509 (RFC 8705 §3.1) : SHA-256 de
 * l'encodage DER, en base64url sans remplissage. C'est la valeur portée par la
 * revendication {@code cnf} du jeton interne et recalculée par chaque service
 * sur le certificat client présenté en mTLS (RG-25, GW-05).
 */
public final class EmpreinteCertificat {

    private EmpreinteCertificat() {}

    public static String x5tS256(X509Certificate certificat) {
        try {
            byte[] condense = MessageDigest.getInstance("SHA-256").digest(certificat.getEncoded());
            return Base64.getUrlEncoder().withoutPadding().encodeToString(condense);
        } catch (NoSuchAlgorithmException | CertificateEncodingException e) {
            throw new IllegalStateException("Empreinte x5t#S256 impossible à calculer", e);
        }
    }
}
