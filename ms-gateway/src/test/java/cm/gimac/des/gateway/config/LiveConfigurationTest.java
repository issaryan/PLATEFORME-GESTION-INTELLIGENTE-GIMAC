package cm.gimac.des.gateway.config;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.gateway.domain.live.port.SourceEvenements;
import cm.gimac.des.gateway.infrastructure.live.KafkaSourceEvenements;
import cm.gimac.des.gateway.service.live.CanalDirectService;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import reactor.test.StepVerifier;

/** Assemblage du canal poussé : source Kafka ou source vide selon gimac.live.actif. */
class LiveConfigurationTest {

    final ApplicationContextRunner contexte = new ApplicationContextRunner().withUserConfiguration(LiveConfiguration.class);

    @Test
    @DisplayName("Inactif : source vide, canal servi")
    void inactif() {
        contexte
            .withPropertyValues("gimac.live.actif=false")
            .run(ctx -> {
                assertThat(ctx).hasSingleBean(CanalDirectService.class).doesNotHaveBean(KafkaSourceEvenements.class);
                StepVerifier.create(ctx.getBean(SourceEvenements.class).evenements()).expectSubscription().expectNoEvent(Duration.ofMillis(50)).thenCancel().verify();
            });
    }

    @Test
    // Vrai client Kafka démarré puis arrêté avec le contexte : chargement des classes du client (JVM froide,
    // agent JaCoCo) et arrêt du consommateur dépassaient le délai par défaut de 15 s sur une machine chargée.
    @org.junit.jupiter.api.Timeout(60)
    @DisplayName("Actif par défaut : consommateur Kafka démarré avec le contexte, courtier injoignable sans blocage")
    void actif() {
        contexte
            .withPropertyValues("gimac.live.bootstrap-servers=localhost:1", "gimac.live.sujets=homologation.liaison")
            .run(ctx -> {
                assertThat(ctx).hasSingleBean(KafkaSourceEvenements.class).hasSingleBean(CanalDirectService.class);
                assertThat(ctx.getBean(KafkaSourceEvenements.class).isRunning()).isTrue();
            });
    }

    @Test
    @DisplayName("Propriétés : valeurs par défaut, courtiers et propriétés supplémentaires du consommateur")
    void proprietes() {
        LiveProperties p = new LiveProperties();
        assertThat(p.isActif()).isTrue();
        assertThat(p.getSujets()).contains("homologation.liaison", "certification.quitus", "collaboration.message", "notification.inapp");
        assertThat(p.getTailleTampon()).isEqualTo(500);
        assertThat(p.getDureeTampon()).isEqualTo(Duration.ofMinutes(5));
        assertThat(p.getBattement()).isEqualTo(Duration.ofSeconds(25));
        assertThat(p.getDureeMaxConnexion()).isEqualTo(Duration.ofMinutes(30));
        assertThat(p.getTamponConnexion()).isEqualTo(256);
        p.setActif(false);
        p.setBootstrapServers("kafka:9092");
        p.setSujets(List.of("collaboration.message"));
        p.setKafka(Map.of("security.protocol", "SSL"));
        p.setTailleTampon(10);
        p.setDureeTampon(Duration.ofMinutes(1));
        p.setBattement(Duration.ofSeconds(5));
        p.setDureeMaxConnexion(Duration.ofMinutes(2));
        p.setTamponConnexion(8);
        assertThat(p.isActif()).isFalse();
        assertThat(p.getSujets()).containsExactly("collaboration.message");
        assertThat(p.getTailleTampon()).isEqualTo(10);
        assertThat(p.getDureeTampon()).isEqualTo(Duration.ofMinutes(1));
        assertThat(p.getBattement()).isEqualTo(Duration.ofSeconds(5));
        assertThat(p.getDureeMaxConnexion()).isEqualTo(Duration.ofMinutes(2));
        assertThat(p.getTamponConnexion()).isEqualTo(8);
        Map<String, Object> config = LiveConfiguration.proprietesConsommateur(p);
        assertThat(config).containsEntry(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "kafka:9092").containsEntry("security.protocol", "SSL");
        assertThat(p.getKafka()).containsEntry("security.protocol", "SSL");
        assertThat(p.getBootstrapServers()).isEqualTo("kafka:9092");
    }
}
