package cm.gimac.des.collaboration.infrastructure.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Signal « en train d'écrire » (D-72) : enveloppe CloudEvents sur collaboration.message, sans attente. */
class KafkaSignalEphemereTest {

    static final Instant T = Instant.parse("2026-10-02T08:00:00Z");
    final JsonMapper json = JsonMapper.builder().build();

    @SuppressWarnings("unchecked")
    final KafkaTemplate<String, String> kafka = mock(KafkaTemplate.class);

    final KafkaSignalEphemere signal = new KafkaSignalEphemere(kafka, json, new TopicResolver(), Clock.fixed(T, ZoneOffset.UTC), "ms-collaboration");

    @Test
    @DisplayName("Enveloppe : type, sujet, destinataires et auteur ; sujet Kafka collaboration.message")
    void enveloppe() {
        UUID c = UUID.randomUUID();
        when(kafka.send(anyString(), anyString(), anyString())).thenReturn(CompletableFuture.<SendResult<String, String>>completedFuture(null));
        signal.saisie(c, "moi", List.of("aminata"));
        ArgumentCaptor<String> valeur = ArgumentCaptor.forClass(String.class);
        verify(kafka).send(eq("collaboration.message"), eq(c.toString()), valeur.capture());
        JsonNode ce = json.readTree(valeur.getValue());
        assertThat(ce.get("type").asString()).isEqualTo("collaboration.message-typing.v1");
        assertThat(ce.get("subject").asString()).isEqualTo(c.toString());
        assertThat(ce.get("source").asString()).isEqualTo("/gimac/des/ms-collaboration");
        assertThat(ce.get("data").get("auteurId").asString()).isEqualTo("moi");
        assertThat(ce.get("data").get("destinataires").get(0).asString()).isEqualTo("aminata");
    }

    @Test
    @DisplayName("Échec du bus, immédiat ou différé : sans exception pour l'appelant")
    void echec() {
        when(kafka.send(anyString(), anyString(), anyString())).thenReturn(CompletableFuture.failedFuture(new IllegalStateException("bus")));
        assertThatCode(() -> signal.saisie(UUID.randomUUID(), "moi", List.of("aminata"))).doesNotThrowAnyException();
        when(kafka.send(anyString(), anyString(), anyString())).thenThrow(new IllegalStateException("bus"));
        assertThatCode(() -> signal.saisie(UUID.randomUUID(), "moi", List.of("aminata"))).doesNotThrowAnyException();
        verify(kafka, org.mockito.Mockito.times(2)).send(anyString(), anyString(), any(String.class));
    }
}
