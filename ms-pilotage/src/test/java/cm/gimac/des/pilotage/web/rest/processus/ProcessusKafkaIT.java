package cm.gimac.des.pilotage.web.rest.processus;

import static cm.gimac.des.pilotage.infrastructure.client.OrganisationStubAdapter.CHEF_RESEAU;
import static cm.gimac.des.pilotage.infrastructure.client.OrganisationStubAdapter.INGENIEUR_RESEAU;
import static cm.gimac.des.pilotage.infrastructure.client.OrganisationStubAdapter.TECHNICIEN_RESEAU;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cm.gimac.des.pilotage.RelaisOutboxDeTest;
import cm.gimac.des.pilotage.infrastructure.client.OrganisationStubAdapter.Compte;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;

/**
 * Processus d'unité et Kafka (D-87) : les événements pilotage.process-* partent par l'outbox vers le sujet
 * pilotage.process en CloudEvents 1.0 ; le départ d'un compte (iam.account-administered.v1, action DESACTIVATION,
 * sujet iam.account) remet ses étapes à prendre par le profil de remplacement et avertit le chef de l'unité,
 * une seule fois par événement.
 */
class ProcessusKafkaIT extends AbstractProcessusIT {

    @Autowired
    private RelaisOutboxDeTest relais;

    @Autowired
    private KafkaTemplate<String, String> kafka;

    private String instanceAvecMiseEnOeuvre() throws Exception {
        String processus = publie("FLUX_KAFKA_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(), CHEF_RESEAU);
        JsonNode i = lire(
            mvc
                .perform(
                    json(
                        post(BASE + "/" + processus + "/instances").with(agent(TECHNICIEN_RESEAU)),
                        Map.of("intitule", "Flux Kafka", "designations", Map.of("MISE_EN_OEUVRE", INGENIEUR_RESEAU))
                    )
                )
                .andExpect(status().isCreated())
                .andReturn()
        );
        return i.get("id").asText();
    }

    private void envoyer(String eventId, String action, String compte) throws Exception {
        String message = json.writeValueAsString(
            Map.of(
                "specversion",
                "1.0",
                "id",
                eventId,
                "source",
                "/gimac/des/iam",
                "type",
                "iam.account-administered.v1",
                "subject",
                compte,
                "data",
                Map.of("action", action, "compteId", compte, "identifiant", "agent", "acteurId", "admin-1", "details", List.of())
            )
        );
        kafka.send("iam.account", compte, message).get(10, TimeUnit.SECONDS);
    }

    private void attendre(String sql, Object arg, int attendu) throws InterruptedException {
        long fin = System.nanoTime() + Duration.ofSeconds(30).toNanos();
        while (System.nanoTime() < fin) {
            if (compter(sql, arg) >= attendu) {
                return;
            }
            Thread.sleep(300);
        }
        throw new AssertionError("Condition non atteinte : " + sql);
    }

    @Test
    @DisplayName("Événements pilotage.process-* relayés vers Kafka en CloudEvents (clé = instance)")
    void evenementsRelayes() throws Exception {
        String id = instanceAvecMiseEnOeuvre();
        relais.relayerJusquaEnvoi(id);
        List<ConsumerRecord<String, String>> recus = relais.lire("pilotage.process", id);
        assertThat(recus).isNotEmpty();
        JsonNode e = json.readTree(recus.get(0).value());
        assertThat(e.get("specversion").asText()).isEqualTo("1.0");
        assertThat(e.get("type").asText()).startsWith("pilotage.process-");
        assertThat(e.get("subject").asText()).isEqualTo(id);
        assertThat(e.at("/data/instanceId").asText()).isEqualTo(id);
    }

    @Test
    @DisplayName("Départ d'un compte : étapes en cours reprises par le profil de remplacement (seule porteuse : à elle), chef averti, idempotent")
    void depart() throws Exception {
        String id = instanceAvecMiseEnOeuvre();
        // DEMANDE (lanceur) terminée par la voie normale serait longue : on fait partir le lanceur, exécutant de DEMANDE.
        organisation.enregistrer(new Compte(TECHNICIEN_RESEAU, "Yves Ateba", "Technicien réseau", "POLE_RESEAU", CHEF_RESEAU, false, true));
        String evenement = UUID.randomUUID().toString();
        envoyer(UUID.randomUUID().toString(), "ACTIVATION", TECHNICIEN_RESEAU);
        envoyer(evenement, "DESACTIVATION", TECHNICIEN_RESEAU);
        attendre("select count(*) from processus_historique where instance_id = ?::uuid and action = 'DEPART'", id, 1);
        attendre("select count(*) from processed_event where event_id = ?::uuid", evenement, 1);

        JsonNode i = lire(mvc.perform(get(INSTANCES + "/" + id).with(chef(CHEF_RESEAU))).andExpect(status().isOk()).andReturn());
        // Seule porteuse active restante du profil de remplacement dans l'unité : l'étape lui est confiée.
        JsonNode demande = etape(i, "DEMANDE");
        assertThat(demande.at("/executant/id").asText()).isEqualTo(INGENIEUR_RESEAU);
        assertThat(demande.get("aPrendre").isNull()).isTrue();
        mvc.perform(get("/api/v1/taches/" + demande.get("tacheId").asText()).with(chef(CHEF_RESEAU))).andExpect(jsonPath("$.responsable.id").value(INGENIEUR_RESEAU));
        JsonNode reaffectee = charge("pilotage.process-step-reassigned.v1", id);
        assertThat(reaffectee.get("cause").asText()).isEqualTo("DEPART");
        assertThat(reaffectee.get("libelleEtape").asText()).isEqualTo(etape(i, reaffectee.get("etape").asText()).get("libelle").asText()).isNotBlank();
        assertThat(reaffectee.get("ancienId").asText()).isEqualTo(TECHNICIEN_RESEAU);
        assertThat(reaffectee.get("executantId").asText()).isEqualTo(INGENIEUR_RESEAU);
        assertThat(reaffectee.get("chefId").asText()).isEqualTo(CHEF_RESEAU);
        assertThat(textes(reaffectee.get("destinataires"))).containsExactlyInAnyOrder(INGENIEUR_RESEAU, CHEF_RESEAU);

        // Rejeu du même événement : sans effet.
        envoyer(evenement, "DESACTIVATION", TECHNICIEN_RESEAU);
        Thread.sleep(2000);
        assertThat(compter("select count(*) from processus_historique where instance_id = ?::uuid and action = 'DEPART'", id)).isEqualTo(1);

        // L'ingénieure poursuit l'étape.
        i = lire(mvc.perform(get(INSTANCES + "/" + id).with(agent(INGENIEUR_RESEAU))).andExpect(status().isOk()).andReturn());
        assertThat(etape(i, "DEMANDE").at("/moi/executer").asBoolean()).isTrue();
    }

    @Test
    @DisplayName("D-93 : profils remplacés (iam.account) et unité modifiée (iam.configuration) consommés une fois")
    void changementsOrganisationConsommes() throws Exception {
        String id = instanceAvecMiseEnOeuvre();
        String profils = UUID.randomUUID().toString();
        envoyer(profils, "PROFILS", TECHNICIEN_RESEAU);
        attendre("select count(*) from processed_event where event_id = ?::uuid", profils, 1);
        String unite = UUID.randomUUID().toString();
        String message = json.writeValueAsString(
            Map.of(
                "specversion",
                "1.0",
                "id",
                unite,
                "source",
                "/gimac/des/iam",
                "type",
                "iam.configuration-changed.v1",
                "subject",
                "UNITE_ORGANISATIONNELLE",
                "data",
                Map.of("objet", "UNITE_ORGANISATIONNELLE", "acteurId", "admin-1", "champs", List.of("MODIFICATION", "code=POLE_RESEAU", "responsable=" + CHEF_RESEAU))
            )
        );
        kafka.send("iam.configuration", "UNITE_ORGANISATIONNELLE", message).get(10, TimeUnit.SECONDS);
        attendre("select count(*) from processed_event where event_id = ?::uuid", unite, 1);
        // Le lanceur reste exécutant de DEMANDE (désigné par son rôle de lanceur) : aucune réaffectation.
        assertThat(compter("select count(*) from processus_historique where instance_id = ?::uuid and action = 'ORGANISATION'", id)).isZero();
    }
}
