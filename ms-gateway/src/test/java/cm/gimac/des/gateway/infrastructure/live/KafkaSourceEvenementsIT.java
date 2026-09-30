package cm.gimac.des.gateway.infrastructure.live;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.gateway.domain.live.Abonne;
import cm.gimac.des.gateway.domain.live.Couche;
import cm.gimac.des.gateway.domain.live.TamponRejeu;
import cm.gimac.des.gateway.domain.live.VueEvenement;
import cm.gimac.des.gateway.service.live.CanalDirectService;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.codec.ServerSentEvent;
import org.testcontainers.kafka.KafkaContainer;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

/**
 * Canal poussé de bout en bout sur un vrai courtier Kafka (Testcontainers) : enveloppes
 * CloudEvents publiées sur les sujets du lot 1, lues par le consommateur de l'instance et remises
 * aux seules connexions autorisées ; aucune donnée métier relayée.
 */
class KafkaSourceEvenementsIT {

    static final KafkaContainer KAFKA = new KafkaContainer("apache/kafka-native:4.3.1")
        .withStartupTimeout(Duration.ofMinutes(2))
        .withStartupAttempts(3)
        .withEnv("KAFKA_LISTENERS", "PLAINTEXT://:9092,BROKER://:9093,CONTROLLER://:9094");

    static final List<String> SUJETS = List.of("homologation.liaison", "certification.quitus", "collaboration.message");

    static KafkaSourceEvenements source;
    static CanalDirectService canal;
    static KafkaProducer<String, String> producteur;

    @BeforeAll
    static void demarrer() throws Exception {
        KAFKA.start();
        try (Admin admin = Admin.create(Map.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers()))) {
            admin.createTopics(SUJETS.stream().map(s -> new NewTopic(s, 2, (short) 1)).toList()).all().get();
        }
        source = new KafkaSourceEvenements(
            Map.of(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers()),
            SUJETS,
            new LectureCloudEvent()
        );
        canal = new CanalDirectService(
            source,
            new TamponRejeu(500, Duration.ofMinutes(5), Clock.systemUTC()),
            new CanalDirectService.Parametres(Duration.ofSeconds(25), Duration.ofMinutes(5), 64)
        );
        source.start();
        // Positionnement « latest » : attendre l'attribution des partitions avant de publier.
        long limite = System.currentTimeMillis() + 60_000;
        while (source.partitionsAttribuees() < SUJETS.size() * 2 && System.currentTimeMillis() < limite) {
            Thread.sleep(200);
        }
        assertThat(source.partitionsAttribuees()).isEqualTo(SUJETS.size() * 2);
        Properties p = new Properties();
        p.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers());
        p.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        p.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producteur = new KafkaProducer<>(p);
    }

    @AfterAll
    static void arreter() {
        if (producteur != null) {
            producteur.close();
        }
        if (canal != null) {
            canal.close();
        }
        if (source != null) {
            source.stop();
        }
        KAFKA.stop();
    }

    static void publier(String sujet, String cle, String valeur) {
        producteur.send(new ProducerRecord<>(sujet, cle, valeur));
        producteur.flush();
    }

    static Flux<VueEvenement> evenements(Abonne abonne) {
        return canal.ouvrir(abonne, null).filter(e -> e.data() != null).map(ServerSentEvent::data);
    }

    @Test
    @DisplayName("Message de la messagerie : remis au seul destinataire du DES, sans contenu")
    void message() {
        Flux<VueEvenement> destinataire = evenements(Abonne.de(Couche.DES, "u1", "", "collaboration,homologation"));
        Flux<VueEvenement> autre = evenements(Abonne.de(Couche.DES, "u3", "", "collaboration,homologation"));
        StepVerifier.create(Flux.merge(destinataire.map(v -> "u1:" + v.id()), autre.map(v -> "u3:" + v.id())).take(3))
            .then(() -> {
                publier(
                    "collaboration.message",
                    "c-1",
                    "{\"specversion\":\"1.0\",\"id\":\"m-1\",\"source\":\"/ms-collaboration\",\"type\":\"collaboration.message-sent.v1\"," +
                    "\"subject\":\"c-1\",\"data\":{\"conversationId\":\"c-1\",\"destinataires\":[\"u1\",\"u2\"]}}"
                );
                publier(
                    "homologation.liaison",
                    "l-1",
                    "{\"specversion\":\"1.0\",\"id\":\"h-1\",\"type\":\"homologation.liaison-opened.v1\",\"subject\":\"l-1\"}"
                );
            })
            .recordWith(java.util.ArrayList::new)
            .expectNextCount(3)
            .consumeRecordedWith(recus -> assertThat(recus).containsExactlyInAnyOrder("u1:m-1", "u1:h-1", "u3:h-1"))
            .verifyComplete();
    }

    @Test
    @DisplayName("Portail : événement de son institution seulement ; collaboration jamais remise")
    void portail() {
        StepVerifier.create(evenements(Abonne.de(Couche.PORTAIL, "u1", "INST-0042", null)).take(1))
            .then(() -> {
                publier("collaboration.message", "c-2", "{\"id\":\"m-2\",\"type\":\"collaboration.message-sent.v1\",\"data\":{\"destinataires\":[\"u1\"]}}");
                publier("certification.quitus", "q-1", "{\"id\":\"q-1\",\"type\":\"certification.quitus-signed.v1\",\"institutionid\":\"INST-0099\"}");
                publier("certification.quitus", "q-2", "{\"id\":\"q-2\",\"type\":\"certification.quitus-signed.v1\",\"institutionid\":\"INST-0042\"}");
            })
            .assertNext(v -> assertThat(v).isEqualTo(new VueEvenement("q-2", "certification.quitus-signed.v1", null, "certification")))
            .expectComplete()
            .verify(Duration.ofSeconds(30));
    }
}
