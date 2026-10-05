package cm.gimac.des.homologation.infrastructure.client;

import cm.gimac.des.homologation.application.port.out.PilotageTachesPort;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Adaptateur de substitution de MS-PILOTAGE (développement, tests) : les tâches sont tenues en mémoire avec la
 * même idempotence que le service réel (origine.reference + responsable) ; une indisponibilité se simule.
 */
@Component
@ConditionalOnProperty(name = "gimac.adapters.pilotage-tasks", havingValue = "stub", matchIfMissing = true)
public class PilotageTachesStubAdapter implements PilotageTachesPort {

    /** Tâche tenue par le substitut : la demande de création et son état (OUVERTE, TERMINEE). */
    public record TacheTenue(Tache tache, String etat) {}

    private final Map<String, TacheTenue> taches = new ConcurrentHashMap<>();
    private final List<String> journal = java.util.Collections.synchronizedList(new ArrayList<>());
    private volatile boolean indisponible;

    static String cle(String reference, String responsableId) {
        return reference + "|" + responsableId;
    }

    public void indisponible(boolean i) {
        this.indisponible = i;
    }

    public Map<String, TacheTenue> taches() {
        return Map.copyOf(taches);
    }

    public List<String> journal() {
        return List.copyOf(journal);
    }

    public void vider() {
        taches.clear();
        journal.clear();
    }

    @Override
    public Resultat creer(Tache t) {
        if (indisponible) {
            return Resultat.INDISPONIBLE;
        }
        journal.add("CREER " + cle(t.origine().reference(), t.responsableId()));
        taches.putIfAbsent(cle(t.origine().reference(), t.responsableId()), new TacheTenue(t, "OUVERTE"));
        return Resultat.FAIT;
    }

    @Override
    public Resultat cloturer(Designation d) {
        return changer(d, "TERMINEE", "CLOTURER ");
    }

    @Override
    public Resultat rouvrir(Designation d) {
        return changer(d, "EN_COURS", "ROUVRIR ");
    }

    private Resultat changer(Designation d, String etat, String trace) {
        if (indisponible) {
            return Resultat.INDISPONIBLE;
        }
        journal.add(trace + cle(d.origine().reference(), d.responsableId()));
        taches.computeIfPresent(cle(d.origine().reference(), d.responsableId()), (k, t) -> new TacheTenue(t.tache(), etat));
        return Resultat.FAIT;
    }
}
