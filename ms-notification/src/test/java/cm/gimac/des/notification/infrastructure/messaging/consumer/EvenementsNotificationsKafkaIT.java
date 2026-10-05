package cm.gimac.des.notification.infrastructure.messaging.consumer;

import static cm.gimac.des.notification.infrastructure.client.AnnuaireStubAdapter.CHEF_DES;
import static cm.gimac.des.notification.infrastructure.client.AnnuaireStubAdapter.CONDUCTEUR;
import static cm.gimac.des.notification.infrastructure.client.AnnuaireStubAdapter.INGENIEUR;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.notification.AbstractNotificationIT;
import cm.gimac.des.notification.RelaisOutboxDeTest;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;

/**
 * Chaîne de bout en bout sur Kafka et PostgreSQL réels (docs/lot1/notifications.md §1) :
 * collaboration.message-sent.v1 et pilotage.task-assigned.v1 publiés sur leurs sujets, consommés une
 * seule fois (livraison en double), notification par destinataire hors auteur, puis
 * notification.inapp-created.v1 relayé par l'outbox sur notification.inapp avec destinataires: [id].
 */
class EvenementsNotificationsKafkaIT extends AbstractNotificationIT {

    @Autowired
    KafkaListenerEndpointRegistry consommateurs;

    @Autowired
    RelaisOutboxDeTest relais;

    @Value("${spring.kafka.bootstrap-servers}")
    String courtier;

    @AfterEach
    void arreterConsommateur() {
        consommateurs.getListenerContainer(EvenementsNotificationsConsumer.GROUPE).stop();
    }

    void publier(String sujet, String cle, String... valeurs) throws Exception {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, courtier);
        try (var producteur = new KafkaProducer<>(props, new StringSerializer(), new StringSerializer())) {
            for (String v : valeurs) {
                producteur.send(new ProducerRecord<>(sujet, cle, v)).get();
            }
        }
    }

    int attendre(String sql, int attendu, Object... args) throws InterruptedException {
        int n = 0;
        for (int essai = 0; essai < 120 && n < attendu; essai++) {
            n = compter(sql, args);
            if (n < attendu) {
                Thread.sleep(500);
            }
        }
        return n;
    }

    @Test
    @DisplayName("Message envoyé : une notification par destinataire, jamais l'auteur, consommée une fois ; événement de groupe ignoré ; publication sur notification.inapp")
    void messageEnvoye() throws Exception {
        String conversation = UUID.randomUUID().toString();
        String evenement =
            """
            {"specversion":"1.0","id":"%s","source":"/gimac/des/collaboration","type":"collaboration.message-sent.v1","subject":"%s",
            "time":"2026-10-05T08:00:00Z","datacontenttype":"application/json",
            "data":{"conversationId":"%s","messageId":"%s","auteurId":"%s","destinataires":["%s","%s"],"envoyeLe":"2026-10-05T08:00:00Z"}}"""
                .formatted(UUID.randomUUID(), conversation, conversation, UUID.randomUUID(), CHEF_DES, CONDUCTEUR, INGENIEUR);
        String ajoutMembres =
            """
            {"specversion":"1.0","id":"%s","type":"collaboration.message-sent.v1","subject":"%s",
            "data":{"conversationId":"%s","messageId":"%s","auteurId":"%s","destinataires":["%s"],"envoyeLe":"2026-10-05T08:00:01Z","evenement":"MEMBRES_AJOUTES"}}"""
                .formatted(UUID.randomUUID(), conversation, conversation, UUID.randomUUID(), CHEF_DES, CONDUCTEUR);
        // Livré deux fois : le consommateur idempotent ne le traite qu'une fois.
        publier("collaboration.message", conversation, evenement, evenement, ajoutMembres);
        consommateurs.getListenerContainer(EvenementsNotificationsConsumer.GROUPE).start();

        assertThat(attendre("select count(*) from notification_utilisateur where reference_id = ?", 2, conversation)).isEqualTo(2);
        assertThat(attendre("select count(*) from processed_event where consumer_group = ?", 2, EvenementsNotificationsConsumer.GROUPE)).isGreaterThanOrEqualTo(2);
        Thread.sleep(1000);
        assertThat(compter("select count(*) from notification_utilisateur where reference_id = ?", conversation)).isEqualTo(2);
        assertThat(compter("select count(*) from notification_utilisateur where reference_id = ? and destinataire_id = ?", conversation, CHEF_DES)).isZero();
        assertThat(compter("select coalesce(max(nombre), 0) from notification_utilisateur where reference_id = ?", conversation)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select titre from notification_utilisateur where reference_id = ? and destinataire_id = ?", String.class, conversation, CONDUCTEUR)).isEqualTo(
            "Paul Mbarga Atangana"
        );

        String notification = jdbc.queryForObject(
            "select id::text from notification_utilisateur where reference_id = ? and destinataire_id = ?",
            String.class,
            conversation,
            CONDUCTEUR
        );
        relais.relayerJusquaEnvoi(notification);
        List<ConsumerRecord<String, String>> publies = relais.lire("notification.inapp", notification);
        assertThat(publies).isNotEmpty();
        JsonNode e = json.readTree(publies.get(0).value());
        assertThat(e.path("specversion").asText()).isEqualTo("1.0");
        assertThat(e.path("type").asText()).isEqualTo("notification.inapp-created.v1");
        assertThat(e.path("subject").asText()).isEqualTo(notification);
        JsonNode d = e.path("data");
        assertThat(d.path("destinataires")).hasSize(1);
        assertThat(d.path("destinataires").get(0).asText()).isEqualTo(CONDUCTEUR);
        assertThat(d.path("type").asText()).isEqualTo("MESSAGE");
        assertThat(d.path("referenceId").asText()).isEqualTo(conversation);
        assertThat(d.path("nombre").asInt()).isEqualTo(1);
    }

    @Test
    @DisplayName("Tâche confiée (pilotage.task) au nouveau responsable ; confiée à soi-même : rien ; rapport soumis (pilotage.report) au destinataire")
    void pilotage() throws Exception {
        String tache = UUID.randomUUID().toString();
        String autoAffectee = UUID.randomUUID().toString();
        String rapport = UUID.randomUUID().toString();
        publier(
            "pilotage.task",
            tache,
            """
            {"specversion":"1.0","id":"%s","type":"pilotage.task-created.v1","subject":"%s","data":{"tacheId":"%s","responsableId":"%s","acteurId":"%s"}}"""
                .formatted(UUID.randomUUID(), tache, tache, CONDUCTEUR, CHEF_DES),
            """
            {"specversion":"1.0","id":"%s","type":"pilotage.task-assigned.v1","subject":"%s","data":{"tacheId":"%s","etat":"OUVERTE","responsableId":"%s","ancienResponsableId":null,"acteurId":"%s","le":"2026-10-05T08:00:00Z"}}"""
                .formatted(UUID.randomUUID(), tache, tache, CONDUCTEUR, CHEF_DES),
            """
            {"specversion":"1.0","id":"%s","type":"pilotage.task-assigned.v1","subject":"%s","data":{"tacheId":"%s","etat":"OUVERTE","responsableId":"%s","ancienResponsableId":"%s","acteurId":"%s","le":"2026-10-05T08:00:00Z"}}"""
                .formatted(UUID.randomUUID(), autoAffectee, autoAffectee, CHEF_DES, CONDUCTEUR, CHEF_DES)
        );
        publier(
            "pilotage.report",
            rapport,
            """
            {"specversion":"1.0","id":"%s","type":"pilotage.report-submitted.v1","subject":"%s","data":{"rapportId":"%s","type":"HEBDOMADAIRE","periode":"2026-W40","etat":"SOUMIS","auteurId":"%s","destinataireId":"%s","acteurId":"%s","le":"2026-10-05T09:00:00Z"}}"""
                .formatted(UUID.randomUUID(), rapport, rapport, INGENIEUR, CHEF_DES, INGENIEUR)
        );
        consommateurs.getListenerContainer(EvenementsNotificationsConsumer.GROUPE).start();

        assertThat(attendre("select count(*) from notification_utilisateur where reference_id in (?, ?)", 2, tache, rapport)).isEqualTo(2);
        assertThat(jdbc.queryForObject("select destinataire_id from notification_utilisateur where reference_id = ?", String.class, tache)).isEqualTo(CONDUCTEUR);
        assertThat(jdbc.queryForObject("select lien from notification_utilisateur where reference_id = ?", String.class, tache)).isEqualTo("/des/taches?tache=" + tache);
        assertThat(jdbc.queryForObject("select destinataire_id from notification_utilisateur where reference_id = ?", String.class, rapport)).isEqualTo(CHEF_DES);
        assertThat(jdbc.queryForObject("select sous_titre from notification_utilisateur where reference_id = ?", String.class, rapport)).isEqualTo(
            "Rapport hebdomadaire 2026-W40"
        );
        Thread.sleep(1000);
        assertThat(compter("select count(*) from notification_utilisateur where reference_id = ?", autoAffectee)).isZero();
    }
}
