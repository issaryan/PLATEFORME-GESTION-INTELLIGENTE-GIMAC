package cm.gimac.des.gateway.config;

import cm.gimac.des.gateway.domain.live.TamponRejeu;
import cm.gimac.des.gateway.domain.live.port.SourceEvenements;
import cm.gimac.des.gateway.infrastructure.live.KafkaSourceEvenements;
import cm.gimac.des.gateway.infrastructure.live.LectureCloudEvent;
import cm.gimac.des.gateway.service.live.CanalDirectService;
import java.time.Clock;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Flux;

/**
 * Assemblage du canal poussé (/bff/{couche}/live) : source Kafka si gimac.live.actif (défaut),
 * source vide sinon (tests, poste sans courtier), service et tampon de reprise.
 */
@Configuration
@EnableConfigurationProperties(LiveProperties.class)
public class LiveConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "gimac.live", name = "actif", havingValue = "true", matchIfMissing = true)
    public KafkaSourceEvenements kafkaSourceEvenements(LiveProperties proprietes) {
        return new KafkaSourceEvenements(proprietesConsommateur(proprietes), proprietes.getSujets(), new LectureCloudEvent());
    }

    @Bean
    @ConditionalOnProperty(prefix = "gimac.live", name = "actif", havingValue = "false")
    public SourceEvenements sourceEvenementsInactive() {
        return Flux::never;
    }

    @Bean(destroyMethod = "close")
    public CanalDirectService canalDirectService(SourceEvenements source, LiveProperties p) {
        return new CanalDirectService(
            source,
            new TamponRejeu(p.getTailleTampon(), p.getDureeTampon(), Clock.systemUTC()),
            new CanalDirectService.Parametres(p.getBattement(), p.getDureeMaxConnexion(), p.getTamponConnexion())
        );
    }

    /** Courtiers et propriétés supplémentaires ; reconnexion espacée quand le courtier est absent. */
    static Map<String, Object> proprietesConsommateur(LiveProperties p) {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, p.getBootstrapServers());
        config.put(ConsumerConfig.RECONNECT_BACKOFF_MAX_MS_CONFIG, 10_000);
        config.putAll(p.getKafka());
        return config;
    }
}
