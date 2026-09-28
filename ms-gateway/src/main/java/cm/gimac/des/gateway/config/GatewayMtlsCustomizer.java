package cm.gimac.des.gateway.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import io.micrometer.core.instrument.MeterRegistry;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.config.HttpClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.lang.NonNull;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Applique strictement le contexte mTLS (Mutual TLS) au client HTTP réactif
 * sous-jacent
 * utilisé par Spring Cloud Gateway.
 * Inclut la télémétrie d'expiration des certificats pour prévenir les
 * interruptions de service (Zero Trust).
 */
@ConditionalOnProperty(name = "gimac.securite.mtls.actif", havingValue = "true")
@Configuration
public class GatewayMtlsCustomizer {

    private static final Logger log = LoggerFactory.getLogger(GatewayMtlsCustomizer.class);

    @NonNull
    private final Resource keyStoreResource;
    @NonNull
    private final String keyStorePassword;
    @NonNull
    private final Resource trustStoreResource;
    @NonNull
    private final String trustStorePassword;

    // Utilisation d'un AtomicLong pour la jauge Micrometer (doit conserver une
    // référence mémoire)
    private final AtomicLong daysUntilExpiryGauge;

    public GatewayMtlsCustomizer(
            @Value("${client.ssl.key-store:classpath:config/tls/keystore.p12}") @NonNull Resource keyStoreResource,
            @Value("${client.ssl.key-store-password:password}") @NonNull String keyStorePassword,
            @Value("${client.ssl.trust-store:classpath:config/tls/keystore.p12}") @NonNull Resource trustStoreResource,
            @Value("${client.ssl.trust-store-password:password}") @NonNull String trustStorePassword,
            @NonNull MeterRegistry meterRegistry) { // Injecté, mais non stocké en attribut de classe

        this.keyStoreResource = Objects.requireNonNull(keyStoreResource, "Le KeyStore ne peut pas être nul");
        this.keyStorePassword = Objects.requireNonNull(keyStorePassword,
                "Le mot de passe du KeyStore ne peut pas être nul");
        this.trustStoreResource = Objects.requireNonNull(trustStoreResource, "Le TrustStore ne peut pas être nul");
        this.trustStorePassword = Objects.requireNonNull(trustStorePassword,
                "Le mot de passe du TrustStore ne peut pas être nul");

        // Utilisation immédiate du registre pour attacher la jauge, sans conserver la
        // référence au registre
        Objects.requireNonNull(meterRegistry, "Le MeterRegistry ne peut pas être nul");
        this.daysUntilExpiryGauge = meterRegistry.gauge("gateway_certificate_days_until_expiry", new AtomicLong(0));
    }

    @Bean
    @NonNull
    public HttpClientCustomizer mtlsHttpClientCustomizer() {
        return httpClient -> {
            try {
                // 1. Chargement du KeyStore (Identité de la Gateway)
                KeyStore keyStore = KeyStore.getInstance("PKCS12");
                try (InputStream keyStoreStream = keyStoreResource.getInputStream()) {
                    keyStore.load(keyStoreStream, keyStorePassword.toCharArray());
                }
                KeyManagerFactory keyManagerFactory = KeyManagerFactory
                        .getInstance(KeyManagerFactory.getDefaultAlgorithm());
                keyManagerFactory.init(keyStore, keyStorePassword.toCharArray());

                // 2. Télémétrie : Analyse proactive de l'expiration du certificat
                if (keyStore.aliases().hasMoreElements()) {
                    String alias = keyStore.aliases().nextElement();
                    X509Certificate cert = (X509Certificate) keyStore.getCertificate(alias);
                    if (cert != null) {
                        long daysUntilExpiry = ChronoUnit.DAYS.between(Instant.now(), cert.getNotAfter().toInstant());
                        this.daysUntilExpiryGauge.set(daysUntilExpiry); // Mise à jour de la métrique via l'AtomicLong

                        if (daysUntilExpiry <= 30) {
                            log.warn(
                                    "ALERTE SÉCURITÉ CRITIQUE : Le certificat mTLS de la Gateway expire dans {} jours.",
                                    daysUntilExpiry);
                        } else {
                            log.info("Certificat mTLS valide. Expiration dans {} jours.", daysUntilExpiry);
                        }
                    }
                }

                // 3. Chargement du TrustStore (Autorités de certification validées)
                KeyStore trustStore = KeyStore.getInstance("PKCS12");
                try (InputStream trustStoreStream = trustStoreResource.getInputStream()) {
                    trustStore.load(trustStoreStream, trustStorePassword.toCharArray());
                }
                TrustManagerFactory trustManagerFactory = TrustManagerFactory
                        .getInstance(TrustManagerFactory.getDefaultAlgorithm());
                trustManagerFactory.init(trustStore);

                // 4. Durcissement TLS 1.3 exclusif
                SslContext sslContext = SslContextBuilder.forClient()
                        .keyManager(keyManagerFactory)
                        .trustManager(trustManagerFactory)
                        .protocols("TLSv1.2", "TLSv1.3")
                        .build();

                log.info("Sécurité Zero Trust : mTLS (TLSv1.2, TLSv1.3) configuré pour le routage interne.");

                return httpClient.secure(sslContextSpec -> Objects
                        .requireNonNull(sslContextSpec, "sslContextSpec ne peut pas être nul").sslContext(sslContext));

            } catch (Exception e) {
                log.error("ALERTE CRITIQUE : Impossible d'initialiser le contexte mTLS de la Gateway.", e);
                throw new IllegalStateException("Échec de configuration mTLS", e);
            }
        };
    }
}