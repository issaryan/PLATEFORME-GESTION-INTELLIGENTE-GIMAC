package cm.gimac.des.gateway.security;

import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

/**
 * Certificat client mTLS de la passerelle et son empreinte {@code x5t#S256}.
 *
 * <p>Le jeton interne émis par la passerelle est lié à ce certificat (RFC 8705) :
 * un service n'accepte le jeton que s'il lui est présenté sur une connexion mTLS
 * authentifiée par ce même certificat. Un jeton intercepté est donc inutilisable
 * depuis un autre poste ou un autre service.
 *
 * <p>Lit le même magasin que {@code GatewayMtlsCustomizer} ; aucun mot de passe
 * par défaut (D-44).
 */
@ConditionalOnProperty(name = "gimac.securite.mtls.actif", havingValue = "true")
@Component
public class CertificatMtlsPasserelle {

    private final X509Certificate certificat;
    private final String empreinte;

    public CertificatMtlsPasserelle(
        @Value("${client.ssl.key-store}") Resource magasin,
        @Value("${client.ssl.key-store-password}") String motDePasse
    ) throws Exception {
        KeyStore ks = KeyStore.getInstance("PKCS12");
        try (InputStream flux = magasin.getInputStream()) {
            ks.load(flux, motDePasse.toCharArray());
        }
        X509Certificate trouve = null;
        for (Enumeration<String> alias = ks.aliases(); alias.hasMoreElements(); ) {
            String nom = alias.nextElement();
            if (ks.isKeyEntry(nom) && ks.getCertificate(nom) instanceof X509Certificate x509) {
                trouve = x509;
                break;
            }
        }
        if (trouve == null) {
            throw new IllegalStateException("Aucune clé privée et son certificat dans le magasin mTLS de la passerelle");
        }
        this.certificat = trouve;
        this.empreinte = EmpreinteCertificat.x5tS256(trouve);
    }

    public X509Certificate certificat() {
        return certificat;
    }

    /** Valeur de {@code cnf.x5t#S256} à poser dans les jetons internes. */
    public String empreinte() {
        return empreinte;
    }
}
