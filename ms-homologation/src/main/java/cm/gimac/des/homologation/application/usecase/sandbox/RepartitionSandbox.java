package cm.gimac.des.homologation.application.usecase.sandbox;

import cm.gimac.des.homologation.application.usecase.ParcoursDepot;
import cm.gimac.des.homologation.infrastructure.persistence.RepartitionSandboxRepository;
import cm.gimac.des.homologation.repository.LiaisonRepository;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Répartition des exécutions d'une campagne relevée (D-121) pour la vue graphique de l'onglet sandbox du dossier :
 * verdicts par cas et chronologie par jour, comptés par la base. Les jours sont ceux du fuseau du GIMAC
 * (gimac.sandbox.fuseau, Africa/Douala par défaut) : une exécution de 23 h 30 tombe le jour affiché à l'écran.
 */
@Service
@Transactional(readOnly = true)
public class RepartitionSandbox {

    public record Cas(String identifiantCas, long executions, long favorables, long defavorables, long nonExecutes, long enAttente) {}

    public record Jour(String jour, long executions, long favorables, long defavorables) {}

    public record Repartition(String fuseau, List<Cas> parCas, List<Jour> parJour) {}

    private final LiaisonRepository liaisons;
    private final RepartitionSandboxRepository depot;
    private final String fuseau;

    public RepartitionSandbox(LiaisonRepository liaisons, RepartitionSandboxRepository depot, @Value("${gimac.sandbox.fuseau:Africa/Douala}") String fuseau) {
        this.liaisons = liaisons;
        this.depot = depot;
        this.fuseau = ZoneId.of(fuseau).getId();
    }

    public Repartition repartition(UUID liaisonId, String campagneId) {
        if (!liaisons.existsById(liaisonId)) {
            throw ParcoursDepot.introuvable(liaisonId);
        }
        List<Cas> cas = depot
            .parCas(liaisonId, campagneId)
            .stream()
            .map(c -> new Cas(c.getIdentifiantCas().isEmpty() ? null : c.getIdentifiantCas(), c.getExecutions(), c.getFavorables(), c.getDefavorables(), c.getNonExecutes(), c.getEnAttente()))
            .toList();
        // Les 366 jours les plus récents, rendus dans l'ordre du temps.
        List<Jour> jours = depot
            .parJour(liaisonId, campagneId, fuseau)
            .stream()
            .map(j -> new Jour(j.getJour(), j.getExecutions(), j.getFavorables(), j.getDefavorables()))
            .sorted(Comparator.comparing(Jour::jour))
            .toList();
        return new Repartition(fuseau, cas, jours);
    }
}
