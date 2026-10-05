package cm.gimac.des.pilotage.infrastructure.messaging;

import org.springframework.stereotype.Component;

/** Sujet d'agrégat d'un type d'événement (PI §0.4 et §0.5). */
@Component
public class TopicResolver {

    /** pilotage.task-created.v1 vers pilotage.task ; pilotage.report-submitted.v1 vers pilotage.report. */
    public String topicFor(String eventType) {
        String[] parts = eventType.split("\\.");
        if (parts.length < 3 || parts[1].indexOf('-') < 0) {
            throw new IllegalArgumentException("Type d'événement mal formé : " + eventType);
        }
        String aggregate = parts[1].substring(0, parts[1].indexOf('-'));
        return parts[0] + "." + aggregate;
    }
}
