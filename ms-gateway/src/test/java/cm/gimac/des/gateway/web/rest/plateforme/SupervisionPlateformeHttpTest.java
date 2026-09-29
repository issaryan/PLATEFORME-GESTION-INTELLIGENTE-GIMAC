package cm.gimac.des.gateway.web.rest.plateforme;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.gateway.domain.supervision.ImpulsionPlateforme;
import cm.gimac.des.gateway.domain.supervision.ServiceCible;
import cm.gimac.des.gateway.infrastructure.supervision.ActuatorWebClientAdapter;
import cm.gimac.des.gateway.infrastructure.supervision.AlertmanagerWebClientAdapter;
import cm.gimac.des.gateway.infrastructure.supervision.ClientsSupervision;
import cm.gimac.des.gateway.infrastructure.supervision.PrometheusWebClientAdapter;
import cm.gimac.des.gateway.infrastructure.supervision.RoutesPasserelleAdapter;
import cm.gimac.des.gateway.service.supervision.SupervisionPlateformeService;
import cm.gimac.des.gateway.test.util.ServeurHttpLocal;
import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * Intégration du module de supervision (D-61) : ressource REST, service d'application et
 * adaptateurs réels face à des serveurs HTTP locaux qui simulent les services de la plateforme
 * (un en service, un en panne, un trop lent, un arrêté), Keycloak, Prometheus et Alertmanager.
 */
class SupervisionPlateformeHttpTest {

    static final String PROMETHEUS_IAM = """
        # TYPE process_uptime_seconds gauge
        process_uptime_seconds{application="iam"} 120.0
        process_cpu_usage{application="iam"} 0.05
        system_cpu_usage{application="iam"} 0.25
        system_cpu_count{application="iam"} 4.0
        jvm_memory_used_bytes{application="iam",area="heap",id="G1 Eden Space"} 2.5E7
        jvm_memory_used_bytes{application="iam",area="heap",id="G1 Old Gen"} 2.5E7
        jvm_memory_max_bytes{application="iam",area="heap",id="G1 Eden Space"} -1.0
        jvm_memory_max_bytes{application="iam",area="heap",id="G1 Old Gen"} 2.0E8
        disk_free_bytes{application="iam",path="/app/."} 4.0E10
        disk_total_bytes{application="iam",path="/app/."} 1.0E11
        system_load_average_1m{application="iam"} 0.8
        jvm_threads_live_threads{application="iam"} 35.0
        """;

    ServeurHttpLocal iam;
    ServeurHttpLocal audit;
    ServeurHttpLocal lent;
    ServeurHttpLocal keycloak;
    ServeurHttpLocal prometheus;
    ServeurHttpLocal alertmanager;
    String adresseArretee;
    WebTestClient client;

    @BeforeEach
    void demarrer() throws IOException {
        iam = new ServeurHttpLocal()
            .json("/management/health", 200, "{\"status\":\"UP\",\"components\":{\"db\":{\"status\":\"UP\"},\"kafka\":{\"status\":\"UP\"},\"redis\":{\"status\":\"UP\"}}}")
            .json("/management/info", 200, "{\"build\":{\"version\":\"0.0.1-SNAPSHOT\",\"artifact\":\"iam\",\"time\":\"2026-09-29T08:00:00Z\"}}")
            .texte("/management/prometheus", PROMETHEUS_IAM)
            .json(
                "/management/env",
                200,
                "{\"activeProfiles\":[\"prod\"],\"propertySources\":[{\"name\":\"systemEnvironment\",\"properties\":{" +
                "\"spring.datasource.url\":{\"value\":\"jdbc:postgresql://iam:s3cr3t@iam-postgresql:5432/iam\"}," +
                "\"spring.datasource.password\":{\"value\":\"s3cr3t\"}," +
                "\"gimac.securite.client-secret\":{\"value\":\"tres-secret\"}," +
                "\"gimac.opa.url\":{\"value\":\"http://opa:8181\"}," +
                "\"spring.cloud.stream.kafka.binder.brokers\":{\"value\":\"kafka:9092\"}," +
                "\"spring.security.oauth2.client.provider.oidc.issuer-uri\":{\"value\":\"http://keycloak:9080/realms/gimac\"}," +
                "\"gimac.jeton\":{\"value\":\"eyJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJ1In0.c2lnbmF0dXJl\"}," +
                "\"PATH\":{\"value\":\"/usr/bin\"}}}]}"
            )
            .json("/management/loggers", 200, "{\"levels\":[\"OFF\",\"ERROR\",\"WARN\",\"INFO\",\"DEBUG\",\"TRACE\"],\"loggers\":{\"ROOT\":{\"configuredLevel\":\"INFO\",\"effectiveLevel\":\"INFO\"},\"cm.gimac.des\":{\"effectiveLevel\":\"INFO\"}}}")
            .repondre("/management/loggers/cm.gimac.des", 204, "application/json", "", 0);
        audit = new ServeurHttpLocal().json("/management/health", 503, "{\"status\":\"DOWN\",\"components\":{\"db\":{\"status\":\"DOWN\"}}}");
        lent = new ServeurHttpLocal().repondre("/management/health", 200, "application/json", "{\"status\":\"UP\"}", 3000);
        ServeurHttpLocal arrete = new ServeurHttpLocal();
        adresseArretee = arrete.url();
        arrete.close();
        keycloak = new ServeurHttpLocal().json("/health/ready", 200, "{\"status\":\"UP\",\"checks\":[{\"name\":\"Keycloak database connections health check\",\"status\":\"UP\"}]}");
        prometheus = new ServeurHttpLocal()
            .json(
                "/api/v1/targets",
                200,
                "{\"status\":\"success\",\"data\":{\"activeTargets\":[{\"labels\":{\"job\":\"services\",\"instance\":\"iam:8081\"},\"health\":\"up\",\"lastScrape\":\"2026-09-29T10:00:00Z\",\"lastScrapeDuration\":0.01,\"lastError\":\"\"},{\"labels\":{\"job\":\"services\",\"instance\":\"audit:8082\"},\"health\":\"down\",\"lastScrape\":\"2026-09-29T10:00:00Z\",\"lastError\":\"server returned HTTP status 503\"}]}}"
            )
            .json("/api/v1/query", 200, "{\"status\":\"success\",\"data\":{\"resultType\":\"vector\",\"result\":[{\"metric\":{\"instance\":\"node-exporter:9100\"},\"value\":[1695980000,\"37.5\"]}]}}");
        alertmanager = new ServeurHttpLocal()
            .json(
                "/api/v2/alerts",
                200,
                "[{\"labels\":{\"alertname\":\"InstanceDown\",\"severity\":\"page\",\"instance\":\"audit:8082\",\"job\":\"services\"},\"annotations\":{\"summary\":\"Instance audit:8082 down\"},\"startsAt\":\"2026-09-29T09:59:00Z\",\"status\":{\"state\":\"active\"}}]"
            );

        Duration delai = Duration.ofMillis(800);
        List<ServiceCible> services = List.of(
            new ServiceCible("iam", iam.url()),
            new ServiceCible("audit", audit.url()),
            new ServiceCible("lent", lent.url()),
            new ServiceCible("arrete", adresseArretee)
        );
        Route route = Route.async().id("iam_route").uri(iam.url()).predicate(e -> true).build();
        SupervisionPlateformeService service = new SupervisionPlateformeService(
            new SupervisionPlateformeService.Parametres(services, delai, Duration.ofMillis(200), keycloak.url() + "/health/ready"),
            new ActuatorWebClientAdapter(ClientsSupervision.webClient(delai, null), () -> Mono.just("jeton-de-service")),
            new PrometheusWebClientAdapter(ClientsSupervision.webClient(delai, prometheus.url())),
            new AlertmanagerWebClientAdapter(ClientsSupervision.webClient(delai, alertmanager.url())),
            new RoutesPasserelleAdapter(() -> Flux.just(route)),
            Clock.systemUTC()
        );
        client = WebTestClient.bindToController(new PlateformeResource(service)).configureClient().responseTimeout(Duration.ofSeconds(10)).build();
    }

    @AfterEach
    void arreter() {
        for (ServeurHttpLocal s : List.of(iam, audit, lent, keycloak, prometheus, alertmanager)) {
            s.close();
        }
    }

    @Test
    @DisplayName("GET /services : UP avec composants, info et métriques ; DOWN ; lent et arrêté injoignables ; Keycloak ; synthèse")
    void services() {
        long debut = System.nanoTime();
        client
            .get()
            .uri("/api/plateforme/services")
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody()
            .jsonPath("$.global.etat")
            .isEqualTo("DEGRADEE")
            .jsonPath("$.global.total")
            .isEqualTo(4)
            .jsonPath("$.global.up")
            .isEqualTo(1)
            .jsonPath("$.global.down")
            .isEqualTo(3)
            .jsonPath("$.global.injoignables")
            .isEqualTo(2)
            .jsonPath("$.services[0].nom")
            .isEqualTo("iam")
            .jsonPath("$.services[0].etat")
            .isEqualTo("UP")
            .jsonPath("$.services[0].composants.redis")
            .isEqualTo("UP")
            .jsonPath("$.services[0].latenceMs")
            .isNumber()
            .jsonPath("$.services[0].info.version")
            .isEqualTo("0.0.1-SNAPSHOT")
            .jsonPath("$.services[0].metriques.dureeFonctionnementSecondes")
            .isEqualTo(120.0)
            .jsonPath("$.services[0].metriques.nombreCpu")
            .isEqualTo(4)
            .jsonPath("$.services[0].metriques.memoireTasUtiliseeOctets")
            .isEqualTo(5.0E7)
            .jsonPath("$.services[0].metriques.memoireTasMaxOctets")
            .isEqualTo(2.0E8)
            .jsonPath("$.services[0].metriques.memoireTasPourcent")
            .isEqualTo(25.0)
            .jsonPath("$.services[0].metriques.disqueLibreOctets")
            .isEqualTo(4.0E10)
            .jsonPath("$.services[0].metriques.threadsActifs")
            .isEqualTo(35)
            .jsonPath("$.services[1].etat")
            .isEqualTo("DOWN")
            .jsonPath("$.services[1].composants.db")
            .isEqualTo("DOWN")
            .jsonPath("$.services[2].etat")
            .isEqualTo("INJOIGNABLE")
            .jsonPath("$.services[2].erreur")
            .isEqualTo("délai dépassé")
            .jsonPath("$.services[3].etat")
            .isEqualTo("INJOIGNABLE")
            .jsonPath("$.keycloak.etat")
            .isEqualTo("UP");
        // Le service lent (3 s) ne retarde pas la réponse au-delà du délai (0,8 s) et de la marge.
        assertThat(Duration.ofNanos(System.nanoTime() - debut)).isLessThan(Duration.ofMillis(2800));
        assertThat(iam.recues()).allSatisfy(r -> assertThat(r.autorisation()).isEqualTo("Bearer jeton-de-service"));
    }

    @Test
    @DisplayName("GET /serveurs : cibles de collecte et hôtes node-exporter")
    void serveurs() {
        client
            .get()
            .uri("/api/plateforme/serveurs")
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody()
            .jsonPath("$.prometheusJoignable")
            .isEqualTo(true)
            .jsonPath("$.ciblesUp")
            .isEqualTo(1)
            .jsonPath("$.ciblesDown")
            .isEqualTo(1)
            .jsonPath("$.cibles[0].instance")
            .isEqualTo("audit:8082")
            .jsonPath("$.cibles[0].erreur")
            .isEqualTo("server returned HTTP status 503")
            .jsonPath("$.nodeExporter")
            .isEqualTo(true)
            .jsonPath("$.hotes[0].instance")
            .isEqualTo("node-exporter:9100")
            .jsonPath("$.hotes[0].cpuPourcent")
            .isEqualTo(37.5);
    }

    @Test
    @DisplayName("GET /serveurs et /alertes : Prometheus et Alertmanager arrêtés signalés sans erreur HTTP")
    void sourcesArretees() {
        prometheus.close();
        alertmanager.close();
        client.get().uri("/api/plateforme/serveurs").exchange().expectStatus().isOk().expectBody().jsonPath("$.prometheusJoignable").isEqualTo(false);
        client.get().uri("/api/plateforme/alertes").exchange().expectStatus().isOk().expectBody().jsonPath("$.joignable").isEqualTo(false);
    }

    @Test
    @DisplayName("GET /alertes : nom, sévérité, résumé, depuis, cible")
    void alertes() {
        client
            .get()
            .uri("/api/plateforme/alertes")
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody()
            .jsonPath("$.nombre")
            .isEqualTo(1)
            .jsonPath("$.alertes[0].nom")
            .isEqualTo("InstanceDown")
            .jsonPath("$.alertes[0].severite")
            .isEqualTo("page")
            .jsonPath("$.alertes[0].resume")
            .isEqualTo("Instance audit:8082 down")
            .jsonPath("$.alertes[0].depuis")
            .isEqualTo("2026-09-29T09:59:00Z")
            .jsonPath("$.alertes[0].cible")
            .isEqualTo("audit:8082");
    }

    @Test
    @DisplayName("GET /configuration : routes et propriétés des services, aucun secret en clair")
    void configuration() {
        String corps = new String(
            client.get().uri("/api/plateforme/configuration").exchange().expectStatus().isOk().expectBody().returnResult().getResponseBodyContent()
        );
        assertThat(corps).doesNotContain("s3cr3t").doesNotContain("tres-secret").doesNotContain("eyJhbGciOiJSUzI1NiJ9").doesNotContain("/usr/bin");
        client
            .get()
            .uri("/api/plateforme/configuration")
            .exchange()
            .expectBody()
            .jsonPath("$.routes[0].id")
            .isEqualTo("iam_route")
            .jsonPath("$.services[0].lue")
            .isEqualTo(true)
            .jsonPath("$.services[0].profilsActifs[0]")
            .isEqualTo("prod")
            .jsonPath("$.services[0].proprietes['spring.datasource.url']")
            .isEqualTo("jdbc:postgresql://iam:******@iam-postgresql:5432/iam")
            .jsonPath("$.services[0].proprietes['gimac.securite.client-secret']")
            .isEqualTo("******")
            .jsonPath("$.services[0].proprietes['gimac.jeton']")
            .isEqualTo("******")
            .jsonPath("$.services[0].proprietes['spring.cloud.stream.kafka.binder.brokers']")
            .isEqualTo("kafka:9092")
            .jsonPath("$.services[0].proprietes['spring.security.oauth2.client.provider.oidc.issuer-uri']")
            .isEqualTo("http://keycloak:9080/realms/gimac")
            .jsonPath("$.services[1].lue")
            .isEqualTo(false);
    }

    @Test
    @DisplayName("Journalisation : lecture filtrée, changement à chaud posté au service, service inconnu 404, niveau invalide 400")
    void journalisation() {
        client
            .get()
            .uri("/api/plateforme/journalisation/iam?filtre=cm.gimac")
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody()
            .jsonPath("$.loggers.length()")
            .isEqualTo(1)
            .jsonPath("$.loggers[0].nom")
            .isEqualTo("cm.gimac.des")
            .jsonPath("$.niveauxDisponibles.length()")
            .isEqualTo(6);
        client
            .post()
            .uri("/api/plateforme/journalisation/iam")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(Map.of("logger", "cm.gimac.des", "niveau", "debug", "motif", "Analyse de l'incident INC-42"))
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody()
            .jsonPath("$.niveau")
            .isEqualTo("DEBUG")
            .jsonPath("$.auteur")
            .isEqualTo("inconnu");
        assertThat(iam.recues()).anySatisfy(r -> {
            assertThat(r.methode()).isEqualTo("POST");
            assertThat(r.chemin()).isEqualTo("/management/loggers/cm.gimac.des");
            assertThat(r.corps()).isEqualTo("{\"configuredLevel\":\"DEBUG\"}");
        });
        client.get().uri("/api/plateforme/journalisation/inconnu").exchange().expectStatus().isNotFound();
        client
            .post()
            .uri("/api/plateforme/journalisation/iam")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(Map.of("logger", "ROOT", "niveau", "FATAL", "motif", "motif suffisant"))
            .exchange()
            .expectStatus()
            .isBadRequest();
        client.get().uri("/api/plateforme/journalisation/audit").exchange().expectStatus().isEqualTo(HttpStatus.BAD_GATEWAY);
    }

    @Test
    @DisplayName("GET /flux : événements SSE « plateforme » poussés périodiquement avec le nombre d'alertes")
    void flux() {
        Flux<ServerSentEvent<ImpulsionPlateforme>> evenements = client
            .get()
            .uri("/api/plateforme/flux")
            .accept(MediaType.TEXT_EVENT_STREAM)
            .exchange()
            .expectStatus()
            .isOk()
            .expectHeader()
            .contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM)
            .returnResult(new ParameterizedTypeReference<ServerSentEvent<ImpulsionPlateforme>>() {})
            .getResponseBody();
        StepVerifier.create(evenements.take(2))
            .assertNext(e -> {
                assertThat(e.event()).isEqualTo("plateforme");
                assertThat(e.id()).isEqualTo("0");
                assertThat(e.data().nombreAlertes()).isEqualTo(1);
                assertThat(e.data().instantane().services()).hasSize(4);
            })
            .assertNext(e -> assertThat(e.id()).isEqualTo("1"))
            .expectComplete()
            .verify(Duration.ofSeconds(15));
    }

    @Test
    @DisplayName("Traduction des erreurs : 404, 400, statut conservé, 502")
    void traduction() {
        assertThat(PlateformeResource.traduire(new cm.gimac.des.gateway.domain.supervision.ServiceInconnuException("x"))).hasMessageContaining("404");
        assertThat(PlateformeResource.traduire(new IllegalArgumentException("x"))).hasMessageContaining("400");
        var statut = new org.springframework.web.server.ResponseStatusException(HttpStatus.CONFLICT);
        assertThat(PlateformeResource.traduire(statut)).isSameAs(statut);
        assertThat(PlateformeResource.traduire(new IllegalStateException("x"))).hasMessageContaining("502");
    }
}
