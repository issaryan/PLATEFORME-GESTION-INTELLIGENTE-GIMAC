package cm.gimac.des.notification.infrastructure.messaging.consumer;

import static cm.gimac.des.notification.infrastructure.client.AnnuaireStubAdapter.CHEF_DES;
import static cm.gimac.des.notification.infrastructure.client.AnnuaireStubAdapter.CONDUCTEUR;
import static cm.gimac.des.notification.infrastructure.client.AnnuaireStubAdapter.INGENIEUR;
import static cm.gimac.des.notification.infrastructure.client.AnnuaireStubAdapter.RSSI;
import static cm.gimac.des.notification.infrastructure.client.ComptesInstitutionStubAdapter.CORRESPONDANT_A1;
import static cm.gimac.des.notification.infrastructure.client.ComptesInstitutionStubAdapter.CORRESPONDANT_A2;
import static cm.gimac.des.notification.infrastructure.client.ComptesInstitutionStubAdapter.INSTITUTION_A;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cm.gimac.des.notification.AbstractNotificationIT;
import cm.gimac.des.notification.RelaisOutboxDeTest;
import cm.gimac.des.notification.infrastructure.client.ComptesInstitutionStubAdapter;
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
import org.springframework.http.MediaType;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;

/**
 * Parcours d'homologation sur Kafka et PostgreSQL réels (D-93 §11) : operation-activated, operation-validation-requested
 * et liaison-milestone-reached publiés par MS-HOMOLOGATION, consommés une seule fois (livraison en double), une
 * notification HOMOLOGATION par compte selon son rôle RACI, comptes de l'institution sur le portail, fonction à affecter
 * jamais nommée, préférence respectée, puis notification.inapp-created.v1 relayé sur notification.inapp.
 */
class EvenementsHomologationKafkaIT extends AbstractNotificationIT {

    @Autowired
    KafkaListenerEndpointRegistry consommateurs;

    @Autowired
    RelaisOutboxDeTest relais;

    @Autowired
    ComptesInstitutionStubAdapter institutions;

    @Value("${spring.kafka.bootstrap-servers}")
    String courtier;

    @AfterEach
    void arreterConsommateur() {
        consommateurs.getListenerContainer(EvenementsHomologationConsumer.GROUPE).stop();
        institutions.reinitialiser();
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
    @DisplayName("Opération ouverte puis en validation, jalon atteint : rôles RACI, institution sur le portail, à affecter ignoré, doublon consommé une fois, préférence respectée, notification.inapp")
    void parcours() throws Exception {
        String liaison = UUID.randomUUID().toString();
        String operation = UUID.randomUUID().toString();
        String lien = "/des/parcours/" + liaison + "?onglet=operations&operation=" + operation;
        // Le RSSI a désactivé les notifications d'homologation.
        mvc
            .perform(put("/api/v1/preferences").contentType(MediaType.APPLICATION_JSON).content("{\"types\":{\"HOMOLOGATION\":false}}").with(interne(RSSI)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.types.HOMOLOGATION").value(false));
        String activee =
            """
            {"specversion":"1.0","id":"%s","type":"homologation.operation-activated.v1","source":"/gimac/des/homologation","subject":"%s","institutionid":"%s",
            "data":{"liaisonId":"%s","reference":"LIA-2026-007","participantId":"%s","institutionNom":"Banque A","typeProjet":"CARTE_INTERBANCAIRE","etatLiaison":"OUVERTE","statut":"CADREE",
            "operationId":"%s","code":"12","libelle":"Coordonnées techniques d'essai","etape":"ESSAIS","vue":"COORDONNEES","geste":"DEPOSER_LIVRABLE","echeance":"2026-10-20","motif":"OUVERTURE",
            "responsable":{"qualite":"R","fonction":"PARTICIPANT","libelleFonction":"Participant","aAffecter":false,"institutionId":"%s"},
            "validateur":{"qualite":"V","fonction":"SNA","compteId":"%s","aAffecter":false},
            "destinataires":[{"qualite":"C","fonction":"DDRM","compteId":"%s","aAffecter":false},{"qualite":"I","fonction":"DES","compteId":"%s","aAffecter":false},
                             {"qualite":"I","fonction":"RSSI","compteId":"%s","aAffecter":false},{"qualite":"I","fonction":"ARCHITECTE","aAffecter":true,"profils":["P"]}],
            "externe":true,"institutionId":"%s","institutionsConcernees":["%s"],"lien":"%s","le":"2026-10-07T08:00:00Z"}}"""
                .formatted(UUID.randomUUID(), liaison, INSTITUTION_A, liaison, INSTITUTION_A, operation, INSTITUTION_A, INGENIEUR, CONDUCTEUR, CHEF_DES, RSSI, INSTITUTION_A, INSTITUTION_A, lien);
        String validation =
            """
            {"specversion":"1.0","id":"%s","type":"homologation.operation-validation-requested.v1","source":"/gimac/des/homologation","subject":"%s",
            "data":{"liaisonId":"%s","reference":"LIA-2026-007","institutionNom":"Banque A","operationId":"%s","code":"12","libelle":"Coordonnées techniques d'essai","geste":"DEPOSER_LIVRABLE",
            "responsable":{"qualite":"R","fonction":"PARTICIPANT","aAffecter":false,"institutionId":"%s"},
            "validateur":{"qualite":"V","fonction":"SNA","compteId":"%s","aAffecter":false},"destinataires":[],
            "externe":true,"institutionId":"%s","institutionsConcernees":["%s"],"lien":"%s","le":"2026-10-07T09:00:00Z"}}"""
                .formatted(UUID.randomUUID(), liaison, liaison, operation, INSTITUTION_A, INGENIEUR, INSTITUTION_A, INSTITUTION_A, lien);
        String jalon =
            """
            {"specversion":"1.0","id":"%s","type":"homologation.liaison-milestone-reached.v1","source":"/gimac/des/homologation","subject":"%s",
            "data":{"liaisonId":"%s","reference":"LIA-2026-007","institutionId":"%s","institutionNom":"Banque A","typeProjet":"CARTE_INTERBANCAIRE","modeleTypeProjet":"CARTE",
            "jalon":"EN_PRODUCTION","libelleJalon":"En production","statut":"EN_PRODUCTION","etatLiaison":"EN_PRODUCTION","operationId":"%s","code":"35","libelle":"Mise en production",
            "versionId":"%s","versionReferentiel":1,"conducteurId":"%s","destinataires":[{"qualite":"I","fonction":"DES","compteId":"%s","aAffecter":false}],
            "lien":"/des/parcours/%s","le":"2026-10-08T08:00:00Z"}}"""
                .formatted(UUID.randomUUID(), liaison, liaison, INSTITUTION_A, UUID.randomUUID(), UUID.randomUUID(), CONDUCTEUR, CHEF_DES, liaison);
        String signee =
            """
            {"specversion":"1.0","id":"%s","type":"homologation.operation-signed.v1","subject":"%s","data":{"liaisonId":"%s","operationId":"%s","signataireId":"%s"}}"""
                .formatted(UUID.randomUUID(), liaison, liaison, operation, CONDUCTEUR);
        publier("homologation.operation", liaison, activee, activee, signee, validation);
        publier("homologation.liaison", liaison, jalon, jalon);
        consommateurs.getListenerContainer(EvenementsHomologationConsumer.GROUPE).start();

        // Ouverte : deux correspondants (action), le conducteur (avis), le chef (information) ; RSSI (préférence) et
        // fonction à affecter : rien. Validation : l'ingénieur (V) et les deux correspondants. Jalon : conducteur, chef,
        // deux correspondants.
        assertThat(attendre("select count(*) from notification_utilisateur where type = 'HOMOLOGATION'", 11)).isEqualTo(11);
        Thread.sleep(1000);
        assertThat(compter("select count(*) from notification_utilisateur where type = 'HOMOLOGATION'")).isEqualTo(11);
        assertThat(compter("select count(*) from notification_utilisateur where reference_id = ? and reference_service = 'homologation' and reference_type = 'OPERATION' and nombre = 1", operation)).isEqualTo(7);
        assertThat(compter("select count(*) from notification_utilisateur where reference_id = ? and reference_type = 'LIAISON'", liaison)).isEqualTo(4);
        assertThat(compter("select count(*) from notification_utilisateur where destinataire_id = ?", RSSI)).isZero();
        assertThat(
            jdbc.queryForMap("select titre, sous_titre, lien from notification_utilisateur where destinataire_id = ? and titre like 'Action attendue%'", CORRESPONDANT_A1)
        )
            .containsEntry("titre", "Action attendue : Coordonnées techniques d'essai")
            .containsEntry("sous_titre", "LIA-2026-007 (Banque A), échéance 2026-10-20")
            .containsEntry("lien", "/portail/liaisons/" + liaison + "/livrables/" + operation);
        assertThat(jdbc.queryForMap("select titre, lien from notification_utilisateur where destinataire_id = ? and reference_type = 'OPERATION'", CONDUCTEUR))
            .containsEntry("titre", "Avis demandé : Coordonnées techniques d'essai")
            .containsEntry("lien", lien);
        assertThat(jdbc.queryForMap("select titre from notification_utilisateur where destinataire_id = ? and reference_type = 'OPERATION'", INGENIEUR)).containsEntry(
            "titre",
            "Validation attendue : Coordonnées techniques d'essai"
        );
        assertThat(jdbc.queryForMap("select titre, lien from notification_utilisateur where destinataire_id = ? and titre like 'Opération en validation%'", CORRESPONDANT_A2)).containsEntry(
            "lien",
            "/portail/liaisons/" + liaison
        );
        assertThat(jdbc.queryForMap("select titre, sous_titre, lien from notification_utilisateur where destinataire_id = ? and reference_type = 'LIAISON'", CONDUCTEUR))
            .containsEntry("titre", "Jalon atteint : En production")
            .containsEntry("sous_titre", "LIA-2026-007 (Banque A)")
            .containsEntry("lien", "/des/parcours/" + liaison);

        String notification = jdbc.queryForObject(
            "select id::text from notification_utilisateur where destinataire_id = ? and reference_type = 'LIAISON'",
            String.class,
            CORRESPONDANT_A1
        );
        relais.relayerJusquaEnvoi(notification);
        List<ConsumerRecord<String, String>> publies = relais.lire("notification.inapp", notification);
        assertThat(publies).isNotEmpty();
        JsonNode d = json.readTree(publies.get(0).value()).path("data");
        assertThat(d.path("type").asText()).isEqualTo("HOMOLOGATION");
        assertThat(d.path("destinataires").get(0).asText()).isEqualTo(CORRESPONDANT_A1);
        assertThat(d.path("referenceService").asText()).isEqualTo("homologation");
        assertThat(d.path("referenceType").asText()).isEqualTo("LIAISON");
        assertThat(d.path("referenceId").asText()).isEqualTo(liaison);
    }
}
