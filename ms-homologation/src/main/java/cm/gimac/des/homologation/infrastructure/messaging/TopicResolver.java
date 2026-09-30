package cm.gimac.des.homologation.infrastructure.messaging;

import java.util.Map;
import org.springframework.stereotype.Component;

/** Sujet d'agrégat d'un type d'événement (PI §0.4 et §0.5). */
@Component
public class TopicResolver {

    /**
     * Sujets fixés par docs/lot1/parcours-backend.md §3, qui priment sur la règle générale : le constat de
     * la couverture part sur le sujet de la liaison, lu par MS-CERTIFICATION.
     */
    static final Map<String, String> SUJETS_FIXES = Map.of("homologation.coverage-established.v1", "homologation.liaison");

    /** homologation.operation-closed.v1 vers homologation.operation ; certification.quitus-signed.v1 vers certification.quitus. */
    public String topicFor(String eventType) {
        String fixe = SUJETS_FIXES.get(eventType);
        if (fixe != null) {
            return fixe;
        }
        String[] parts = eventType.split("\\.");
        if (parts.length < 3 || parts[1].indexOf('-') < 0) {
            throw new IllegalArgumentException("Type d'événement mal formé : " + eventType);
        }
        String aggregate = parts[1].substring(0, parts[1].indexOf('-'));
        return parts[0] + "." + aggregate;
    }
}
