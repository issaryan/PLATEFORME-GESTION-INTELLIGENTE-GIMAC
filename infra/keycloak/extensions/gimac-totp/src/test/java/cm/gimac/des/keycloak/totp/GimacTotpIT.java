package cm.gimac.des.keycloak.totp;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.keycloak.models.utils.Base32;
import org.keycloak.models.utils.TimeBasedOTP;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.MountableFile;

/**
 * Extension gimac-totp chargée dans un vrai Keycloak (image du Dockerfile) avec le domaine gimac généré
 * (D-65) : authentification réservée au compte de service iam-service (401, 403), enregistrement d'un
 * code vérifié, puis connexion par octroi direct à deux facteurs sur web_app (acr 2) et à un facteur sur
 * gimac-connexion (acr 1) ; aucune action requise Keycloak ne bloque l'octroi direct.
 */
class GimacTotpIT {

    static final String IMAGE = "quay.io/keycloak/keycloak:26.7.4-0";
    static final String MOT_DE_PASSE = "Test-Gimac-2026-IT";
    static final Map<String, String> SECRETS = Map.of(
        "GIMAC_SECRET_IAM_SERVICE", "secret-iam",
        "GIMAC_SECRET_GATEWAY_SERVICE", "secret-passerelle",
        "GIMAC_SECRET_CONNEXION", "secret-connexion",
        "GIMAC_SECRET_WEB_APP", "secret-web-app"
    );
    static final ObjectMapper JSON = new ObjectMapper();
    static final HttpClient HTTP = HttpClient.newBuilder().proxy(HttpClient.Builder.NO_PROXY).connectTimeout(Duration.ofSeconds(10)).build();

    static GenericContainer<?> keycloak;
    static String base;

    @BeforeAll
    @SuppressWarnings("resource")
    static void demarrer() {
        Path jar = Path.of(System.getProperty("gimac.totp.jar", "target/gimac-totp.jar"));
        assertThat(Files.exists(jar)).as("jar de l'extension construit (mvn verify)").isTrue();
        keycloak = new GenericContainer<>(IMAGE)
            .withExposedPorts(8080)
            .withEnv("KC_BOOTSTRAP_ADMIN_USERNAME", "admin")
            .withEnv("KC_BOOTSTRAP_ADMIN_PASSWORD", "admin")
            .withEnv("GIMAC_MOT_DE_PASSE_TEST", MOT_DE_PASSE)
            .withCopyFileToContainer(MountableFile.forHostPath(jar), "/opt/keycloak/providers/gimac-totp.jar")
            .withCopyFileToContainer(MountableFile.forHostPath(Path.of("../../gimac-realm.json")), "/opt/keycloak/data/import/gimac-realm.json")
            .withCommand("start-dev", "--import-realm")
            .waitingFor(Wait.forHttp("/realms/gimac").forStatusCode(200).withStartupTimeout(Duration.ofMinutes(4)));
        SECRETS.forEach(keycloak::withEnv);
        for (String autre : new String[] { "TESTER_CLI", "HOMOLOGATION_SERVICE", "CERTIFICATION_SERVICE", "PARTICIPANT_SERVICE", "GATEWAY_SUPERVISION", "COLLABORATION_SERVICE", "INTERNAL" }) {
            keycloak.withEnv("GIMAC_SECRET_" + autre, "x");
        }
        keycloak.start();
        base = "http://" + keycloak.getHost() + ":" + keycloak.getMappedPort(8080) + "/realms/gimac";
    }

    @AfterAll
    static void arreter() {
        if (keycloak != null) keycloak.stop();
    }

    static HttpResponse<String> jeton(Map<String, String> formulaire) throws Exception {
        String corps = formulaire.entrySet().stream().map(e -> e.getKey() + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8)).collect(Collectors.joining("&"));
        return HTTP.send(
            HttpRequest.newBuilder(URI.create(base + "/protocol/openid-connect/token")).header("Content-Type", "application/x-www-form-urlencoded").POST(HttpRequest.BodyPublishers.ofString(corps)).build(),
            HttpResponse.BodyHandlers.ofString()
        );
    }

    static String service(String client) throws Exception {
        String secret = SECRETS.get("GIMAC_SECRET_" + client.toUpperCase().replace("-", "_"));
        HttpResponse<String> r = jeton(Map.of("grant_type", "client_credentials", "client_id", client, "client_secret", secret));
        assertThat(r.statusCode()).as(r.body()).isEqualTo(200);
        return JSON.readTree(r.body()).get("access_token").asText();
    }

    static HttpResponse<String> motDePasse(String client, String utilisateur, String totp) throws Exception {
        String secret = SECRETS.get(client.equals("web_app") ? "GIMAC_SECRET_WEB_APP" : "GIMAC_SECRET_CONNEXION");
        var f = new java.util.HashMap<>(Map.of("grant_type", "password", "client_id", client, "client_secret", secret, "username", utilisateur, "password", MOT_DE_PASSE, "scope", "openid"));
        if (totp != null) f.put("otp", totp);
        return jeton(f);
    }

    static JsonNode charge(String jwt) throws Exception {
        return JSON.readTree(Base64.getUrlDecoder().decode(jwt.split("\\.")[1]));
    }

    static HttpResponse<String> appel(String methode, String chemin, String jeton, String corps) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(base + "/gimac-totp" + chemin)).header("Content-Type", "application/json");
        if (jeton != null) b.header("Authorization", "Bearer " + jeton);
        b.method(methode, corps == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(corps));
        return HTTP.send(b.build(), HttpResponse.BodyHandlers.ofString());
    }

    static String id(String iam, String utilisateur) throws Exception {
        HttpResponse<String> r = HTTP.send(
            HttpRequest.newBuilder(URI.create(base.replace("/realms/", "/admin/realms/") + "/users?exact=true&username=" + utilisateur)).header("Authorization", "Bearer " + iam).build(),
            HttpResponse.BodyHandlers.ofString()
        );
        return JSON.readTree(r.body()).get(0).get("id").asText();
    }

    @Test
    @DisplayName("Code TOTP enregistré par iam-service, puis octroi direct acr 1 (gimac-connexion) et acr 2 (web_app avec code)")
    void parcours() throws Exception {
        String iam = service("iam-service");
        String sujet = id(iam, "test-chef-ddrm");
        String chemin = "/utilisateurs/" + sujet + "/totp";

        // Premier accès : mot de passe seul accepté sur gimac-connexion (acr 1), aucune action Keycloak ne bloque.
        HttpResponse<String> premier = motDePasse("gimac-connexion", "test-chef-ddrm", null);
        assertThat(premier.statusCode()).as(premier.body()).isEqualTo(200);
        JsonNode acces = charge(JSON.readTree(premier.body()).get("access_token").asText());
        assertThat(acces.get("acr").asText()).isEqualTo("1");
        assertThat(acces.get("sub").asText()).isEqualTo(sujet);
        // Sans code configuré, web_app refuse (Keycloak répond 500 : identifiant OTP absent pour un exécuteur
        // REQUIRED) sans poser d'action requise : l'octroi à un facteur reste possible.
        assertThat(motDePasse("web_app", "test-chef-ddrm", "123456").statusCode()).isNotEqualTo(200);
        assertThat(motDePasse("gimac-connexion", "test-chef-ddrm", null).statusCode()).isEqualTo(200);

        // Accès : 401 sans jeton, 403 pour un utilisateur ou un compte de service sans manage-users.
        assertThat(appel("GET", chemin, null, null).statusCode()).isEqualTo(401);
        assertThat(appel("GET", chemin, "jeton-invalide", null).statusCode()).isEqualTo(401);
        assertThat(appel("GET", chemin, JSON.readTree(premier.body()).get("access_token").asText(), null).statusCode()).isEqualTo(403);
        assertThat(appel("GET", chemin, service("gateway-service"), null).statusCode()).isEqualTo(403);
        assertThat(appel("GET", "/utilisateurs/inconnu/totp", iam, null).statusCode()).isEqualTo(404);
        assertThat(appel("GET", chemin, iam, null).body()).contains("\"configure\":false");

        byte[] octets = new byte[20];
        new SecureRandom().nextBytes(octets);
        String secret = Base32.encode(octets);
        String code = new TimeBasedOTP("HmacSHA1", 6, 30, 1).generateTOTP(octets);
        String faux = code.equals("000000") ? "111111" : "000000";
        HttpResponse<String> refus = appel("POST", chemin, iam, "{\"secret\":\"" + secret + "\",\"code\":\"" + faux + "\"}");
        assertThat(refus.statusCode()).isEqualTo(400);
        assertThat(refus.body()).contains("GIMAC_TOTP_CODE_INVALIDE");
        HttpResponse<String> ok = appel("POST", chemin, iam, "{\"secret\":\"" + secret + "\",\"code\":\"" + code + "\",\"libelle\":\"Téléphone\"}");
        assertThat(ok.statusCode()).as(ok.body()).isEqualTo(204);
        assertThat(appel("GET", chemin, iam, null).body()).contains("\"configure\":true");

        // Connexion à deux facteurs : web_app exige le code et pose acr 2 ; sans code, refus.
        assertThat(motDePasse("web_app", "test-chef-ddrm", null).statusCode()).isIn(400, 401);
        HttpResponse<String> deux = motDePasse("web_app", "test-chef-ddrm", code);
        assertThat(deux.statusCode()).as(deux.body()).isEqualTo(200);
        JsonNode fort = charge(JSON.readTree(deux.body()).get("access_token").asText());
        assertThat(fort.get("acr").asText()).isEqualTo("2");
        assertThat(fort.get("exp").asLong() - fort.get("iat").asLong()).isLessThanOrEqualTo(300);
        // Jeton d'identité : acr, rôles et permissions comme dans le jeton d'accès (session de la passerelle).
        JsonNode identite = charge(JSON.readTree(deux.body()).get("id_token").asText());
        assertThat(identite.get("acr").asText()).isEqualTo("2");
        assertThat(identite.get("roles").toString()).contains("ROLE_CHEF_DDRM").contains("PAR_TRANSITION_APPROVE");
        assertThat(fort.get("roles").toString()).contains("ROLE_CHEF_DDRM");
        // Révocation du jeton de rafraîchissement (déconnexion par la passerelle).
        HttpResponse<String> revocation = HTTP.send(
            HttpRequest.newBuilder(URI.create(base + "/protocol/openid-connect/revoke"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString("client_id=web_app&client_secret=secret-web-app&token_type_hint=refresh_token&token=" + JSON.readTree(deux.body()).get("refresh_token").asText()))
                .build(),
            HttpResponse.BodyHandlers.ofString()
        );
        assertThat(revocation.statusCode()).isEqualTo(200);

        assertThat(appel("DELETE", chemin, iam, null).statusCode()).isEqualTo(204);
        assertThat(appel("GET", chemin, iam, null).body()).contains("\"configure\":false");
    }
}
