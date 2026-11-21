package cm.gimac.des.gateway.web.rest;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.gateway.IntegrationTest;
import cm.gimac.des.gateway.web.filter.AdresseSourceWebFilter;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Route membre de la façade ISO 20022 de la Sandbox GIMAC (décision de l'utilisateur du 21/11/2026, AUTH_E05) : hors
 * session, sans CSRF ni serveur de ressources (le jeton OAuth2 de la façade est relayé tel quel), réécrite vers
 * /facade/v1/external seulement ; X-Adresse-Source du client jamais transmis.
 */
@IntegrationTest
@AutoConfigureWebTestClient(timeout = IntegrationTest.DEFAULT_TIMEOUT)
class RouteMembreSandboxIT {

    record Recue(String methode, String chemin, Map<String, List<String>> entetes, String corps) {}

    static final List<Recue> RECUES = new CopyOnWriteArrayList<>();
    static final HttpServer FACADE;

    static {
        try {
            FACADE = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        FACADE.createContext("/", e -> {
            RECUES.add(new Recue(e.getRequestMethod(), e.getRequestURI().getPath(), Map.copyOf(e.getRequestHeaders()), new String(e.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)));
            byte[] r = "{\"error\":\"invalid_grant\",\"error_description\":\"Bad credentials\"}".getBytes(StandardCharsets.UTF_8);
            e.getResponseHeaders().add("Content-Type", "application/json");
            e.sendResponseHeaders(400, r.length);
            e.getResponseBody().write(r);
            e.close();
        });
        FACADE.start();
    }

    @DynamicPropertySource
    static void proprietes(DynamicPropertyRegistry r) {
        // Route du config/application.yml principal (absent du contexte de test), reprise à l'identique.
        String route = "spring.cloud.gateway.server.webflux.routes[0].";
        r.add(route + "id", () -> "membre-sbx-iso20022");
        r.add(route + "uri", () -> "http://127.0.0.1:" + FACADE.getAddress().getPort());
        r.add(route + "predicates[0]", () -> "Path=/sbx/iso20022/facade/**");
        r.add(route + "filters[0]", () -> "RewritePath=/sbx/iso20022/facade/(?<p>.*), /facade/v1/external/$\\{p}");
        r.add("spring.cloud.gateway.server.webflux.default-filters[0]", () -> "TokenRelay");
    }

    @AfterAll
    static void arreter() {
        FACADE.stop(0);
    }

    @Autowired
    WebTestClient client;

    @BeforeEach
    void vider() {
        RECUES.clear();
    }

    @Test
    @DisplayName("Point de jeton : POST de machine sans session ni CSRF, réponse de la façade rendue telle quelle")
    void jeton() {
        client
            .post()
            .uri("/sbx/iso20022/facade/oauth/token")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .header(AdresseSourceWebFilter.EN_TETE, "198.51.100.7")
            .bodyValue("grant_type=password&client_id=restapp&client_secret=restapp&scope=read&username=TST-99101-1&password=faux")
            .exchange()
            .expectStatus()
            .isEqualTo(400)
            .expectBody()
            .jsonPath("$.error")
            .isEqualTo("invalid_grant");
        assertThat(RECUES).singleElement().satisfies(r -> {
            assertThat(r.chemin()).isEqualTo("/facade/v1/external/oauth/token");
            assertThat(r.corps()).contains("grant_type=password");
            assertThat(r.entetes().getOrDefault(AdresseSourceWebFilter.EN_TETE, List.of())).doesNotContain("198.51.100.7");
        });
    }

    @Test
    @DisplayName("Envoi : jeton de la façade relayé sans être lu comme JWT ; payee simulé jamais joignable")
    void envoi() {
        client
            .post()
            .uri("/sbx/iso20022/facade/send")
            .contentType(MediaType.APPLICATION_XML)
            .header("Authorization", "Bearer 81f413ae-6883-464b-8e6a-e0f9ce7a5d22")
            .bodyValue("<Document/>")
            .exchange()
            .expectStatus()
            .isEqualTo(400);
        assertThat(RECUES).singleElement().satisfies(r -> {
            assertThat(r.chemin()).isEqualTo("/facade/v1/external/send");
            assertThat(r.entetes().get("Authorization")).containsExactly("Bearer 81f413ae-6883-464b-8e6a-e0f9ce7a5d22");
        });
        RECUES.clear();
        client.post().uri("/sbx/iso20022/facade/../payee/send").exchange();
        client.post().uri("/sbx/iso20022/payee/send").exchange().expectStatus().value(s -> assertThat(s).isIn(401, 403, 404));
        assertThat(RECUES).noneMatch(r -> r.chemin().contains("/payee/"));
    }
}
