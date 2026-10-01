package cm.gimac.des.gateway.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;
import tools.jackson.databind.json.JsonMapper;

/** Publication des événements de sécurité de la passerelle (D-65) sur gateway.security.events. */
// Première simulation de KafkaTemplate (instrumentation Mockito) lente sur une machine chargée.
@org.junit.jupiter.api.Timeout(60)
class GatewayAuditEventPublisherTest {

    @SuppressWarnings("unchecked")
    final KafkaTemplate<String, String> kafka = mock(KafkaTemplate.class);

    @Test
    @DisplayName("Événement de connexion : type, acteur, statut, sévérité, adresse et détails")
    void evenement() {
        when(kafka.send(anyString(), anyString())).thenReturn(CompletableFuture.failedFuture(new IllegalStateException("courtier absent")));
        var p = new GatewayAuditEventPublisher(kafka, JsonMapper.builder().build());
        p.publishSecurityEvent("gateway.login-failed.v1", null, "10.0.0.1", 401, Map.of("identifiant", "ag***a"));
        p.publishSecurityEvent("gateway.login-succeeded.v1", "u-1", "10.0.0.1", 200, Map.of());
        p.publishMutation("u-1", "c-1", "", "POST", "/api/x", 503, "ip");
        ArgumentCaptor<String> json = ArgumentCaptor.forClass(String.class);
        verify(kafka, org.mockito.Mockito.times(3)).send(eq(GatewayAuditEventPublisher.TOPIC), json.capture());
        assertThat(json.getAllValues().get(0))
            .contains("\"eventType\":\"gateway.login-failed.v1\"")
            .contains("\"actor\":\"anonymous\"")
            .contains("\"severity\":\"SECURITY\"")
            .contains("\"identifiant\":\"ag***a\"");
        assertThat(json.getAllValues().get(1)).contains("\"actor\":\"u-1\"").contains("\"severity\":\"INFO\"");
        assertThat(json.getAllValues().get(2)).contains("\"severity\":\"CRITICAL\"");
    }

    @Test
    @DisplayName("Échec de construction : aucune exception propagée")
    void echec() {
        when(kafka.send(anyString(), anyString())).thenThrow(new IllegalStateException("x"));
        var p = new GatewayAuditEventPublisher(kafka, JsonMapper.builder().build());
        p.publishSecurityEvent("t", "a", "ip", 429, Map.of());
        p.publishMutation("a", null, null, "PUT", "/x", 404, "ip");
    }
}
