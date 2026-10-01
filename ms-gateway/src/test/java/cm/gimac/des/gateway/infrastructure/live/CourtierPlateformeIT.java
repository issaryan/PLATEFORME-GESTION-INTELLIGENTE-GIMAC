package cm.gimac.des.gateway.infrastructure.live;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.gateway.domain.live.EvenementLive;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;
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
import org.junit.jupiter.api.Timeout;
import org.testcontainers.kafka.KafkaContainer;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.AbstractConstruct;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.SequenceNode;
import reactor.test.StepVerifier;

/**
 * Canal poussé sur un courtier configuré comme celui de l'environnement Docker Compose de la
 * plateforme (infra/docker-compose : docker-compose.yml généré, complété par docker-compose.gimac.yml).
 *
 * <p>Les autres tests Kafka utilisent la configuration de Testcontainers, qui règle à 1 la réplication
 * des sujets internes ; la pile, elle, reçoit seulement les variables de son Compose. Sans
 * {@code KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1}, le courtier mono-nœud ne peut pas créer
 * {@code __consumer_offsets} (facteur 3 par défaut) : aucun coordinateur de groupe, aucun consommateur
 * abonné ne reçoit de partition et le canal poussé reste muet. Ici, seules les variables d'écoute
 * réseau viennent de Testcontainers (courtier joignable depuis l'hôte) ; toutes les autres sont
 * celles du Compose.
 */
class CourtierPlateformeIT {

    static final Path COMPOSE = Path.of("..", "infra", "docker-compose");

    /** Variables d'écoute réseau : celles de Testcontainers, pour joindre le courtier depuis l'hôte. */
    static final Set<String> RESEAU = Set.of(
        "CLUSTER_ID",
        "KAFKA_NODE_ID",
        "KAFKA_PROCESS_ROLES",
        "KAFKA_LISTENERS",
        "KAFKA_ADVERTISED_LISTENERS",
        "KAFKA_LISTENER_SECURITY_PROTOCOL_MAP",
        "KAFKA_INTER_BROKER_LISTENER_NAME",
        "KAFKA_CONTROLLER_LISTENER_NAMES",
        "KAFKA_CONTROLLER_QUORUM_VOTERS"
    );

    static final String SUJET = "collaboration.message";

    static KafkaContainer kafka;
    static KafkaSourceEvenements source;

    @BeforeAll
    static void demarrer() throws Exception {
        Map<String, Object> service = serviceKafka();
        kafka = new KafkaContainer(String.valueOf(service.get("image"))).withStartupTimeout(Duration.ofMinutes(2)).withStartupAttempts(3);
        Map<String, String> env = new LinkedHashMap<>(kafka.getEnvMap());
        env.keySet().retainAll(RESEAU);
        environnement(service).forEach((cle, valeur) -> {
            if (!RESEAU.contains(cle)) {
                env.put(cle, valeur);
            }
        });
        kafka.setEnv(env.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue()).toList());
        kafka.start();
        try (Admin admin = Admin.create(Map.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers()))) {
            admin.createTopics(List.of(new NewTopic(SUJET, 6, (short) 1))).all().get();
        }
        source = new KafkaSourceEvenements(
            Map.of(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers()),
            List.of(SUJET),
            new LectureCloudEvent()
        );
        source.start();
    }

    @AfterAll
    static void arreter() {
        if (source != null) {
            source.stop();
        }
        if (kafka != null) {
            kafka.stop();
        }
    }

    @Test
    @Timeout(value = 2, unit = TimeUnit.MINUTES)
    @DisplayName("Courtier du Compose : le consommateur de la passerelle obtient ses partitions et reçoit l'événement")
    void remiseSurLeCourtierDuCompose() throws Exception {
        long limite = System.currentTimeMillis() + 60_000;
        while (source.partitionsAttribuees() < 6 && System.currentTimeMillis() < limite) {
            Thread.sleep(200);
        }
        assertThat(source.partitionsAttribuees())
            .as("partitions attribuées au groupe %s (coordinateur de groupe disponible ?)", source.groupe())
            .isEqualTo(6);

        Properties p = new Properties();
        p.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        p.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        p.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        StepVerifier.create(source.evenements().take(1))
            .then(() -> {
                try (KafkaProducer<String, String> producteur = new KafkaProducer<>(p)) {
                    producteur.send(
                        new ProducerRecord<>(
                            SUJET,
                            "c-1",
                            "{\"specversion\":\"1.0\",\"id\":\"m-1\",\"type\":\"collaboration.message-sent.v1\"," +
                            "\"subject\":\"c-1\",\"data\":{\"conversationId\":\"c-1\",\"destinataires\":[\"u1\"]}}"
                        )
                    );
                }
            })
            .assertNext((EvenementLive e) -> assertThat(e.id()).isEqualTo("m-1"))
            .expectComplete()
            .verify(Duration.ofSeconds(30));
    }

    /** Service kafka du Compose : docker-compose.yml généré, surchargé par docker-compose.gimac.yml. */
    @SuppressWarnings("unchecked")
    static Map<String, Object> serviceKafka() throws IOException {
        Map<String, Object> service = new LinkedHashMap<>();
        Map<String, String> env = new LinkedHashMap<>();
        for (String fichier : List.of("docker-compose.yml", "docker-compose.gimac.yml")) {
            try (Reader r = Files.newBufferedReader(COMPOSE.resolve(fichier))) {
                Map<String, Object> racine = new Yaml(new EtiquettesCompose()).load(r);
                Map<String, Object> services = (Map<String, Object>) racine.get("services");
                Map<String, Object> kafka = (Map<String, Object>) services.get("kafka");
                if (kafka != null) {
                    env.putAll(environnement(kafka));
                    service.putAll(kafka);
                }
            }
        }
        assertThat(service).as("service kafka de %s", COMPOSE).containsKey("image");
        service.put("environment", env);
        return service;
    }

    /** Variables d'environnement d'un service, sous forme de table ou de liste « CLE=valeur ». */
    @SuppressWarnings("unchecked")
    static Map<String, String> environnement(Map<String, Object> service) {
        Map<String, String> env = new LinkedHashMap<>();
        Object brut = service.get("environment");
        if (brut instanceof Map<?, ?> table) {
            table.forEach((cle, valeur) -> env.put(String.valueOf(cle), String.valueOf(valeur)));
        } else if (brut instanceof List<?> liste) {
            for (Object ligne : liste) {
                String[] cv = String.valueOf(ligne).split("=", 2);
                env.put(cv[0], cv.length > 1 ? cv[1] : "");
            }
        }
        return env;
    }

    /** Lecture sûre du YAML ; les étiquettes de fusion de Compose (!override, !reset) sont ignorées. */
    static final class EtiquettesCompose extends SafeConstructor {

        EtiquettesCompose() {
            super(new LoaderOptions());
            yamlMultiConstructors.put(
                "!",
                new AbstractConstruct() {
                    @Override
                    public Object construct(Node noeud) {
                        return switch (noeud) {
                            case ScalarNode s -> constructScalar(s);
                            case SequenceNode q -> constructSequence(q);
                            case MappingNode m -> constructMapping(m);
                            default -> null;
                        };
                    }
                }
            );
        }
    }
}
