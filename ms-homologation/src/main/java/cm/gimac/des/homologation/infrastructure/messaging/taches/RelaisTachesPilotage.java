package cm.gimac.des.homologation.infrastructure.messaging.taches;

import cm.gimac.des.homologation.application.port.out.PilotageTachesPort;
import cm.gimac.des.homologation.domain.JsonTexte;
import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Relais des demandes de tâches vers MS-PILOTAGE (docs/lot1/modelisateur-processus.md §5) : dans l'ordre
 * d'écriture ; un échec transitoire repousse la demande (délai croissant) et retient les suivantes de la même
 * tâche, pour qu'une clôture ne devance jamais la création ; un refus définitif (4xx) est consigné sans réessai.
 */
@Component
public class RelaisTachesPilotage {

    private static final Logger LOG = LoggerFactory.getLogger(RelaisTachesPilotage.class);
    static final int LOT = 50;
    static final int MAXIMUM = 12;

    private final DemandeTacheRepository demandes;
    private final PilotageTachesPort pilotage;
    private final Clock clock;

    public RelaisTachesPilotage(DemandeTacheRepository demandes, PilotageTachesPort pilotage, Clock clock) {
        this.demandes = demandes;
        this.pilotage = pilotage;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${gimac.taches.delai:2000}", initialDelayString = "${gimac.taches.delai-initial:10000}")
    @Transactional
    public void relayer() {
        Instant maintenant = clock.instant();
        Set<String> retenues = new HashSet<>();
        for (DemandeTache d : demandes.verrouillerEnAttente(LOT)) {
            if (retenues.contains(d.getCle())) {
                continue;
            }
            if (d.getProchainEssai().isAfter(maintenant)) {
                retenues.add(d.getCle());
                continue;
            }
            PilotageTachesPort.Resultat r;
            try {
                r = envoyer(d);
            } catch (RuntimeException e) {
                r = PilotageTachesPort.Resultat.INDISPONIBLE;
                LOG.warn("Demande de tâche {} ({}) en échec : {}", d.getNumero(), d.getNature(), e.toString());
            }
            switch (r) {
                case FAIT -> d.traitee(DemandeTache.Statut.ENVOYEE, maintenant);
                case REFUSE -> {
                    d.traitee(DemandeTache.Statut.REFUSEE, maintenant);
                    LOG.warn("Demande de tâche {} ({}) refusée par MS-PILOTAGE ; clé {}", d.getNumero(), d.getNature(), d.getCle());
                }
                case INDISPONIBLE -> {
                    d.echec("MS-PILOTAGE indisponible", maintenant, MAXIMUM);
                    retenues.add(d.getCle());
                }
            }
        }
    }

    private PilotageTachesPort.Resultat envoyer(DemandeTache d) {
        return switch (d.getNature()) {
            case CREER -> pilotage.creer(JsonTexte.lire(d.getCorps(), PilotageTachesPort.Tache.class));
            case CLOTURER -> pilotage.cloturer(JsonTexte.lire(d.getCorps(), PilotageTachesPort.Designation.class));
            case ROUVRIR -> pilotage.rouvrir(JsonTexte.lire(d.getCorps(), PilotageTachesPort.Designation.class));
        };
    }
}
