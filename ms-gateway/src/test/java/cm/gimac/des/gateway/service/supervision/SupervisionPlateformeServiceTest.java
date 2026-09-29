package cm.gimac.des.gateway.service.supervision;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.gateway.domain.supervision.Alerte;
import cm.gimac.des.gateway.domain.supervision.ChangementJournalisation;
import cm.gimac.des.gateway.domain.supervision.CibleCollecte;
import cm.gimac.des.gateway.domain.supervision.ConfigurationPlateforme;
import cm.gimac.des.gateway.domain.supervision.EnvironnementService;
import cm.gimac.des.gateway.domain.supervision.EtatAlertes;
import cm.gimac.des.gateway.domain.supervision.EtatConteneur;
import cm.gimac.des.gateway.domain.supervision.EtatGlobal;
import cm.gimac.des.gateway.domain.supervision.EtatHote;
import cm.gimac.des.gateway.domain.supervision.EtatSante;
import cm.gimac.des.gateway.domain.supervision.EtatServeurs;
import cm.gimac.des.gateway.domain.supervision.EtatService;
import cm.gimac.des.gateway.domain.supervision.ImpulsionPlateforme;
import cm.gimac.des.gateway.domain.supervision.InfoService;
import cm.gimac.des.gateway.domain.supervision.InstantanePlateforme;
import cm.gimac.des.gateway.domain.supervision.Metriques;
import cm.gimac.des.gateway.domain.supervision.NiveauLogger;
import cm.gimac.des.gateway.domain.supervision.NiveauxJournalisation;
import cm.gimac.des.gateway.domain.supervision.RouteVue;
import cm.gimac.des.gateway.domain.supervision.Sante;
import cm.gimac.des.gateway.domain.supervision.ServiceCible;
import cm.gimac.des.gateway.domain.supervision.ServiceInconnuException;
import cm.gimac.des.gateway.domain.supervision.port.SondeService;
import cm.gimac.des.gateway.domain.supervision.port.SourceAlertes;
import cm.gimac.des.gateway.domain.supervision.port.SourceRoutes;
import cm.gimac.des.gateway.domain.supervision.port.SourceServeurs;
import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * Service d'application de la supervision (D-61), ports simulés : appels parallèles, délai,
 * injoignable, agrégation, masquage de la configuration, journalisation et flux.
 */
class SupervisionPlateformeServiceTest {

    static final Instant T0 = Instant.parse("2026-09-29T10:00:00Z");
    static final Clock HORLOGE = Clock.fixed(T0, ZoneOffset.UTC);
    static final ServiceCible IAM = new ServiceCible("iam", "http://iam:8081");
    static final ServiceCible LENT = new ServiceCible("lent", "http://lent:1");
    static final ServiceCible ARRETE = new ServiceCible("arrete", "http://arrete:1");
    static final Metriques METRIQUES = new Metriques(10.0, 0.1, 0.2, 4, 1.0, 2.0, 50.0, 3.0, 4.0, 0.5, 20);

    /** Sonde simulée : IAM répond, LENT ne répond jamais, ARRETE est en erreur de connexion. */
    static class SondeSimulee implements SondeService {

        final List<String> modifications = new ArrayList<>();
        boolean infoEnErreur;
        boolean santeVide;

        Mono<Sante> selon(ServiceCible s, Mono<Sante> ok) {
            return switch (s.nom()) {
                case "lent" -> Mono.never();
                case "arrete" -> Mono.error(new IOException("Connection refused: arrete/10.0.0.9:1"));
                default -> santeVide ? Mono.empty() : ok;
            };
        }

        @Override
        public Mono<Sante> sante(ServiceCible s) {
            return selon(s, Mono.just(new Sante(EtatSante.UP, Map.of("db", "UP", "kafka", "UP"))));
        }

        @Override
        public Mono<Sante> santeExterne(String url) {
            return url.contains("panne") ? Mono.error(new IOException("refus")) : Mono.just(new Sante(EtatSante.UP, Map.of()));
        }

        @Override
        public Mono<InfoService> info(ServiceCible s) {
            return infoEnErreur ? Mono.error(new IllegalStateException("500")) : Mono.just(new InfoService("1.0.0", "iam", null, "abc123", "main"));
        }

        @Override
        public Mono<Metriques> metriques(ServiceCible s) {
            return Mono.just(METRIQUES);
        }

        @Override
        public Mono<EnvironnementService> environnement(ServiceCible s) {
            if (s.nom().equals("arrete")) {
                return Mono.error(new IOException("refus"));
            }
            if (s.nom().equals("lent")) {
                return Mono.empty();
            }
            return Mono.just(
                new EnvironnementService(
                    List.of("prod"),
                    Map.of("gimac.opa.url", "http://opa:8181", "spring.datasource.password", "abc", "spring.datasource.url", "jdbc:postgresql://db/iam?password=x", "java.home", "/opt")
                )
            );
        }

        @Override
        public Mono<NiveauxJournalisation> journalisation(ServiceCible s) {
            return Mono.just(
                new NiveauxJournalisation(
                    s.nom(),
                    List.of("OFF", "ERROR", "WARN", "INFO", "DEBUG", "TRACE"),
                    List.of(new NiveauLogger("ROOT", "INFO", "INFO"), new NiveauLogger("cm.gimac.des", null, "INFO"), new NiveauLogger("org.hibernate", null, "WARN"))
                )
            );
        }

        @Override
        public Mono<Void> modifierNiveau(ServiceCible s, String logger, String niveau) {
            return Mono.fromRunnable(() -> modifications.add(s.nom() + ":" + logger + ":" + niveau));
        }
    }

    static SourceServeurs serveurs(boolean enPanne) {
        return new SourceServeurs() {
            @Override
            public Mono<List<CibleCollecte>> cibles() {
                return enPanne
                    ? Mono.error(new IOException("Connection refused"))
                    : Mono.just(List.of(new CibleCollecte("services", "iam:8081", "up", T0, 0.01, null), new CibleCollecte("services", "audit:8082", "down", null, null, "refus")));
            }

            @Override
            public Mono<List<EtatHote>> hotes() {
                return Mono.just(List.of(new EtatHote("node-exporter:9100", 12.5, 40.0, 8.0e9, 55.0, 0.7)));
            }

            @Override
            public Mono<List<EtatConteneur>> conteneurs() {
                return Mono.error(new IOException("cAdvisor absent"));
            }
        };
    }

    static SourceAlertes alertes(boolean enPanne) {
        return () ->
            enPanne ? Mono.error(new IOException("refus")) : Mono.just(List.of(new Alerte("InstanceDown", "page", "audit arrêté", null, T0, "audit:8082", "services", "active")));
    }

    static final SourceRoutes ROUTES = () -> Flux.just(new RouteVue("iam_route", "http://iam:8081", "Paths: [/services/iam/**]", List.of("[StripPrefix parts = 2]"), 0));

    static SupervisionPlateformeService service(SondeSimulee sonde, List<ServiceCible> cibles, String keycloak, boolean prometheusEnPanne, boolean alertmanagerEnPanne) {
        return new SupervisionPlateformeService(
            new SupervisionPlateformeService.Parametres(cibles, Duration.ofMillis(300), Duration.ofMillis(50), keycloak),
            sonde,
            serveurs(prometheusEnPanne),
            alertes(alertmanagerEnPanne),
            ROUTES,
            HORLOGE
        );
    }

    @Test
    @DisplayName("Services interrogés en parallèle : le lent et l'arrêté sont injoignables sans retarder les autres")
    void instantane() {
        SondeSimulee sonde = new SondeSimulee();
        SupervisionPlateformeService s = service(sonde, List.of(IAM, LENT, ARRETE), "http://keycloak:9990/health/ready", false, false);
        long debut = System.nanoTime();
        InstantanePlateforme i = s.instantane().block();
        // Trois services dont un qui ne répond jamais : borné par un seul délai (300 ms), pas par la somme.
        assertThat(Duration.ofNanos(System.nanoTime() - debut)).isLessThan(Duration.ofMillis(1500));
        assertThat(i.horodatage()).isEqualTo(T0);
        assertThat(i.services()).extracting(EtatService::nom).containsExactly("iam", "lent", "arrete");
        EtatService iam = i.services().get(0);
        assertThat(iam.etat()).isEqualTo(EtatSante.UP);
        assertThat(iam.latenceMs()).isNotNull().isGreaterThanOrEqualTo(0);
        assertThat(iam.composants()).containsEntry("db", "UP").containsEntry("kafka", "UP");
        assertThat(iam.info().version()).isEqualTo("1.0.0");
        assertThat(iam.metriques()).isEqualTo(METRIQUES);
        assertThat(i.services().get(1).etat()).isEqualTo(EtatSante.INJOIGNABLE);
        assertThat(i.services().get(1).erreur()).isEqualTo("délai dépassé");
        assertThat(i.services().get(2).etat()).isEqualTo(EtatSante.INJOIGNABLE);
        assertThat(i.services().get(2).erreur()).contains("Connection refused");
        assertThat(i.global()).isEqualTo(new EtatGlobal(EtatGlobal.DEGRADEE, 3, 1, 2, 2));
        assertThat(i.keycloak().etat()).isEqualTo(EtatSante.UP);
    }

    @Test
    @DisplayName("Info en erreur : service UP avec info vide ; Keycloak en panne : injoignable ; sans adresse : absent")
    void degradations() {
        SondeSimulee sonde = new SondeSimulee();
        sonde.infoEnErreur = true;
        InstantanePlateforme i = service(sonde, List.of(IAM), "http://panne:9990/health/ready", false, false).instantane().block();
        assertThat(i.services().get(0).etat()).isEqualTo(EtatSante.UP);
        assertThat(i.services().get(0).info()).isEqualTo(InfoService.VIDE);
        assertThat(i.keycloak().etat()).isEqualTo(EtatSante.INJOIGNABLE);
        assertThat(service(sonde, List.of(IAM), "", false, false).instantane().block().keycloak()).isNull();
        assertThat(service(sonde, List.of(IAM), null, false, false).instantane().block().global().etat()).isEqualTo(EtatGlobal.OPERATIONNELLE);
        sonde.santeVide = true;
        assertThat(service(sonde, List.of(IAM), null, false, false).instantane().block().services().get(0).erreur()).isEqualTo("réponse vide");
    }

    @Test
    @DisplayName("Serveurs : cibles comptées, node-exporter détecté, conteneurs absents tolérés ; Prometheus en panne signalé")
    void etatServeurs() {
        EtatServeurs e = service(new SondeSimulee(), List.of(IAM), null, false, false).etatServeurs().block();
        assertThat(e.prometheusJoignable()).isTrue();
        assertThat(e.ciblesUp()).isEqualTo(1);
        assertThat(e.ciblesDown()).isEqualTo(1);
        assertThat(e.nodeExporter()).isTrue();
        assertThat(e.hotes()).hasSize(1);
        assertThat(e.conteneurs()).isEmpty();
        EtatServeurs panne = service(new SondeSimulee(), List.of(IAM), null, true, false).etatServeurs().block();
        assertThat(panne.prometheusJoignable()).isFalse();
        assertThat(panne.erreur()).startsWith("Prometheus injoignable");
        assertThat(panne.cibles()).isEmpty();
    }

    @Test
    @DisplayName("Alertes actives ; Alertmanager en panne signalé")
    void etatAlertes() {
        EtatAlertes a = service(new SondeSimulee(), List.of(IAM), null, false, false).etatAlertes().block();
        assertThat(a.joignable()).isTrue();
        assertThat(a.nombre()).isEqualTo(1);
        assertThat(a.alertes().get(0).nom()).isEqualTo("InstanceDown");
        EtatAlertes panne = service(new SondeSimulee(), List.of(IAM), null, false, true).etatAlertes().block();
        assertThat(panne.joignable()).isFalse();
        assertThat(panne.erreur()).startsWith("Alertmanager injoignable");
    }

    @Test
    @DisplayName("Configuration : routes, propriétés retenues et masquées ; service injoignable ou muet signalé")
    void configuration() {
        ConfigurationPlateforme c = service(new SondeSimulee(), List.of(IAM, ARRETE, LENT), null, false, false).configuration().block();
        assertThat(c.routes()).extracting(RouteVue::id).containsExactly("iam_route");
        assertThat(c.services()).hasSize(3);
        var iam = c.services().get(0);
        assertThat(iam.lue()).isTrue();
        assertThat(iam.profilsActifs()).containsExactly("prod");
        assertThat(iam.proprietes())
            .containsEntry("gimac.opa.url", "http://opa:8181")
            .containsEntry("spring.datasource.url", "jdbc:postgresql://db/iam?password=******")
            .doesNotContainKeys("spring.datasource.password", "java.home");
        assertThat(c.services().get(1).lue()).isFalse();
        assertThat(c.services().get(1).erreur()).isEqualTo("refus");
        assertThat(c.services().get(2).erreur()).isEqualTo("réponse vide");
    }

    @Test
    @DisplayName("Journalisation : lecture filtrée par préfixe, service inconnu refusé")
    void journalisation() {
        SupervisionPlateformeService s = service(new SondeSimulee(), List.of(IAM), null, false, false);
        assertThat(s.journalisation("iam", null).block().loggers()).hasSize(3);
        assertThat(s.journalisation("iam", " ").block().loggers()).hasSize(3);
        assertThat(s.journalisation("iam", "cm.gimac").block().loggers()).extracting(NiveauLogger::nom).containsExactly("cm.gimac.des");
        StepVerifier.create(s.journalisation("inconnu", null)).expectError(ServiceInconnuException.class).verify();
        assertThat(s.services()).containsExactly(IAM);
    }

    @Test
    @DisplayName("Changement de niveau : appliqué, tracé avec auteur et motif ; NULL rétablit l'héritage")
    void modifier() {
        SondeSimulee sonde = new SondeSimulee();
        SupervisionPlateformeService s = service(sonde, List.of(IAM), null, false, false);
        ChangementJournalisation c = s.modifierJournalisation("iam", "cm.gimac.des", "debug", "Analyse de l'incident INC-42", "super-admin").block();
        assertThat(c).isEqualTo(new ChangementJournalisation("iam", "cm.gimac.des", "DEBUG", "Analyse de l'incident INC-42", "super-admin", T0));
        s.modifierJournalisation("iam", "ROOT", "null", "Retour au niveau hérité", "super-admin").block();
        assertThat(sonde.modifications).containsExactly("iam:cm.gimac.des:DEBUG", "iam:ROOT:null");
    }

    @Test
    @DisplayName("Changement de niveau refusé : logger, niveau ou motif invalides ; service inconnu")
    void modifierRefus() {
        SondeSimulee sonde = new SondeSimulee();
        SupervisionPlateformeService s = service(sonde, List.of(IAM), null, false, false);
        StepVerifier.create(s.modifierJournalisation("iam", "a b", "INFO", "motif valable", "u")).expectError(IllegalArgumentException.class).verify();
        StepVerifier.create(s.modifierJournalisation("iam", null, "INFO", "motif valable", "u")).expectError(IllegalArgumentException.class).verify();
        StepVerifier.create(s.modifierJournalisation("iam", "ROOT", "FATAL", "motif valable", "u")).expectError(IllegalArgumentException.class).verify();
        StepVerifier.create(s.modifierJournalisation("iam", "ROOT", null, "motif valable", "u")).expectError(IllegalArgumentException.class).verify();
        StepVerifier.create(s.modifierJournalisation("iam", "ROOT", "INFO", "  ab  ", "u")).expectError(IllegalArgumentException.class).verify();
        StepVerifier.create(s.modifierJournalisation("iam", "ROOT", "INFO", null, "u")).expectError(IllegalArgumentException.class).verify();
        StepVerifier.create(s.modifierJournalisation("iam", "ROOT", "INFO", "x".repeat(501), "u")).expectError(IllegalArgumentException.class).verify();
        StepVerifier.create(s.modifierJournalisation("inconnu", "ROOT", "INFO", "motif valable", "u")).expectError(ServiceInconnuException.class).verify();
        assertThat(sonde.modifications).isEmpty();
    }

    @Test
    @DisplayName("Flux temps réel : impulsions périodiques numérotées, -1 alerte si Alertmanager est injoignable")
    void flux() {
        List<ImpulsionPlateforme> impulsions = service(new SondeSimulee(), List.of(IAM), null, false, false).flux().take(2).collectList().block(Duration.ofSeconds(5));
        assertThat(impulsions).extracting(ImpulsionPlateforme::sequence).containsExactly(0L, 1L);
        assertThat(impulsions.get(0).nombreAlertes()).isEqualTo(1);
        assertThat(impulsions.get(0).instantane().global().up()).isEqualTo(1);
        ImpulsionPlateforme panne = service(new SondeSimulee(), List.of(IAM), null, false, true).flux().blockFirst(Duration.ofSeconds(5));
        assertThat(panne.nombreAlertes()).isEqualTo(-1);
    }

    @Test
    @DisplayName("Cause d'erreur : délai, message masqué, classe à défaut de message")
    void cause() {
        assertThat(SupervisionPlateformeService.cause(new TimeoutException())).isEqualTo("délai dépassé");
        assertThat(SupervisionPlateformeService.cause(new IOException("http://u:secret@h/"))).isEqualTo("http://u:******@h/");
        assertThat(SupervisionPlateformeService.cause(new IllegalStateException())).isEqualTo("IllegalStateException");
        assertThat(SupervisionPlateformeService.cause(new IllegalStateException(" "))).isEqualTo("IllegalStateException");
    }
}
