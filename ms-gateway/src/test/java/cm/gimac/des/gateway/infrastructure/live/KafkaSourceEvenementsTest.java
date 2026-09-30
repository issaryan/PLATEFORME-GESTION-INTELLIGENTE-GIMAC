package cm.gimac.des.gateway.infrastructure.live;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import cm.gimac.des.gateway.domain.live.EvenementLive;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

/** Source Kafka : lecture et diffusion des enregistrements, cycle de vie du consommateur. */
class KafkaSourceEvenementsTest {

    static final Map<String, Object> INJOIGNABLE = Map.of(
        ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
        "localhost:1",
        ConsumerConfig.RECONNECT_BACKOFF_MAX_MS_CONFIG,
        10_000
    );

    @Test
    @DisplayName("Enregistrement lu et diffusé ; valeur inexploitable ou erreur de lecture ignorée")
    void recevoir() {
        KafkaSourceEvenements source = new KafkaSourceEvenements(INJOIGNABLE, List.of("homologation.liaison"), new LectureCloudEvent());
        StepVerifier.create(source.evenements())
            .then(() -> {
                source.recevoir(new ConsumerRecord<>("homologation.liaison", 0, 0, "l-1", "pas du json"));
                source.recevoir(new ConsumerRecord<>("homologation.liaison", 0, 1, "l-1", "{\"id\":\"e-1\",\"type\":\"t\",\"subject\":\"l-1\"}"));
            })
            .assertNext(e -> assertThat(e.id()).isEqualTo("e-1"))
            .thenCancel()
            .verify(Duration.ofSeconds(5));

        LectureCloudEvent defaillante = mock(LectureCloudEvent.class);
        when(defaillante.lire(any(), any())).thenThrow(new IllegalStateException("boum"));
        KafkaSourceEvenements autre = new KafkaSourceEvenements(INJOIGNABLE, List.of("x"), defaillante);
        autre.recevoir(new ConsumerRecord<>("x", 0, 0, "k", "v"));
    }

    @Test
    @DisplayName("Groupe propre à l'instance ; courtier injoignable : démarrage non bloquant, arrêt propre")
    void cycleDeVie() {
        KafkaSourceEvenements source = new KafkaSourceEvenements(INJOIGNABLE, List.of("homologation.liaison"), new LectureCloudEvent());
        KafkaSourceEvenements autre = new KafkaSourceEvenements(INJOIGNABLE, List.of("homologation.liaison"), new LectureCloudEvent());
        assertThat(source.groupe()).startsWith(KafkaSourceEvenements.PREFIXE_GROUPE).isNotEqualTo(autre.groupe());
        assertThat(source.isRunning()).isFalse();
        assertThat(source.partitionsAttribuees()).isZero();
        source.start();
        assertThat(source.isRunning()).isTrue();
        source.stop();
        assertThat(source.isRunning()).isFalse();
        StepVerifier.create(source.evenements()).expectComplete().verify(Duration.ofSeconds(5));
        assertThat(EvenementLive.HOMOLOGATION).isEqualTo("homologation");
    }
}
