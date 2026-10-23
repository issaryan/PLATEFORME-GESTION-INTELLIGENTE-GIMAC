package cm.gimac.des.notification.infrastructure.messaging.consumer;

import static cm.gimac.des.notification.infrastructure.client.DestinatairesSandboxStubAdapter.ADMIN_99001;
import static cm.gimac.des.notification.infrastructure.client.DestinatairesSandboxStubAdapter.ADMIN_GIMAC;
import static cm.gimac.des.notification.infrastructure.client.DestinatairesSandboxStubAdapter.CHEF_99001;
import static cm.gimac.des.notification.infrastructure.client.DestinatairesSandboxStubAdapter.TESTEUR_99001;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.springframework.http.MediaType;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;

/**
 * Demandes de notification de la sandbox sur Kafka et PostgreSQL réels (US-EXI-09) : invitation et suspension publiées
 * par SBX-ESPACE sur leur sujet, remises une seule fois malgré une redélivrance, comptes de l'institution sur le portail,
 * administrateur du GIMAC sur le DES, préférence respectée ; demande sans règle de SBX-RACCORDEMENT au rebut ;
 * notification.inapp-created.v1 relayé ; résolution à blanc et catalogue réservés à SBX_HAB_ADMINISTRATION.
 */
class DemandesSandboxKafkaIT extends AbstractNotificationIT {

    @Autowired
    KafkaListenerEndpointRegistry consommateurs;

    @Autowired
    RelaisOutboxDeTest relais;

    @Value("${spring.kafka.bootstrap-servers}")
    String courtier;

    @AfterEach
    void arreter() {
        consommateurs.getListenerContainer(DemandesSandboxConsumer.GROUPE).stop();
        jdbc.execute("truncate table rebut_notification_sandbox");
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

    static String demande(String id, String service, String modele, String destinataires) {
        return """
        {"specversion":"1.0","id":"%s","type":"%s.notification-requested.v1","source":"/gimac/sandbox/%s","partitionkey":"99001",
        "data":{"modele":"%s","destinataires":[%s],"codeParticipant":"99001","codeEspace":"ESP-99001","le":"2026-10-23T08:00:00Z"}}""".formatted(
                id,
                service,
                service,
                modele,
                destinataires
            );
    }

    @Test
    @DisplayName("Invitation et suspension remises une fois, portail et DES, préférence ; sans règle au rebut ; notification.inapp")
    void parcours() throws Exception {
        // Le testeur de l'institution a désactivé les avis de la sandbox (portail).
        mvc
            .perform(put("/api/v1/preferences").contentType(MediaType.APPLICATION_JSON).content("{\"types\":{\"SANDBOX\":false}}").with(interne(TESTEUR_99001)))
            .andExpect(status().isOk());
        String invitation = demande(UUID.randomUUID().toString(), "sbx-espace", "SBX_ESP_INVITATION_ADMINISTRATEUR", "\"%s\"".formatted(ADMIN_99001));
        String suspension = demande(
            UUID.randomUUID().toString(),
            "sbx-espace",
            "SBX_ESP_ESPACE_SUSPENDU",
            "\"ROLE:ADMINISTRATEUR_INSTITUTION\",\"ROLE:CHEF_DE_PROJET\",\"ROLE:TESTEUR\",\"ROLE:ADMINISTRATEUR_GIMAC\""
        );
        String sansRegle = demande(UUID.randomUUID().toString(), "sbx-raccordement", "SBX_RAC_MOTIF_INCONNU", "\"ROLE:ADMINISTRATEUR_INSTITUTION\"");
        publier("sbx-espace.notification-requested.v1", "99001", invitation, invitation, suspension);
        publier("sbx-raccordement.notification-requested.v1", "99001", sansRegle, sansRegle);
        consommateurs.getListenerContainer(DemandesSandboxConsumer.GROUPE).start();

        // Invitation : l'administrateur ; suspension : administrateur, chef de projet, administrateur du GIMAC (testeur :
        // préférence) ; rien pour la demande sans règle, mise une seule fois au rebut.
        assertThat(attendre("select count(*) from notification_utilisateur where type = 'SANDBOX'", 4)).isEqualTo(4);
        assertThat(attendre("select count(*) from rebut_notification_sandbox", 1)).isEqualTo(1);
        Thread.sleep(1000);
        assertThat(compter("select count(*) from notification_utilisateur where type = 'SANDBOX'")).isEqualTo(4);
        assertThat(compter("select count(*) from rebut_notification_sandbox")).isEqualTo(1);
        assertThat(jdbc.queryForMap("select service, modele, code_participant, motif from rebut_notification_sandbox"))
            .containsEntry("service", "sbx-raccordement")
            .containsEntry("modele", "SBX_RAC_MOTIF_INCONNU")
            .containsEntry("code_participant", "99001")
            .containsEntry("motif", "MODELE_SANS_REGLE");
        assertThat(compter("select count(*) from notification_utilisateur where destinataire_id = ?", TESTEUR_99001)).isZero();
        assertThat(compter("select count(*) from notification_utilisateur where destinataire_id = ?", ADMIN_99001)).isEqualTo(2);
        assertThat(jdbc.queryForMap("select titre, lien, reference_service, reference_type from notification_utilisateur where destinataire_id = ?", CHEF_99001))
            .containsEntry("titre", "Espace sandbox 99001 suspendu")
            .containsEntry("lien", "/portail")
            .containsEntry("reference_service", "sandbox")
            .containsEntry("reference_type", "SBX_ESP_ESPACE_SUSPENDU");
        assertThat(jdbc.queryForMap("select lien from notification_utilisateur where destinataire_id = ?", ADMIN_GIMAC)).containsEntry("lien", "/des/sandbox/espaces/99001");

        String notification = jdbc.queryForObject("select id::text from notification_utilisateur where destinataire_id = ?", String.class, CHEF_99001);
        relais.relayerJusquaEnvoi(notification);
        List<ConsumerRecord<String, String>> publies = relais.lire("notification.inapp", notification);
        assertThat(publies).isNotEmpty();
        JsonNode d = json.readTree(publies.get(0).value()).path("data");
        assertThat(d.path("type").asText()).isEqualTo("SANDBOX");
        assertThat(d.path("destinataires").get(0).asText()).isEqualTo(CHEF_99001);
        assertThat(d.path("referenceService").asText()).isEqualTo("sandbox");
    }

    @Test
    @DisplayName("Résolution à blanc et catalogue : SBX_HAB_ADMINISTRATION exigée, rien n'est créé, modèle sans règle 400")
    void resolutionABlanc() throws Exception {
        String corps = "{\"modele\":\"SBX_ESP_ESPACE_RETABLI\",\"destinataires\":[\"ROLE:ADMINISTRATEUR_INSTITUTION\",\"ROLE:ADMINISTRATEUR_GIMAC\"],\"codeParticipant\":\"99001\"}";
        mvc
            .perform(post("/api/v1/sandbox/resolution").contentType(MediaType.APPLICATION_JSON).content(corps).with(utilisateur(ADMIN_GIMAC, "SBX_HAB_ADMINISTRATION")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].compteId").value(ADMIN_99001))
            .andExpect(jsonPath("$[0].portail").value(true))
            .andExpect(jsonPath("$[1].lien").value("/des/sandbox/espaces/99001"));
        mvc
            .perform(post("/api/v1/sandbox/resolution").contentType(MediaType.APPLICATION_JSON).content("{\"modele\":\"INCONNU\"}").with(utilisateur(ADMIN_GIMAC, "SBX_HAB_ADMINISTRATION")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("NOT_REQUETE_INVALIDE"));
        mvc.perform(post("/api/v1/sandbox/resolution").contentType(MediaType.APPLICATION_JSON).content(corps).with(interne(ADMIN_GIMAC))).andExpect(status().isForbidden());
        mvc
            .perform(get("/api/v1/sandbox/modeles").with(utilisateur(ADMIN_GIMAC, "SBX_HAB_ADMINISTRATION")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].code").value("SBX_ESP_INVITATION_ADMINISTRATEUR"))
            .andExpect(jsonPath("$[0].portee").value("INSTITUTION"));
        assertThat(compter("select count(*) from notification_utilisateur")).isZero();
    }
}
