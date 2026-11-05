package cm.gimac.des.participant.domain.event;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Contrat de participant.subscription-changed.v1 : la charge publiée est exactement la charge d'exemple
 * contrats/participant.subscription-changed.v1.exemple.json, que SBX-CAMPAGNE, SBX-RACCORDEMENT et SBX-REFERENTIEL lisent
 * dans leurs propres tests de contrat (même fichier dans le dépôt de la sandbox), et chaque champ est décrit par le schéma.
 */
class ContratSouscriptionsSandboxTest {

    private final JsonMapper json = JsonMapper.builder().build();

    private JsonNode lire(String ressource) throws Exception {
        try (InputStream in = getClass().getResourceAsStream(ressource)) {
            return json.readTree(in);
        }
    }

    @Test
    @DisplayName("Charge publiée identique à l'exemple partagé, champs requis et décrits par le schéma, sujet participant.subscription")
    void chargeConformeAuSchemaEtALExemple() throws Exception {
        SubscriptionChangedPayload p = new SubscriptionChangedPayload(
            UUID.fromString("6f1c2a3e-3333-4a6b-9c1d-000000000003"),
            "99001",
            List.of("CRT.ATM", "ISO.ACC", "W2W"),
            1793836800000L,
            Instant.parse("2026-11-05T00:00:00Z")
        );
        JsonNode publie = json.readTree(json.writeValueAsString(p));
        assertThat(publie).isEqualTo(lire("/contrats/participant.subscription-changed.v1.exemple.json"));
        JsonNode schema = lire("/config/contrats/participant.subscription-changed.v1.schema.json");
        assertThat(schema.path("title").asString()).isEqualTo(SubscriptionChangedPayload.TYPE);
        java.util.Collection<String> decrits = schema.path("properties").propertyNames();
        assertThat(publie.propertyNames()).allMatch(decrits::contains);
        assertThat(schema.path("required").valueStream().map(JsonNode::asString)).allMatch(publie::has);
        assertThat(new cm.gimac.des.participant.infrastructure.messaging.TopicResolver().topicFor(SubscriptionChangedPayload.TYPE)).isEqualTo("participant.subscription");
    }
}
