package cm.gimac.des.gateway.infrastructure.supervision;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import cm.gimac.des.gateway.domain.supervision.Alerte;
import cm.gimac.des.gateway.domain.supervision.CibleCollecte;
import cm.gimac.des.gateway.domain.supervision.EnvironnementService;
import cm.gimac.des.gateway.domain.supervision.EtatConteneur;
import cm.gimac.des.gateway.domain.supervision.EtatHote;
import cm.gimac.des.gateway.domain.supervision.EtatSante;
import cm.gimac.des.gateway.domain.supervision.InfoService;
import cm.gimac.des.gateway.domain.supervision.Metriques;
import cm.gimac.des.gateway.domain.supervision.NiveauxJournalisation;
import cm.gimac.des.gateway.domain.supervision.RouteVue;
import cm.gimac.des.gateway.domain.supervision.Sante;
import cm.gimac.des.gateway.domain.supervision.ServiceCible;
import cm.gimac.des.gateway.test.util.ServeurHttpLocal;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.OrderedGatewayFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import reactor.core.publisher.Mono;

/**
 * Adaptateurs WebClient de la supervision (D-61) face à des serveurs HTTP locaux qui simulent
 * un service Spring Boot, Prometheus et Alertmanager ; lecture tolérante des réponses.
 */
class AdaptateursSupervisionTest {

    static final Duration DELAI = Duration.ofSeconds(2);

    ServeurHttpLocal service;
    ServeurHttpLocal prometheus;
    ServeurHttpLocal alertmanager;
    ServiceCible cible;
    ActuatorWebClientAdapter actuator;

    @BeforeEach
    void demarrer() throws IOException {
        service = new ServeurHttpLocal();
        prometheus = new ServeurHttpLocal();
        alertmanager = new ServeurHttpLocal();
        cible = new ServiceCible("iam", service.url() + "/");
        actuator = new ActuatorWebClientAdapter(ClientsSupervision.webClient(DELAI, null), () -> Mono.just("jeton-de-service"));
    }

    @AfterEach
    void arreter() {
        service.close();
        prometheus.close();
        alertmanager.close();
    }

    @Test
    @DisplayName("Santé : état et composants lus dans le corps, jeton de service transmis")
    void sante() {
        service.json("/management/health", 200, "{\"status\":\"UP\",\"components\":{\"db\":{\"status\":\"UP\"},\"kafka\":{\"status\":\"DOWN\"}}}");
        Sante s = actuator.sante(cible).block();
        assertThat(s.etat()).isEqualTo(EtatSante.UP);
        assertThat(s.composants()).containsEntry("db", "UP").containsEntry("kafka", "DOWN");
        assertThat(service.recues().get(0).autorisation()).isEqualTo("Bearer jeton-de-service");
        assertThat(service.recues().get(0).chemin()).isEqualTo("/management/health");
    }

    @Test
    @DisplayName("Santé 503 : DOWN lu dans le corps ; corps illisible : état déduit du statut HTTP")
    void santeEnPanne() {
        service.json("/management/health", 503, "{\"status\":\"OUT_OF_SERVICE\"}");
        assertThat(actuator.sante(cible).block().etat()).isEqualTo(EtatSante.OUT_OF_SERVICE);
        service.repondre("/management/health", 503, "text/html", "<html>panne</html>", 0);
        assertThat(actuator.sante(cible).block().etat()).isEqualTo(EtatSante.DOWN);
        service.repondre("/management/health", 200, "text/plain", "", 0);
        assertThat(actuator.sante(cible).block().etat()).isEqualTo(EtatSante.UP);
        assertThat(ActuatorWebClientAdapter.lireSante(Map.of(), HttpStatus.UNAUTHORIZED).etat()).isEqualTo(EtatSante.INCONNU);
    }

    @Test
    @DisplayName("Santé de Keycloak : vérifications MicroProfile Health ; sans jeton disponible, aucun en-tête")
    void santeKeycloak() {
        service.json("/health/ready", 200, "{\"status\":\"UP\",\"checks\":[{\"name\":\"Keycloak database connections health check\",\"status\":\"UP\"},{\"status\":\"UP\"}]}");
        ActuatorWebClientAdapter sansJeton = new ActuatorWebClientAdapter(ClientsSupervision.webClient(DELAI, null), Mono::empty);
        Sante s = sansJeton.santeExterne(service.url() + "/health/ready").block();
        assertThat(s.etat()).isEqualTo(EtatSante.UP);
        assertThat(s.composants()).containsOnlyKeys("Keycloak database connections health check");
        assertThat(service.recues().get(0).autorisation()).isNull();
        ActuatorWebClientAdapter jetonEnErreur = new ActuatorWebClientAdapter(ClientsSupervision.webClient(DELAI, null), () -> Mono.error(new IllegalStateException("x")));
        assertThat(jetonEnErreur.santeExterne(service.url() + "/health/ready").block().etat()).isEqualTo(EtatSante.UP);
    }

    @Test
    @DisplayName("Info : version, artefact, date, commit abrégé ou simple, branche")
    void info() {
        service.json(
            "/management/info",
            200,
            "{\"build\":{\"version\":\"0.0.1\",\"artifact\":\"iam\",\"time\":\"2026-09-29T08:00:00Z\"},\"git\":{\"branch\":\"main\",\"commit\":{\"id\":{\"abbrev\":\"51cec2f\",\"full\":\"51cec2f0\"}}}}"
        );
        assertThat(actuator.info(cible).block()).isEqualTo(new InfoService("0.0.1", "iam", "2026-09-29T08:00:00Z", "51cec2f", "main"));
        assertThat(ActuatorWebClientAdapter.lireInfo(Map.of("build", Map.of("name", "audit"), "git", Map.of("commit", Map.of("id", "abc"))))).isEqualTo(
            new InfoService(null, "audit", null, "abc", null)
        );
        assertThat(ActuatorWebClientAdapter.lireInfo(Map.of())).isEqualTo(InfoService.VIDE);
    }

    @Test
    @DisplayName("Métriques : exposition Prometheus lue ligne à ligne et analysée")
    void metriques() {
        service.texte(
            "/management/prometheus",
            "# HELP x\nprocess_uptime_seconds 12.0\njvm_threads_live_threads{application=\"iam\"} 30\nhttp_server_requests_seconds_count{uri=\"/\"} 4\n"
        );
        Metriques m = actuator.metriques(cible).block();
        assertThat(m.dureeFonctionnementSecondes()).isEqualTo(12.0);
        assertThat(m.threadsActifs()).isEqualTo(30);
    }

    @Test
    @DisplayName("Environnement : valeur de la première source, profils actifs")
    void environnement() {
        service.json(
            "/management/env",
            200,
            "{\"activeProfiles\":[\"prod\",\"kafka\"],\"propertySources\":[{\"name\":\"systemEnvironment\",\"properties\":{\"gimac.opa.url\":{\"value\":\"http://opa:8181\"}}},{\"name\":\"application.yml\",\"properties\":{\"gimac.opa.url\":{\"value\":\"http://localhost:8181\"},\"spring.datasource.password\":{\"value\":\"******\"}}}]}"
        );
        EnvironnementService e = actuator.environnement(cible).block();
        assertThat(e.profilsActifs()).containsExactly("prod", "kafka");
        assertThat(e.proprietes()).containsEntry("gimac.opa.url", "http://opa:8181").containsEntry("spring.datasource.password", "******");
    }

    @Test
    @DisplayName("Loggers : niveaux disponibles, niveaux configuré et effectif ; changement à chaud posté")
    void loggers() {
        service.json(
            "/management/loggers",
            200,
            "{\"levels\":[\"OFF\",\"ERROR\",\"INFO\"],\"loggers\":{\"ROOT\":{\"configuredLevel\":\"INFO\",\"effectiveLevel\":\"INFO\"},\"cm.gimac\":{\"effectiveLevel\":\"INFO\"}}}"
        );
        NiveauxJournalisation n = actuator.journalisation(cible).block();
        assertThat(n.service()).isEqualTo("iam");
        assertThat(n.niveauxDisponibles()).containsExactly("OFF", "ERROR", "INFO");
        assertThat(n.loggers()).hasSize(2);
        assertThat(n.loggers().get(1).niveauConfigure()).isNull();

        service.repondre("/management/loggers/cm.gimac.des", 204, "application/json", "", 0);
        actuator.modifierNiveau(cible, "cm.gimac.des", "DEBUG").block();
        ServeurHttpLocal.Recue post = service.recues().get(service.recues().size() - 1);
        assertThat(post.methode()).isEqualTo("POST");
        assertThat(post.corps()).isEqualTo("{\"configuredLevel\":\"DEBUG\"}");
        service.repondre("/management/loggers/ROOT", 204, "application/json", "", 0);
        actuator.modifierNiveau(cible, "ROOT", null).block();
        assertThat(service.recues().get(service.recues().size() - 1).corps()).isEqualTo("{\"configuredLevel\":null}");
    }

    @Test
    @DisplayName("Prometheus : cibles triées (dernière collecte, erreur), hôtes node-exporter et conteneurs cAdvisor")
    void prometheus() {
        prometheus.json(
            "/api/v1/targets",
            200,
            "{\"status\":\"success\",\"data\":{\"activeTargets\":[" +
            "{\"labels\":{\"job\":\"services\",\"instance\":\"iam:8081\"},\"health\":\"up\",\"lastScrape\":\"2026-09-29T10:00:00.123+01:00\",\"lastScrapeDuration\":0.012,\"lastError\":\"\"}," +
            "{\"labels\":{\"job\":\"node-exporter\"},\"scrapeUrl\":\"http://node-exporter:9100/metrics\",\"health\":\"down\",\"lastScrape\":\"0001-01-01T00:00:00Z\",\"lastError\":\"connection refused\"}," +
            "{\"labels\":{\"job\":\"services\",\"instance\":\"audit:8082\"},\"lastScrape\":\"illisible\",\"lastScrapeDuration\":\"x\"}]}}"
        );
        prometheus.json(
            "/api/v1/query",
            200,
            "{\"status\":\"success\",\"data\":{\"resultType\":\"vector\",\"result\":[{\"metric\":{\"instance\":\"node-exporter:9100\",\"name\":\"gimac-des-iam-1\"},\"value\":[1695980000.0,\"42.123\"]},{\"metric\":{},\"value\":[1,\"1\"]},{\"metric\":{\"instance\":\"h2\"},\"value\":[1]}]}}"
        );
        PrometheusWebClientAdapter adaptateur = new PrometheusWebClientAdapter(ClientsSupervision.webClient(DELAI, prometheus.url() + "/"));
        List<CibleCollecte> cibles = adaptateur.cibles().block();
        assertThat(cibles).extracting(CibleCollecte::job).containsExactly("node-exporter", "services", "services");
        assertThat(cibles.get(0).instance()).isEqualTo("http://node-exporter:9100/metrics");
        assertThat(cibles.get(0).derniereCollecte()).isNull();
        assertThat(cibles.get(0).erreur()).isEqualTo("connection refused");
        assertThat(cibles.get(1).instance()).isEqualTo("audit:8082");
        assertThat(cibles.get(1).etat()).isEqualTo("unknown");
        assertThat(cibles.get(1).derniereCollecte()).isNull();
        assertThat(cibles.get(1).dureeDerniereCollecteSecondes()).isNull();
        assertThat(cibles.get(2).derniereCollecte()).isEqualTo(Instant.parse("2026-09-29T09:00:00.123Z"));
        assertThat(cibles.get(2).dureeDerniereCollecteSecondes()).isEqualTo(0.012);
        assertThat(cibles.get(2).erreur()).isNull();

        List<EtatHote> hotes = adaptateur.hotes().block();
        assertThat(hotes).singleElement().satisfies(h -> {
            assertThat(h.instance()).isEqualTo("node-exporter:9100");
            assertThat(h.cpuPourcent()).isEqualTo(42.12);
            assertThat(h.chargeMoyenne1m()).isEqualTo(42.123);
        });
        List<EtatConteneur> conteneurs = adaptateur.conteneurs().block();
        assertThat(conteneurs).containsExactly(new EtatConteneur("gimac-des-iam-1", 42.123, 42.123));
        assertThat(prometheus.recues()).anySatisfy(r -> assertThat(r.requete()).contains("query=").contains("node_load1"));
    }

    @Test
    @DisplayName("Alertmanager : alertes actives, les plus récentes d'abord")
    void alertmanager() {
        alertmanager.json(
            "/api/v2/alerts",
            200,
            "[{\"labels\":{\"alertname\":\"InstanceDown\",\"severity\":\"page\",\"instance\":\"audit:8082\",\"job\":\"services\"},\"annotations\":{\"summary\":\"Instance audit:8082 down\",\"description\":\"arrêtée\"},\"startsAt\":\"2026-09-29T09:00:00Z\",\"status\":{\"state\":\"active\"}}," +
            "{\"labels\":{\"alertname\":\"Recente\"},\"startsAt\":\"2026-09-29T09:30:00Z\"},{\"labels\":{\"alertname\":\"SansDate\"}}]"
        );
        List<Alerte> alertes = new AlertmanagerWebClientAdapter(ClientsSupervision.webClient(DELAI, alertmanager.url())).alertesActives().block();
        assertThat(alertes).extracting(Alerte::nom).containsExactly("Recente", "InstanceDown", "SansDate");
        Alerte a = alertes.get(1);
        assertThat(a.severite()).isEqualTo("page");
        assertThat(a.resume()).isEqualTo("Instance audit:8082 down");
        assertThat(a.cible()).isEqualTo("audit:8082");
        assertThat(a.etat()).isEqualTo("active");
        assertThat(alertmanager.recues().get(0).requete()).contains("active=true").contains("silenced=false");
    }

    @Test
    @DisplayName("Routes : identifiant, uri, prédicats et filtres (ordonnés ou non), secrets masqués")
    void routes() {
        GatewayFilter relais = (e, c) -> c.filter(e);
        Route route = Route.async()
            .id("iam_route")
            .uri("http://u:secret@iam:8081")
            .predicate(e -> true)
            .filters(new OrderedGatewayFilter(relais, 1), relais)
            .order(3)
            .build();
        RouteVue vue = new RoutesPasserelleAdapter(() -> reactor.core.publisher.Flux.just(route)).routes().blockFirst();
        assertThat(vue.id()).isEqualTo("iam_route");
        assertThat(vue.uri()).isEqualTo("http://u:******@iam:8081");
        assertThat(vue.ordre()).isEqualTo(3);
        assertThat(vue.filtres()).hasSize(2).allSatisfy(f -> assertThat(f).isEqualTo(String.valueOf(relais)));
        assertThat(vue.predicats()).isNotBlank();
    }

    @Test
    @DisplayName("Jeton de service : valeur du client autorisé ; erreur ou absence de gestionnaire : vide")
    void jeton() {
        ReactiveOAuth2AuthorizedClientManager gestionnaire = mock(ReactiveOAuth2AuthorizedClientManager.class);
        ClientRegistration enregistrement = ClientRegistration.withRegistrationId(JetonServiceSupervision.ENREGISTREMENT)
            .clientId("gateway-supervision")
            .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
            .tokenUri("http://keycloak/token")
            .build();
        OAuth2AccessToken jeton = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "abc", Instant.now(), Instant.now().plusSeconds(60));
        when(gestionnaire.authorize(any())).thenReturn(Mono.just(new OAuth2AuthorizedClient(enregistrement, "gateway-supervision", jeton)));
        assertThat(new JetonServiceSupervision(gestionnaire).get().block()).isEqualTo("abc");

        when(gestionnaire.authorize(any())).thenReturn(Mono.error(new IllegalArgumentException("Could not find ClientRegistration")));
        JetonServiceSupervision enPanne = new JetonServiceSupervision(gestionnaire);
        assertThat(enPanne.get().block()).isNull();
        assertThat(enPanne.get().block()).isNull();
        assertThat(new JetonServiceSupervision(null).get().block()).isNull();
    }

    @Test
    @DisplayName("Outils : nombre, date, carte et liste tolérants ; client sans adresse de base")
    void outils() {
        assertThat(ClientsSupervision.nombre(null)).isNull();
        assertThat(ClientsSupervision.nombre(3)).isEqualTo(3.0);
        assertThat(ClientsSupervision.nombre("NaN")).isNull();
        assertThat(ClientsSupervision.nombre("x")).isNull();
        assertThat(ClientsSupervision.instant(null)).isNull();
        assertThat(ClientsSupervision.carte("x")).isEmpty();
        assertThat(ClientsSupervision.liste("x")).isEmpty();
        assertThat(ClientsSupervision.texte(null)).isNull();
        assertThat(ClientsSupervision.sansBarreFinale("http://a/")).isEqualTo("http://a");
        assertThat(ClientsSupervision.webClient(DELAI, " ")).isNotNull();
        assertThat(PrometheusWebClientAdapter.lireVecteur(Map.of(), "instance")).isEmpty();
    }
}
