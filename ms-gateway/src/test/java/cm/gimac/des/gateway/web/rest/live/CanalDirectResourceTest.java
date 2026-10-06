package cm.gimac.des.gateway.web.rest.live;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockOidcLogin;

import cm.gimac.des.gateway.config.SecurityConfiguration;
import cm.gimac.des.gateway.config.TestSecurityConfiguration;
import cm.gimac.des.gateway.domain.live.EvenementLive;
import cm.gimac.des.gateway.domain.live.TamponRejeu;
import cm.gimac.des.gateway.service.live.CanalDirectService;
import cm.gimac.des.gateway.web.filter.OAuth2ReactiveRefreshTokensWebFilter;
import java.time.Clock;
import java.time.Duration;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;
import tech.jhipster.config.JHipsterProperties;

/**
 * Canal poussé GET /bff/{couche}/live sur la vraie chaîne de sécurité de la passerelle : 401 sans
 * session, 403 sans second facteur, 404 pour une couche inconnue, 403 au portail sans institution ;
 * remise en SSE (id, données limitées à id, type, subject, topic) selon la couche, l'institution
 * et les destinataires ; reprise par « depuis » et Last-Event-ID.
 */
@WebFluxTest(
    controllers = CanalDirectResource.class,
    excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = OAuth2ReactiveRefreshTokensWebFilter.class)
)
@Import({ SecurityConfiguration.class, TestSecurityConfiguration.class, CanalDirectResourceTest.Canal.class })
class CanalDirectResourceTest {

    static final Duration DELAI = Duration.ofSeconds(10);
    static final ParameterizedTypeReference<ServerSentEvent<String>> SSE = new ParameterizedTypeReference<>() {};

    /** Source simulée : événements émis par le test. */
    @TestConfiguration(proxyBeanMethods = false)
    @EnableConfigurationProperties(JHipsterProperties.class)
    static class Canal {

        @Bean
        Sinks.Many<EvenementLive> sourceSimulee() {
            return Sinks.many().multicast().directBestEffort();
        }

        @Bean
        CanalDirectService canalDirectService(Sinks.Many<EvenementLive> source) {
            return new CanalDirectService(
                source::asFlux,
                new TamponRejeu(500, Duration.ofMinutes(5), Clock.systemUTC()),
                new CanalDirectService.Parametres(Duration.ofSeconds(25), Duration.ofMinutes(30), 64)
            );
        }
    }

    @Autowired
    WebTestClient client;

    @Autowired
    Sinks.Many<EvenementLive> source;

    @Autowired
    CanalDirectService canal;

    /** Connexions ouvertes avant la requête en cours. */
    int avant;

    WebTestClient session(String compte, String acr, String institution) {
        return client.mutateWith(
            mockOidcLogin().idToken(t -> {
                t.subject(compte).claim("preferred_username", compte);
                if (acr != null) {
                    t.claim("acr", acr);
                }
                if (institution != null) {
                    t.claim("institution_id", institution);
                }
            })
        );
    }

    Flux<ServerSentEvent<String>> ouvrir(WebTestClient c, String uri) {
        return ouvrir(c, uri, null);
    }

    Flux<ServerSentEvent<String>> ouvrir(WebTestClient c, String uri, String dernierId) {
        avant = canal.connexions();
        WebTestClient.RequestHeadersSpec<?> requete = c.get().uri(uri).accept(MediaType.TEXT_EVENT_STREAM);
        if (dernierId != null) {
            requete = requete.header("Last-Event-ID", dernierId);
        }
        return requete
            .exchange()
            .expectStatus()
            .isOk()
            .expectHeader()
            .contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM)
            .expectHeader()
            .valueEquals("X-Accel-Buffering", "no")
            .returnResult(SSE)
            .getResponseBody()
            .filter(e -> e.data() != null);
    }

    void emettre(EvenementLive... evenements) {
        // Laisse la connexion s'abonner à la diffusion avant l'émission.
        for (int i = 0; i < 250 && canal.connexions() <= avant; i++) {
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        for (EvenementLive e : evenements) {
            source.tryEmitNext(e);
        }
    }

    static EvenementLive message(String id, String... destinataires) {
        return new EvenementLive(id, "collaboration.message-sent.v1", "c-1", EvenementLive.COLLABORATION, null, Set.of(destinataires));
    }

    static EvenementLive homologation(String id, String institution) {
        return new EvenementLive(id, "homologation.liaison-opened.v1", "l-1", EvenementLive.HOMOLOGATION, institution, null);
    }

    @Test
    @DisplayName("401 sans session, sur chaque couche")
    void sansSession() {
        for (String couche : new String[] { "des", "portail", "console" }) {
            client.get().uri("/bff/" + couche + "/live").accept(MediaType.TEXT_EVENT_STREAM).exchange().expectStatus().isUnauthorized();
        }
    }

    @Test
    @DisplayName("403 sans second facteur (acr 1 ou absent)")
    void sansDoubleFacteur() {
        session("u1", "1", null).get().uri("/bff/des/live").accept(MediaType.TEXT_EVENT_STREAM).exchange().expectStatus().isForbidden();
        session("u1", null, null).get().uri("/bff/des/live").accept(MediaType.TEXT_EVENT_STREAM).exchange().expectStatus().isForbidden();
    }

    @Test
    @DisplayName("404 pour une couche inconnue ; 403 au portail sans institution")
    void coucheEtInstitution() {
        session("u1", "2", null).get().uri("/bff/autre/live").accept(MediaType.TEXT_EVENT_STREAM).exchange().expectStatus().isNotFound();
        session("u1", "2", null).get().uri("/bff/portail/live").accept(MediaType.TEXT_EVENT_STREAM).exchange().expectStatus().isForbidden();
    }

    @Test
    @DisplayName("DES : message remis à son destinataire, données limitées à id, type, subject, topic et acteur")
    void destinataire() {
        StepVerifier.create(ouvrir(session("u1", "2", null), "/bff/des/live?sujets=collaboration,homologation"))
            .then(() -> emettre(message("m-1", "u2"), message("m-2", "u1", "u2")))
            .assertNext(e -> {
                assertThat(e.id()).isEqualTo("m-2");
                assertThat(e.data()).isEqualTo(
                    "{\"id\":\"m-2\",\"type\":\"collaboration.message-sent.v1\",\"subject\":\"c-1\",\"topic\":\"collaboration\",\"acteur\":null}"
                );
            })
            .thenCancel()
            .verify(DELAI);
    }

    @Test
    @DisplayName("Sécurité : un autre utilisateur ne reçoit pas un message qui ne lui est pas adressé")
    void autreUtilisateur() {
        StepVerifier.create(ouvrir(session("u3", "2", null), "/bff/des/live?sujets=collaboration,homologation"))
            .then(() -> emettre(message("m-1", "u1", "u2"), homologation("h-1", null)))
            .assertNext(e -> assertThat(e.id()).isEqualTo("h-1"))
            .thenCancel()
            .verify(DELAI);
    }

    @Test
    @DisplayName("Sécurité : le portail ne reçoit jamais la collaboration, ni les événements d'une autre institution")
    void portail() {
        StepVerifier.create(ouvrir(session("u1", "3", "INST-0042"), "/bff/portail/live?sujets=collaboration,homologation"))
            .then(() -> emettre(message("m-1", "u1"), homologation("h-1", "INST-0099"), homologation("h-2", "INST-0042")))
            .assertNext(e -> {
                assertThat(e.id()).isEqualTo("h-2");
                assertThat(e.data()).contains("\"topic\":\"homologation\"").doesNotContain("INST-0042");
            })
            .thenCancel()
            .verify(DELAI);
    }

    @Test
    @DisplayName("Reprise : « depuis » puis Last-Event-ID, prioritaire")
    void reprise() {
        source.tryEmitNext(homologation("r-1", null));
        source.tryEmitNext(homologation("r-2", null));
        source.tryEmitNext(homologation("r-3", null));
        StepVerifier.create(ouvrir(session("u1", "2", null), "/bff/console/live?sujets=homologation&depuis=r-1"))
            .assertNext(e -> assertThat(e.id()).isEqualTo("r-2"))
            .assertNext(e -> assertThat(e.id()).isEqualTo("r-3"))
            .thenCancel()
            .verify(DELAI);
        StepVerifier.create(ouvrir(session("u1", "2", null), "/bff/console/live?depuis=r-1", "r-2"))
            .assertNext(e -> assertThat(e.id()).isEqualTo("r-3"))
            .thenCancel()
            .verify(DELAI);
    }

    @Test
    @DisplayName("Présence (D-72) : 401 sans session, 403 sans second facteur, 404 au portail ou couche inconnue")
    void presenceSecurite() {
        client.get().uri("/bff/des/presence").exchange().expectStatus().isUnauthorized();
        session("u1", "1", null).get().uri("/bff/des/presence").exchange().expectStatus().isForbidden();
        session("u1", "3", "INST-0042").get().uri("/bff/portail/presence").exchange().expectStatus().isNotFound();
        session("u1", "2", null).get().uri("/bff/autre/presence").exchange().expectStatus().isNotFound();
    }

    @Test
    @DisplayName("Présence (D-72) : le compte connecté au canal figure parmi les comptes en ligne ; annonce sans identifiant")
    void presence() {
        StepVerifier.create(ouvrir(session("u7", "2", null), "/bff/des/live"))
            .then(() -> {
                emettre();
                session("u8", "2", null)
                    .get()
                    .uri("/bff/console/presence")
                    .exchange()
                    .expectStatus()
                    .isOk()
                    .expectBody()
                    .jsonPath("$.enLigne")
                    .value(v -> assertThat(v.toString()).contains("u7"));
                emettre(
                    new EvenementLive("p-1", EvenementLive.EN_LIGNE, "u9", EvenementLive.PRESENCE, null, null, "u9")
                );
            })
            .assertNext(e -> {
                assertThat(e.id()).isNull();
                assertThat(e.data()).contains("\"acteur\":\"u9\"").contains("\"topic\":\"presence\"");
            })
            .thenCancel()
            .verify(DELAI);
    }

    @Test
    @DisplayName("Accusé de lecture (D-88) : remis au destinataire avec identifiant de reprise, lecteur et luJusquA ; pas au lecteur")
    void accuseDeLecture() {
        EvenementLive l = new EvenementLive("lu-1", EvenementLive.LECTURE, "c-1", EvenementLive.COLLABORATION, null, Set.of("u1"), "u2", "2026-10-06T08:00:00Z");
        StepVerifier.create(ouvrir(session("u1", "2", null), "/bff/des/live?sujets=collaboration"))
            .then(() -> emettre(l))
            .assertNext(e -> {
                assertThat(e.id()).isEqualTo("lu-1");
                assertThat(e.data()).isEqualTo(
                    "{\"id\":\"lu-1\",\"type\":\"collaboration.conversation-read.v1\",\"subject\":\"c-1\",\"topic\":\"collaboration\"," +
                    "\"acteur\":\"u2\",\"luJusquA\":\"2026-10-06T08:00:00Z\"}"
                );
            })
            .thenCancel()
            .verify(DELAI);
        StepVerifier.create(ouvrir(session("u2", "2", null), "/bff/des/live?sujets=collaboration,homologation"))
            .then(() -> emettre(l, homologation("h-lu", null)))
            .assertNext(e -> assertThat(e.id()).isEqualTo("h-lu"))
            .thenCancel()
            .verify(DELAI);
    }
}
