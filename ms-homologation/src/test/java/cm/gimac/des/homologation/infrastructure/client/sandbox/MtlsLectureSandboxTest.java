package cm.gimac.des.homologation.infrastructure.client.sandbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import cm.gimac.des.homologation.application.port.out.LectureSandboxPort.Indisponible;
import cm.gimac.des.homologation.config.ServiceTokenConfiguration;
import cm.gimac.des.homologation.infrastructure.client.ClientMtls;
import cm.gimac.des.homologation.infrastructure.client.ServiceTokenInterceptor;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsExchange;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.KeyStore;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.TrustManagerFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.ssl.DefaultSslBundleRegistry;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.jks.JksSslStoreBundle;
import org.springframework.boot.ssl.jks.JksSslStoreDetails;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.web.client.RestClient;

/**
 * Certificat client présenté en mTLS (RFC 8705, CC-SBX-GIMAC-02 §D.8) : à SBX-LECTURE par la passerelle interne, et au
 * fournisseur d'identité pour obtenir un jeton lié. Serveur HTTPS de test exigeant le certificat client ; le client ne
 * pose jamais lui-même d'en-tête d'empreinte.
 */
class MtlsLectureSandboxTest {

    private static final String MOT_DE_PASSE = "changeit";
    private static final List<String> VUS = new CopyOnWriteArrayList<>();

    @TempDir
    static Path dossier;

    private static HttpsServer serveur;
    private static DefaultSslBundleRegistry paquets;
    private static String base;

    @BeforeAll
    static void demarrer() throws Exception {
        Path magasin = dossier.resolve("service.p12");
        Process p = new ProcessBuilder(
            Path.of(System.getProperty("java.home"), "bin", "keytool").toString(), "-genkeypair", "-alias", "service", "-keyalg", "EC", "-groupname", "secp256r1",
            "-dname", "CN=localhost", "-ext", "SAN=dns:localhost", "-validity", "2", "-storetype", "PKCS12", "-keystore", magasin.toString(),
            "-storepass", MOT_DE_PASSE, "-keypass", MOT_DE_PASSE
        ).redirectErrorStream(true).start();
        assertThat(p.waitFor()).isZero();
        KeyStore ks = KeyStore.getInstance("PKCS12");
        try (var in = java.nio.file.Files.newInputStream(magasin)) {
            ks.load(in, MOT_DE_PASSE.toCharArray());
        }
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ks, MOT_DE_PASSE.toCharArray());
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(ks);
        SSLContext contexte = SSLContext.getInstance("TLS");
        contexte.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
        serveur = HttpsServer.create(new InetSocketAddress("localhost", 0), 0);
        serveur.setHttpsConfigurator(
            new HttpsConfigurator(contexte) {
                @Override
                public void configure(HttpsParameters parametres) {
                    var ssl = contexte.getDefaultSSLParameters();
                    ssl.setNeedClientAuth(true);
                    parametres.setSSLParameters(ssl);
                }
            }
        );
        serveur.createContext("/v1/liaisons", e -> repondre(e, "{\"campagnes\":[]}"));
        serveur.createContext("/token", e -> repondre(e, "{\"access_token\":\"jeton-lie\",\"token_type\":\"Bearer\",\"expires_in\":300}"));
        serveur.start();
        base = "https://localhost:" + serveur.getAddress().getPort();
        JksSslStoreDetails details = new JksSslStoreDetails("PKCS12", null, magasin.toString(), MOT_DE_PASSE);
        paquets = new DefaultSslBundleRegistry("des-client", SslBundle.of(new JksSslStoreBundle(details, details), org.springframework.boot.ssl.SslBundleKey.of(MOT_DE_PASSE)));
    }

    @AfterAll
    static void arreter() {
        serveur.stop(0);
    }

    /** Consigne le sujet du certificat client et l'éventuel en-tête d'empreinte, puis répond en JSON. */
    private static void repondre(HttpExchange e, String corps) throws IOException {
        String sujet;
        try {
            sujet = ((HttpsExchange) e).getSSLSession().getPeerPrincipal().getName();
        } catch (SSLPeerUnverifiedException ex) {
            sujet = "aucun";
        }
        VUS.add(e.getRequestURI().getPath() + " " + sujet + " " + e.getRequestHeaders().getFirst("X-Client-Cert-SHA256") + " " + e.getRequestHeaders().getFirst("Authorization"));
        byte[] b = corps.getBytes(StandardCharsets.UTF_8);
        e.getResponseHeaders().set("Content-Type", "application/json");
        e.sendResponseHeaders(200, b.length);
        e.getResponseBody().write(b);
        e.close();
    }

    private static ServiceTokenInterceptor jeton(OAuth2AuthorizedClientManager manager) {
        return new ServiceTokenInterceptor(manager);
    }

    @Test
    @DisplayName("Paquet désigné : le jeton est demandé en mTLS, puis SBX-LECTURE est lue en mTLS avec ce jeton, sans en-tête d'empreinte")
    void certificatPresente() {
        ClientRegistration enregistrement = ClientRegistration.withRegistrationId("service-token")
            .clientId("homologation-service").clientSecret("secret-de-test").authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
            .tokenUri(base + "/token").build();
        var depot = new InMemoryClientRegistrationRepository(enregistrement);
        OAuth2AuthorizedClientManager manager = new ServiceTokenConfiguration().serviceTokenClientManager(depot, new InMemoryOAuth2AuthorizedClientService(depot), paquets, " des-client ");
        var adaptateur = new LectureSandboxRestAdapter(RestClient.builder(), jeton(manager), paquets, base, "des-client");

        assertThat(adaptateur.campagnes(UUID.randomUUID())).isEmpty();
        assertThat(VUS).anyMatch(v -> v.startsWith("/token CN=localhost null"));
        assertThat(VUS).anyMatch(v -> v.startsWith("/v1/liaisons/") && v.endsWith("CN=localhost null Bearer jeton-lie"));
        assertThat(manager.authorize(OAuth2AuthorizeRequest.withClientRegistrationId("service-token").principal("service").build()).getAccessToken().getTokenValue()).isEqualTo("jeton-lie");
    }

    @Test
    @DisplayName("Sans paquet : fabrique par défaut, poignée de main refusée (Indisponible) ; adresse http refusée en mTLS")
    void sansCertificat() {
        assertThat(ClientMtls.fabrique(paquets, null)).isEmpty();
        assertThat(ClientMtls.fabrique(paquets, " ")).isEmpty();
        var sans = new LectureSandboxRestAdapter(LectureSandboxRestAdapter.construire(RestClient.builder(), jeton(mock(OAuth2AuthorizedClientManager.class)), paquets, base, ""));
        assertThat(sans).isNotNull();
        assertThatThrownBy(() -> LectureSandboxRestAdapter.construire(RestClient.builder(), jeton(mock(OAuth2AuthorizedClientManager.class)), paquets, "http://sbx-lecture:8111", "des-client"))
            .isInstanceOf(IllegalStateException.class).hasMessageContaining("https");
        var nu = new LectureSandboxRestAdapter(RestClient.builder().baseUrl(base).build());
        assertThatThrownBy(() -> nu.campagnes(UUID.randomUUID())).isInstanceOf(Indisponible.class);
    }
}
