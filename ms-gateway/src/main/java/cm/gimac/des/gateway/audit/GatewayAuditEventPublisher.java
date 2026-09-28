package cm.gimac.des.gateway.audit;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Publie chaque opération MUTANTE traversant la Gateway vers le topic
 * {@code gateway.security.events}, que le service Audit consomme et scelle
 * dans le journal souverain immuable (chaînage SHA-256).
 *
 * <p>C'est le point de capture TRANSVERSE : toute écriture (POST/PUT/PATCH/DELETE)
 * vers n'importe lequel des microservices passe par l'ingress unique — on obtient
 * donc une couverture d'audit systémique en un seul endroit, sans instrumenter
 * chaque service. Les lectures (GET) ne sont pas journalisées ici (volume,
 * faible valeur probante) : seul le changement d'état importe au régulateur.</p>
 *
 * <p>Actif uniquement si {@code spring.kafka.bootstrap-servers} est défini —
 * la Gateway démarre normalement sans Kafka (dev local).</p>
 */
@ConditionalOnProperty(name = "gimac.passerelle.journal-decisions.actif", havingValue = "true")
@Component
@ConditionalOnProperty(prefix = "spring.kafka", name = "bootstrap-servers")
public class GatewayAuditEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(GatewayAuditEventPublisher.class);
    static final String TOPIC = "gateway.security.events";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public GatewayAuditEventPublisher(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Émission « fire-and-forget » : ne bloque jamais le thread Netty et n'échoue
     * jamais la requête cliente si Kafka est indisponible (l'audit ne doit pas
     * dégrader la disponibilité, mais toute perte est visible dans les logs).
     */
    public void publishMutation(String actor, String correlationId, String tenantId,
                                String method, String path, int status, String ip) {
        try {
            ObjectNode n = objectMapper.createObjectNode();
            // Champs consommés par KafkaEventBuilder (service Audit).
            n.put("actor", actor != null ? actor : "anonymous");
            if (correlationId != null && !correlationId.isBlank() && !"NO-CORRELATION-ID".equals(correlationId)) {
                n.put("correlationId", correlationId);
            }
            n.put("tenantId", tenantId != null && !tenantId.isBlank() ? tenantId : "default");
            n.put("timestamp", Instant.now().toString());
            n.put("severity", severityFor(status));
            // Détail de l'opération (conservé dans le payload canonique scellé).
            n.put("method", method);
            n.put("path", path);
            n.put("status", status);
            n.put("ipAddress", ip);

            String json = objectMapper.writeValueAsString(n);
            kafkaTemplate.send(TOPIC, json).whenComplete((res, ex) -> {
                if (ex != null) {
                    log.error("[{}] Échec publication audit {} {} -> journal souverain : {}",
                            correlationId, method, path, ex.getMessage());
                }
            });
        } catch (Exception e) {
            log.error("[{}] Construction de l'événement d'audit impossible : {}", correlationId, e.getMessage());
        }
    }

    /** 401/403 = alerte sécurité ; 5xx = critique ; 4xx = avertissement ; sinon info. */
    private static String severityFor(int status) {
        if (status == 401 || status == 403) return "SECURITY";
        if (status >= 500) return "CRITICAL";
        if (status >= 400) return "WARN";
        return "INFO";
    }
}
