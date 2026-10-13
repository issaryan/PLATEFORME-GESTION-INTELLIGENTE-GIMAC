package cm.gimac.des.participant.domain;

import java.util.EnumSet;
import java.util.UUID;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.generator.EventType;
import org.hibernate.generator.EventTypeSets;

/** Générateur de {@link IdentifiantAttribuable} : UUID aléatoire, ou celui déjà attribué à l'entité. */
public class IdentifiantAttribuableGenerateur implements BeforeExecutionGenerator {

    @Override
    public Object generate(SharedSessionContractImplementor session, Object owner, Object valeurCourante, EventType type) {
        return valeurCourante != null ? valeurCourante : UUID.randomUUID();
    }

    @Override
    public EnumSet<EventType> getEventTypes() {
        return EventTypeSets.INSERT_ONLY;
    }

    @Override
    public boolean allowAssignedIdentifiers() {
        return true;
    }
}
