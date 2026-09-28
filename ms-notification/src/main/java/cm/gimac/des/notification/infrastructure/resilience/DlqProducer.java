package cm.gimac.des.notification.infrastructure.resilience;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@ConditionalOnProperty(name = "gimac.notification.resilience.actif", havingValue = "true")
@Component
public class DlqProducer {

    private final StreamBridge streamBridge;

    public DlqProducer(StreamBridge streamBridge) {
        this.streamBridge = streamBridge;
    }

    public void sendToDlq(String originalTopic, Object payload, Throwable exception) {
        String stackTrace = getStackTraceAsString(exception);
        
        Message<?> message = MessageBuilder.withPayload(payload)
                .setHeader("x-original-topic", originalTopic)
                .setHeader("x-exception-message", exception.getMessage() != null ? exception.getMessage() : "Unknown error")
                .setHeader("x-exception-stacktrace", stackTrace.getBytes(StandardCharsets.UTF_8))
                .build();
                
        // Publie vers notifications.dlq
        streamBridge.send("notifications-dlq-out-0", message);
    }
    
    private String getStackTraceAsString(Throwable e) {
        StringBuilder sb = new StringBuilder();
        sb.append(e.toString()).append("\n");
        for (StackTraceElement element : e.getStackTrace()) {
            sb.append("\tat ").append(element.toString()).append("\n");
        }
        return sb.toString();
    }
}
