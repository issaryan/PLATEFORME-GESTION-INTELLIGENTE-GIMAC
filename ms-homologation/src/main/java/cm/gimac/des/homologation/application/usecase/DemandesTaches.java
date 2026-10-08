package cm.gimac.des.homologation.application.usecase;

import cm.gimac.des.homologation.application.port.out.PilotageTachesPort;
import cm.gimac.des.homologation.domain.JsonTexte;
import cm.gimac.des.homologation.infrastructure.messaging.taches.DemandeTache;
import cm.gimac.des.homologation.infrastructure.messaging.taches.DemandeTacheRepository;
import java.time.Clock;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Écriture des demandes de tâches pour MS-PILOTAGE dans la transaction du geste (docs/lot1/modelisateur-processus.md
 * §5) ; le relais asynchrone les transmet. Le geste métier n'attend jamais MS-PILOTAGE.
 */
@Component
public class DemandesTaches {

    public static final String SERVICE = "homologation";

    private final DemandeTacheRepository demandes;
    private final Clock clock;

    public DemandesTaches(DemandeTacheRepository demandes, Clock clock) {
        this.demandes = demandes;
        this.clock = clock;
    }

    static String cle(PilotageTachesPort.Origine o, String responsableId) {
        return o.reference() + "|" + responsableId;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void creer(PilotageTachesPort.Tache t) {
        creer(t, null);
    }

    /**
     * Création avec remontée (D-93 §1) : si MS-PILOTAGE refuse la tâche (PIL_RESPONSABLE_INVALIDE : compte inactif ou
     * inconnu), le relais crée une tâche de réaffectation pour {@code escalade} (le conducteur de la liaison).
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void creer(PilotageTachesPort.Tache t, String escalade) {
        DemandeTache d = new DemandeTache(DemandeTache.Nature.CREER, cle(t.origine(), t.responsableId()), JsonTexte.ecrire(t), clock.instant());
        d.escaladerVers(escalade == null || escalade.equals(t.responsableId()) ? null : escalade);
        demandes.save(d);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void cloturer(PilotageTachesPort.Designation d) {
        demandes.save(new DemandeTache(DemandeTache.Nature.CLOTURER, cle(d.origine(), d.responsableId()), JsonTexte.ecrire(d), clock.instant()));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void rouvrir(PilotageTachesPort.Designation d) {
        demandes.save(new DemandeTache(DemandeTache.Nature.ROUVRIR, cle(d.origine(), d.responsableId()), JsonTexte.ecrire(d), clock.instant()));
    }
}
