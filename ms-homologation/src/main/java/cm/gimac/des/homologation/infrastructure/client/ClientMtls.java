package cm.gimac.des.homologation.infrastructure.client;

import java.util.Optional;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.http.client.ClientHttpRequestFactory;

/**
 * Certificat client du service (RFC 8705, CC-SBX-GIMAC-02 §D.8) : fabrique HTTP présentant le certificat d'un paquet
 * SSL de Spring Boot ({@code spring.ssl.bundle.*}, fourni par le déploiement). Nom de paquet vide : aucun certificat,
 * fabrique par défaut. L'empreinte du certificat n'est jamais posée en en-tête par le client : la passerelle interne de
 * service la transmet après avoir terminé le mTLS.
 */
public final class ClientMtls {

    private ClientMtls() {}

    /** Fabrique présentant le certificat du paquet nommé, ou vide si aucun paquet n'est désigné. */
    public static Optional<ClientHttpRequestFactory> fabrique(SslBundles paquets, String nom) {
        if (nom == null || nom.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(ClientHttpRequestFactoryBuilder.detect().build(HttpClientSettings.ofSslBundle(paquets.getBundle(nom.strip()))));
    }
}
