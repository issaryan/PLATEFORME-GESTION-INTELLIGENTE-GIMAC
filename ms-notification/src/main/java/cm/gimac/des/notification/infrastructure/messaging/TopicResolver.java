package cm.gimac.des.notification.infrastructure.messaging;

import org.springframework.stereotype.Component;

/** Sujet d'agrégat d'un type d'événement (PI §0.4 et §0.5). */
@Component
public class TopicResolver {

    /** notification.inapp-created.v1 vers notification.inapp (docs/lot1/notifications.md §1). */
    public String topicFor(String eventType) {
        String[] parts = eventType.split("\\.");
        if (parts.length < 3 || parts[1].indexOf('-') < 0) {
            throw new IllegalArgumentException("Type d'événement mal formé : " + eventType);
        }
        String aggregate = parts[1].substring(0, parts[1].indexOf('-'));
        return parts[0] + "." + aggregate;
    }
}
