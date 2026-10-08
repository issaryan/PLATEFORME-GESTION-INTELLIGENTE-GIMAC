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

    @Test
    @DisplayName("Demandes (iam.request) et tâches à prendre ou relancées (pilotage.task) : notifications DEMANDE et TACHE avec les liens du contrat")
    void demandesEtTachesAPrendre() throws Exception {
        String demande = UUID.randomUUID().toString();
        String tache = UUID.randomUUID().toString();
        publier(
            "iam.request",
            demande,
            """
            {"specversion":"1.0","id":"%s","type":"iam.request-submitted.v1","subject":"%s","data":{"demandeId":"%s","type":"CREATION_COMPTE","etat":"EN_ATTENTE","demandeurId":"%s","cibleId":null,"destinataires":["%s"],"le":"2026-10-06T08:00:00Z"}}"""
                .formatted(UUID.randomUUID(), demande, demande, CHEF_DES, CONDUCTEUR),
            """
            {"specversion":"1.0","id":"%s","type":"iam.request-decided.v1","subject":"%s","data":{"demandeId":"%s","type":"CREATION_COMPTE","etat":"ACCEPTEE","demandeurId":"%s","acteurId":"%s","destinataires":["%s"],"le":"2026-10-06T09:00:00Z"}}"""
                .formatted(UUID.randomUUID(), demande, demande, CHEF_DES, CONDUCTEUR, CHEF_DES)
        );
        publier(
            "pilotage.task",
            tache,
            """
            {"specversion":"1.0","id":"%s","type":"pilotage.task-offered.v1","subject":"%s","data":{"tacheId":"%s","titre":"Sauvegardes","uniteCible":"POLE_RESEAU","destinataires":["%s","%s"],"acteurId":"%s","le":"2026-10-06T08:00:00Z"}}"""
                .formatted(UUID.randomUUID(), tache, tache, CONDUCTEUR, INGENIEUR, CHEF_DES),
            """
            {"specversion":"1.0","id":"%s","type":"pilotage.task-reminded.v1","subject":"%s","data":{"tacheId":"%s","titre":"Sauvegardes","responsableId":"%s","acteurId":"%s","le":"2026-10-06T10:00:00Z"}}"""
                .formatted(UUID.randomUUID(), tache, tache, INGENIEUR, CHEF_DES)
        );
        // D-98 : offre d'une décision (transition de statut à prononcer) : lien profond vers l'écran de la décision.
        String decision = UUID.randomUUID().toString();
        publier(
            "pilotage.task",
            decision,
            """
            {"specversion":"1.0","id":"%s","type":"pilotage.task-offered.v1","subject":"%s","data":{"tacheId":"%s","titre":"Prononcer l'adhésion : Banque B","uniteCible":null,"destinataires":["%s"],"acteurId":"%s","le":"2026-10-06T08:00:00Z","lien":"/des/institutions/i-1?onglet=transitions"}}"""
                .formatted(UUID.randomUUID(), decision, decision, CHEF_DES, CONDUCTEUR)
        );
        consommateurs.getListenerContainer(EvenementsNotificationsConsumer.GROUPE).start();

        assertThat(attendre("select count(*) from notification_utilisateur where reference_id = ?", 2, demande)).isEqualTo(2);
        assertThat(attendre("select count(*) from notification_utilisateur where reference_id = ?", 3, tache)).isEqualTo(3);
        assertThat(
            jdbc.queryForObject("select lien from notification_utilisateur where reference_id = ? and destinataire_id = ?", String.class, demande, CONDUCTEUR)
        ).isEqualTo("/des/administration/demandes?demande=" + demande);
        assertThat(
            jdbc.queryForObject("select lien from notification_utilisateur where reference_id = ? and destinataire_id = ?", String.class, demande, CHEF_DES)
        ).isEqualTo("/des/equipe/demandes?demande=" + demande);
        assertThat(compter("select count(*) from notification_utilisateur where reference_id = ? and type = 'DEMANDE'", demande)).isEqualTo(2);
        assertThat(compter("select count(*) from notification_utilisateur where reference_id = ? and lien = '/des/taches?vue=unite'", tache)).isEqualTo(2);
        assertThat(compter("select count(*) from notification_utilisateur where reference_id = ? and destinataire_id = ? and lien = ?", tache, INGENIEUR, "/des/taches?tache=" + tache)).isEqualTo(1);
        assertThat(attendre("select count(*) from notification_utilisateur where reference_id = ?", 1, decision)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select lien from notification_utilisateur where reference_id = ? and destinataire_id = ?", String.class, decision, CHEF_DES)).isEqualTo(
            "/des/institutions/i-1?onglet=transitions"
        );
    }

    @Test
    @DisplayName("Mentions et réponses (D-82) sur Kafka réel : MENTION et REPONSE en plus du message, cumul en une MENTION, auteur exclu, ancien événement sans champs, publication sur notification.inapp")
    void mentionsEtReponses() throws Exception {
        String conversation = UUID.randomUUID().toString();
        String m1 = UUID.randomUUID().toString();
        String m2 = UUID.randomUUID().toString();
        String m3 = UUID.randomUUID().toString();
        // m1 : Paul répond à Aminata et mentionne Serge ; m2 : Paul répond à Aminata en la mentionnant (une MENTION) ;
        // m3 : ancien événement sans les champs D-82 ; événement de groupe avec champs : ignoré.
        String reponseEtMention =
            """
            {"specversion":"1.0","id":"%s","type":"collaboration.message-sent.v1","subject":"%s",
            "data":{"conversationId":"%s","messageId":"%s","auteurId":"%s","destinataires":["%s","%s"],"envoyeLe":"2026-10-06T08:00:00Z","evenement":null,"reponseAAuteurId":"%s","mentionnes":["%s"]}}"""
                .formatted(UUID.randomUUID(), conversation, conversation, m1, CHEF_DES, CONDUCTEUR, INGENIEUR, CONDUCTEUR, INGENIEUR);
        String cumul =
            """
            {"specversion":"1.0","id":"%s","type":"collaboration.message-sent.v1","subject":"%s",
            "data":{"conversationId":"%s","messageId":"%s","auteurId":"%s","destinataires":["%s","%s"],"envoyeLe":"2026-10-06T08:00:01Z","evenement":null,"reponseAAuteurId":"%s","mentionnes":["%s"]}}"""
                .formatted(UUID.randomUUID(), conversation, conversation, m2, CHEF_DES, CONDUCTEUR, INGENIEUR, CONDUCTEUR, CONDUCTEUR);
        String ancien =
            """
            {"specversion":"1.0","id":"%s","type":"collaboration.message-sent.v1","subject":"%s",
            "data":{"conversationId":"%s","messageId":"%s","auteurId":"%s","destinataires":["%s"],"envoyeLe":"2026-10-06T08:00:02Z"}}"""
                .formatted(UUID.randomUUID(), conversation, conversation, m3, INGENIEUR, CHEF_DES);
        String groupe =
            """
            {"specversion":"1.0","id":"%s","type":"collaboration.message-sent.v1","subject":"%s",
            "data":{"conversationId":"%s","messageId":"%s","auteurId":"%s","destinataires":["%s"],"envoyeLe":"2026-10-06T08:00:03Z","evenement":"MEMBRE_RETIRE","reponseAAuteurId":null,"mentionnes":[]}}"""
                .formatted(UUID.randomUUID(), conversation, conversation, UUID.randomUUID(), CHEF_DES, CONDUCTEUR);
        publier("collaboration.message", conversation, reponseEtMention, reponseEtMention, cumul, ancien, groupe);
        consommateurs.getListenerContainer(EvenementsNotificationsConsumer.GROUPE).start();

        // Messages : Aminata et Serge (regroupés), Paul (m3) ; m1 : REPONSE Aminata, MENTION Serge ; m2 : MENTION Aminata.
        assertThat(attendre("select count(*) from notification_utilisateur where conversation_id = ?", 6, conversation)).isEqualTo(6);
        assertThat(attendre("select count(*) from notification_utilisateur where reference_id = ? and type = 'MESSAGE' and nombre = 2", 2, conversation)).isEqualTo(2);
        Thread.sleep(1000);
        assertThat(compter("select count(*) from notification_utilisateur where conversation_id = ?", conversation)).isEqualTo(6);
        assertThat(compter("select count(*) from notification_utilisateur where reference_id = ?", m1)).isEqualTo(2);
        assertThat(compter("select count(*) from notification_utilisateur where reference_id = ? and destinataire_id = ? and type = 'REPONSE'", m1, CONDUCTEUR)).isEqualTo(1);
        assertThat(compter("select count(*) from notification_utilisateur where reference_id = ? and destinataire_id = ? and type = 'MENTION'", m1, INGENIEUR)).isEqualTo(1);
        assertThat(compter("select count(*) from notification_utilisateur where reference_id = ?", m2)).isEqualTo(1);
        assertThat(compter("select count(*) from notification_utilisateur where reference_id = ? and destinataire_id = ? and type = 'MENTION'", m2, CONDUCTEUR)).isEqualTo(1);
        assertThat(compter("select count(*) from notification_utilisateur where reference_id = ?", m3)).isZero();
        assertThat(compter("select count(*) from notification_utilisateur where conversation_id = ? and destinataire_id = ? and type <> 'MESSAGE'", conversation, CHEF_DES)).isZero();
        assertThat(jdbc.queryForObject("select titre from notification_utilisateur where reference_id = ? and destinataire_id = ?", String.class, m1, INGENIEUR)).isEqualTo(
            "Paul Mbarga Atangana vous a mentionné"
        );
        assertThat(jdbc.queryForObject("select lien from notification_utilisateur where reference_id = ? and destinataire_id = ?", String.class, m1, CONDUCTEUR)).isEqualTo(
            "/des/messagerie?conversation=" + conversation + "&message=" + m1
        );

        String notification = jdbc.queryForObject(
            "select id::text from notification_utilisateur where reference_id = ? and destinataire_id = ?",
            String.class,
            m1,
            INGENIEUR
        );
        relais.relayerJusquaEnvoi(notification);
        List<ConsumerRecord<String, String>> publies = relais.lire("notification.inapp", notification);
        assertThat(publies).isNotEmpty();
        JsonNode d = json.readTree(publies.get(0).value()).path("data");
        assertThat(d.path("type").asText()).isEqualTo("MENTION");
        assertThat(d.path("destinataires")).hasSize(1);
        assertThat(d.path("destinataires").get(0).asText()).isEqualTo(INGENIEUR);
        assertThat(d.path("referenceService").asText()).isEqualTo("collaboration");
        assertThat(d.path("referenceType").asText()).isEqualTo("MESSAGE");
        assertThat(d.path("referenceId").asText()).isEqualTo(m1);
        assertThat(d.path("nombre").asInt()).isEqualTo(1);
    }

    @Test
    @DisplayName("D-87 : processus d'unité (pilotage.process) sur Kafka réel : PROCESSUS par destinataire, jamais l'acteur, titres, lien tel quel, livraison en double consommée une fois, process-published ignoré, publication sur notification.inapp")
    void processus() throws Exception {
        String instance = UUID.randomUUID().toString();
        String lien = "/des/processus/instances/" + instance;
        String lancee =
            """
            {"specversion":"1.0","id":"%s","type":"pilotage.process-instance-started.v1","source":"/ms-pilotage","subject":"%s",
            "data":{"instanceId":"%s","processusId":"%s","code":"OUVERTURE_FLUX_RESEAU","version":1,"unite":"POLE_RESEAU","intitule":"Flux vers la banque X","lanceurId":"%s","destinataires":["%s","%s"],"lien":"%s","le":"2026-10-07T08:00:00Z"}}"""
                .formatted(UUID.randomUUID(), instance, instance, UUID.randomUUID(), CONDUCTEUR, CHEF_DES, CONDUCTEUR, lien);
        String activee =
            """
            {"specversion":"1.0","id":"%s","type":"pilotage.process-step-activated.v1","source":"/ms-pilotage","subject":"%s",
            "data":{"instanceId":"%s","processusId":"%s","code":"OUVERTURE_FLUX_RESEAU","intitule":"Flux vers la banque X","etape":"ANALYSE","libelleEtape":"Analyse de la demande","tacheId":"%s","executantId":null,"profil":"ROLE_POLE_RESEAU","unite":"POLE_RESEAU","echeance":"2026-10-09","renvoi":false,"motif":null,"destinataires":["%s"],"lien":"%s?etape=ANALYSE","le":"2026-10-07T08:00:01Z"}}"""
                .formatted(UUID.randomUUID(), instance, instance, UUID.randomUUID(), UUID.randomUUID(), INGENIEUR, lien);
        String signee =
            """
            {"specversion":"1.0","id":"%s","type":"pilotage.process-step-signed.v1","source":"/ms-pilotage","subject":"%s",
            "data":{"instanceId":"%s","code":"OUVERTURE_FLUX_RESEAU","intitule":"Flux vers la banque X","etape":"RECETTE","signature":"PV","signataireId":"%s","empreinte":"ab","documents":[{"livrable":"PV_RECETTE","documentId":"%s","sha256":"cd"}],"etatEtape":"TERMINEE","destinataires":["%s"],"lien":"%s?etape=RECETTE","le":"2026-10-07T08:00:02Z"}}"""
                .formatted(UUID.randomUUID(), instance, instance, CHEF_DES, UUID.randomUUID(), INGENIEUR, lien);
        String close =
            """
            {"specversion":"1.0","id":"%s","type":"pilotage.process-instance-closed.v1","source":"/ms-pilotage","subject":"%s",
            "data":{"instanceId":"%s","processusId":"%s","code":"OUVERTURE_FLUX_RESEAU","intitule":"Flux vers la banque X","etat":"CLOSE","lanceurId":"%s","acteurId":"%s","motif":null,"destinataires":["%s"],"lien":"%s","le":"2026-10-07T08:00:03Z"}}"""
                .formatted(UUID.randomUUID(), instance, instance, UUID.randomUUID(), CONDUCTEUR, CHEF_DES, CONDUCTEUR, lien);
        String publiee =
            """
            {"specversion":"1.0","id":"%s","type":"pilotage.process-published.v1","source":"/ms-pilotage","subject":"%s",
            "data":{"processusId":"%s","code":"OUVERTURE_FLUX_RESEAU","version":1,"unite":"POLE_RESEAU","libelle":"Ouverture","acteurId":"%s","motif":null,"avertissementsLeves":false,"versionRetiree":null,"le":"2026-10-07T07:00:00Z"}}"""
                .formatted(UUID.randomUUID(), instance, UUID.randomUUID(), CHEF_DES);
        publier("pilotage.process", instance, publiee, lancee, lancee, activee, signee, close);
        consommateurs.getListenerContainer(EvenementsNotificationsConsumer.GROUPE).start();

        // Lancée : le chef (pas le lanceur) ; activée : Serge ; signée : Serge ; close : Aminata (lanceuse).
        assertThat(attendre("select count(*) from notification_utilisateur where reference_id = ? and type = 'PROCESSUS'", 4, instance)).isEqualTo(4);
        Thread.sleep(1000);
        assertThat(compter("select count(*) from notification_utilisateur where reference_id = ?", instance)).isEqualTo(4);
        assertThat(compter("select count(*) from notification_utilisateur where reference_id = ? and reference_service = 'pilotage' and reference_type = 'PROCESSUS' and nombre = 1", instance)).isEqualTo(4);
        assertThat(compter("select count(*) from notification_utilisateur where reference_id = ? and destinataire_id = ?", instance, CHEF_DES)).isEqualTo(1);
        assertThat(compter("select count(*) from notification_utilisateur where reference_id = ? and destinataire_id = ?", instance, INGENIEUR)).isEqualTo(2);
        assertThat(
            jdbc.queryForMap("select titre, sous_titre, lien from notification_utilisateur where reference_id = ? and destinataire_id = ?", instance, CHEF_DES)
        ).containsEntry("titre", "Processus lancé : Flux vers la banque X").containsEntry("sous_titre", "OUVERTURE_FLUX_RESEAU, lancé par Aminata Ngono").containsEntry("lien", lien);
        assertThat(
            jdbc.queryForMap("select titre, sous_titre, lien from notification_utilisateur where reference_id = ? and destinataire_id = ? and titre like 'Étape à prendre%'", instance, INGENIEUR)
        ).containsEntry("titre", "Étape à prendre : Analyse de la demande").containsEntry("sous_titre", "Flux vers la banque X, échéance 2026-10-09").containsEntry("lien", lien + "?etape=ANALYSE");
        assertThat(
            jdbc.queryForMap("select titre, sous_titre from notification_utilisateur where reference_id = ? and destinataire_id = ? and titre like 'Étape signée%'", instance, INGENIEUR)
        ).containsEntry("titre", "Étape signée : RECETTE").containsEntry("sous_titre", "Flux vers la banque X, signée par Paul Mbarga Atangana");
        assertThat(
            jdbc.queryForMap("select titre, sous_titre from notification_utilisateur where reference_id = ? and destinataire_id = ?", instance, CONDUCTEUR)
        ).containsEntry("titre", "Processus terminé : Flux vers la banque X").containsEntry("sous_titre", "OUVERTURE_FLUX_RESEAU");

        String notification = jdbc.queryForObject(
            "select id::text from notification_utilisateur where reference_id = ? and destinataire_id = ?",
            String.class,
            instance,
            CONDUCTEUR
        );
        relais.relayerJusquaEnvoi(notification);
        List<ConsumerRecord<String, String>> publies = relais.lire("notification.inapp", notification);
        assertThat(publies).isNotEmpty();
        JsonNode d = json.readTree(publies.get(0).value()).path("data");
        assertThat(d.path("type").asText()).isEqualTo("PROCESSUS");
        assertThat(d.path("destinataires").get(0).asText()).isEqualTo(CONDUCTEUR);
        assertThat(d.path("referenceService").asText()).isEqualTo("pilotage");
        assertThat(d.path("referenceId").asText()).isEqualTo(instance);
    }
}
