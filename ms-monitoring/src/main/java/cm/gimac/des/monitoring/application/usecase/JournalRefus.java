package cm.gimac.des.monitoring.application.usecase;

import cm.gimac.des.monitoring.application.port.in.Appelant;
import cm.gimac.des.monitoring.domain.Alert;
import cm.gimac.des.monitoring.infrastructure.messaging.outbox.OutboxPublisher;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tentatives refusées journalisées vers MS-AUDIT (MON-04, erreur « opérateur hors vacation ») : l'événement part dans
 * sa propre transaction, après celle de l'acte refusé (annulée), que le refus n'annule donc pas.
 */
@Component
public class JournalRefus {

    private static final Logger LOG = LoggerFactory.getLogger(JournalRefus.class);

    private final OutboxPublisher outbox;

    public JournalRefus(OutboxPublisher outbox) {
        this.outbox = outbox;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void acquittementRefuse(Appelant appelant, Alert a) {
        LOG.warn("Acquittement de {} refusé : {} n'est pas de la vacation en cours", a.getReference(), appelant.id());
        Map<String, Object> evt = new LinkedHashMap<>();
        evt.put("alerteId", a.getId());
        evt.put("reference", a.getReference());
        evt.put("par", appelant.id());
        evt.put("motif", "MON_HORS_VACATION");
        outbox.publish("Alert", a.getId(), "monitoring.alert-ack-refused.v1", evt);
    }
}
