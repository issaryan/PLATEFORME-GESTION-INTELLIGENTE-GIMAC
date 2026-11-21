package cm.gimac.des.gateway.web.filter;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.InetSocketAddress;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.server.reactive.ReactorHttpHandlerAdapter;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.adapter.WebHttpHandlerBuilder;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.server.HttpServer;

/** Adresse source posée par la passerelle seule (décision du 21/11/2026, AUTH_E05 de la recette ISO 20022). */
class AdresseSourceWebFilterTest {

    static String vue(AdresseSourceWebFilter f, MockServerHttpRequest requete) {
        AtomicReference<String> vue = new AtomicReference<>();
        f.filter(MockServerWebExchange.from(requete), e -> {
            vue.set(String.join("|", e.getRequest().getHeaders().getOrEmpty(AdresseSourceWebFilter.EN_TETE)));
            return Mono.empty();
        }).block();
        return vue.get();
    }

    static MockServerHttpRequest.BaseBuilder<?> depuis(String ip) {
        return MockServerHttpRequest.post("/sbx/iso20022/facade/send").remoteAddress(new InetSocketAddress(ip, 40000));
    }

    @Test
    @DisplayName("En-tête du client retiré et reposé depuis le pair ; X-Forwarded-For d'un pair non fiable ignoré")
    void pairNonFiable() {
        AdresseSourceWebFilter f = new AdresseSourceWebFilter(List.of());
        assertThat(vue(f, depuis("192.0.2.10").header(AdresseSourceWebFilter.EN_TETE, "198.51.100.7").header("X-Forwarded-For", "198.51.100.8").build()))
            .isEqualTo("192.0.2.10");
        // Sans pair connu : aucun en-tête, jamais celui du client.
        assertThat(vue(f, MockServerHttpRequest.get("/x").header(AdresseSourceWebFilter.EN_TETE, "198.51.100.7").build())).isEmpty();
    }

    @Test
    @DisplayName("Mandataire de confiance : dernier élément de X-Forwarded-For, littéral seulement")
    void mandataire() {
        AdresseSourceWebFilter f = new AdresseSourceWebFilter(List.of(" 127.0.0.1 ", "", "nom-inconnu.invalid"));
        assertThat(vue(f, depuis("127.0.0.1").header("X-Forwarded-For", "198.51.100.8, 192.0.2.20").build())).isEqualTo("192.0.2.20");
        assertThat(vue(f, depuis("127.0.0.1").header("X-Forwarded-For", "2001:db8::1").build())).isEqualTo("2001:db8::1");
        assertThat(vue(f, depuis("127.0.0.1").header("X-Forwarded-For", "nom.exemple").build())).isEqualTo("127.0.0.1");
        assertThat(vue(f, depuis("127.0.0.1").header("X-Forwarded-For", " ").build())).isEqualTo("127.0.0.1");
        assertThat(vue(f, depuis("127.0.0.1").build())).isEqualTo("127.0.0.1");
        assertThat(vue(f, depuis("192.0.2.30").header("X-Forwarded-For", "198.51.100.8").build())).isEqualTo("192.0.2.30");
    }

    @Test
    @DisplayName("Mandataires résolus par nom, résolution renouvelée chaque minute")
    void resolution() {
        Instant debut = Instant.parse("2026-11-21T12:00:00Z");
        Clock[] horloge = { Clock.fixed(debut, ZoneOffset.UTC) };
        AdresseSourceWebFilter f = new AdresseSourceWebFilter(List.of("localhost"), new Clock() {
            @Override public ZoneOffset getZone() { return ZoneOffset.UTC; }
            @Override public Clock withZone(java.time.ZoneId z) { return this; }
            @Override public Instant instant() { return horloge[0].instant(); }
        });
        assertThat(f.resoudre("localhost")).contains("127.0.0.1");
        var premiere = f.resoudre("localhost");
        assertThat(f.resoudre("localhost")).isSameAs(premiere);
        horloge[0] = Clock.fixed(debut.plus(Duration.ofMinutes(2)), ZoneOffset.UTC);
        assertThat(f.resoudre("localhost")).isNotSameAs(premiere);
        assertThat(AdresseSourceWebFilter.adresses("nom-inconnu.invalid")).isEmpty();
        assertThat(AdresseSourceWebFilter.texte(null)).isEmpty();
        assertThat(AdresseSourceWebFilter.texte(InetSocketAddress.createUnresolved("hote", 1))).isEqualTo("hote");
    }

    @Test
    @DisplayName("Route membre déclarée dans la configuration principale : façade externe seule, taille bornée")
    void routeDeclaree() throws Exception {
        String yml = java.nio.file.Files.readString(java.nio.file.Path.of("src/main/resources/config/application.yml"));
        int i = yml.indexOf("- id: membre-sbx-iso20022");
        assertThat(i).isPositive();
        String route = yml.substring(i, yml.indexOf("- id:", i + 5));
        assertThat(route).contains("Path=/sbx/iso20022/facade/**", "RewritePath=/sbx/iso20022/facade/(?<p>.*), /facade/v1/external/${p}", "maxSize: 1MB")
            .doesNotContain("payee", "OpaAuthorize");
    }

    @Test
    @DisplayName("Serveur Netty réel, en-têtes transmis acceptés : X-Forwarded-For forgé ignoré, pair TCP retenu")
    void netty() {
        AtomicReference<String> vue = new AtomicReference<>();
        var gestionnaire = WebHttpHandlerBuilder
            .webHandler(e -> {
                vue.set(e.getRequest().getHeaders().getFirst(AdresseSourceWebFilter.EN_TETE));
                return e.getResponse().setComplete();
            })
            .filter(new AdresseSourceWebFilter(List.of()))
            .build();
        DisposableServer serveur = HttpServer.create().host("127.0.0.1").port(0).forwarded(true).handle(new ReactorHttpHandlerAdapter(gestionnaire)).bindNow();
        try {
            HttpClient
                .create()
                .headers(h -> h.add("X-Forwarded-For", "198.51.100.8").add(AdresseSourceWebFilter.EN_TETE, "198.51.100.7"))
                .get()
                .uri("http://127.0.0.1:" + serveur.port() + "/sbx/iso20022/facade/send")
                .response()
                .block(Duration.ofSeconds(10));
        } finally {
            serveur.disposeNow();
        }
        assertThat(vue.get()).isEqualTo("127.0.0.1");
    }
}
