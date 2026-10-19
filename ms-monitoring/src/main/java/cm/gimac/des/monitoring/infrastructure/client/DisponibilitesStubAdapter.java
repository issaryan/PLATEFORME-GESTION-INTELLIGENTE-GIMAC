package cm.gimac.des.monitoring.infrastructure.client;

import cm.gimac.des.monitoring.application.port.out.DisponibilitesPort;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Substitut des disponibilités (développement, tests ; jamais en production) : absences posées par les tests. */
@Component
@ConditionalOnProperty(name = "gimac.adaptateurs.disponibilites", havingValue = "stub", matchIfMissing = true)
public class DisponibilitesStubAdapter implements DisponibilitesPort {

    private final List<Absence> absences = new CopyOnWriteArrayList<>();

    public void ajouter(Absence a) {
        absences.add(a);
    }

    public void effacer() {
        absences.clear();
    }

    @Override
    public List<Absence> absencesValidees(Instant du, Instant au) {
        return absences.stream().filter(a -> a.debut().isBefore(au) && a.fin().isAfter(du)).toList();
    }
}
