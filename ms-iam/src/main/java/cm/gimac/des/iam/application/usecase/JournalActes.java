package cm.gimac.des.iam.application.usecase;

import cm.gimac.des.iam.domain.compte.Acteur;
import cm.gimac.des.iam.domain.event.CompteAdministrePayload;
import cm.gimac.des.iam.domain.event.ConfigurationModifieePayload;
import cm.gimac.des.iam.infrastructure.messaging.outbox.OutboxPublisher;
import cm.gimac.des.iam.infrastructure.persistence.JournalAdministration;
import cm.gimac.des.iam.infrastructure.persistence.JournalAdministrationRepository;
import cm.gimac.des.iam.infrastructure.security.CorrelationContext;
import java.time.Clock;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Trace de chaque acte d'administration (D-62) : entrée du journal consultable depuis la console et
 * événement vers le registre d'audit, dans la même transaction.
 */
@Component
public class JournalActes {

    private final JournalAdministrationRepository journal;
    private final OutboxPublisher outbox;
    private final Clock clock;

    public JournalActes(JournalAdministrationRepository journal, OutboxPublisher outbox, Clock clock) {
        this.journal = journal;
        this.outbox = outbox;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void compte(Acteur acteur, String action, String compteId, String libelle, String motif, List<String> details) {
        journal.save(new JournalAdministration(clock.instant(), acteur.id(), acteur.nom(), action, compteId, libelle, motif, String.join(", ", details), CorrelationContext.current()));
        outbox.publish("account", compteId, CompteAdministrePayload.TYPE, new CompteAdministrePayload(action, compteId, libelle, acteur.id(), motif, details));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void configuration(Acteur acteur, String objet, String motif, List<String> champs) {
        journal.save(new JournalAdministration(clock.instant(), acteur.id(), acteur.nom(), "CONFIGURATION_" + objet, null, objet, motif, String.join(", ", champs), CorrelationContext.current()));
        outbox.publish("configuration", objet, ConfigurationModifieePayload.TYPE, new ConfigurationModifieePayload(objet, acteur.id(), motif, champs));
    }
}
