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
 * tâche, pour qu'une clôture ne devance jamais la création ; un refus définitif (4xx) est consigné sans réessai. Une
 * création refusée (PIL_RESPONSABLE_INVALIDE : compte inactif ou inconnu) qui porte un compte de remontée devient une
 * tâche de réaffectation pour ce compte, le conducteur de la liaison (D-93 §1) : le travail n'est jamais perdu.
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
                    remonter(d, maintenant);
                }
                case INDISPONIBLE -> {
                    d.echec("MS-PILOTAGE indisponible", maintenant, MAXIMUM);
                    retenues.add(d.getCle());
                }
            }
        }
    }

    /** Remontée au conducteur d'une création refusée : tâche « Réaffecter » (même lien, référence suffixée /REFUS). */
    void remonter(DemandeTache d, Instant maintenant) {
        if (d.getNature() != DemandeTache.Nature.CREER || d.getEscaladeVers() == null) {
            return;
        }
        PilotageTachesPort.Tache t = JsonTexte.lire(d.getCorps(), PilotageTachesPort.Tache.class);
        if (d.getEscaladeVers().equals(t.responsableId())) {
            return;
        }
        PilotageTachesPort.Origine o = new PilotageTachesPort.Origine(t.origine().type(), t.origine().reference() + "/REFUS", t.origine().service(), t.origine().lien());
        String titre = "Réaffecter : " + t.titre();
        String description = t.responsableId() == null
            ? "MS-PILOTAGE a refusé la tâche à prendre (permissions " + t.permissionsRequises() + "). Désigner la personne qui tient la fonction " +
            "concernée. Tâche d'origine : " + t.description()
            : "MS-PILOTAGE a refusé la tâche destinée au compte " + t.responsableId() + " (compte inactif ou inconnu). Désigner une autre " +
            "personne pour la fonction concernée. Tâche d'origine : " + t.description();
        PilotageTachesPort.Tache r = new PilotageTachesPort.Tache(
            titre.length() > 200 ? titre.substring(0, 199) + "…" : titre,
            description.length() > 2000 ? description.substring(0, 1999) + "…" : description,
            t.echeance(),
            "HAUTE",
            d.getEscaladeVers(),
            t.createurId(),
            o
        );
        demandes.save(new DemandeTache(DemandeTache.Nature.CREER, o.reference() + "|" + d.getEscaladeVers(), JsonTexte.ecrire(r), maintenant));
        LOG.info("Tâche refusée {} remontée au conducteur {}", d.getCle(), d.getEscaladeVers());
    }

    private PilotageTachesPort.Resultat envoyer(DemandeTache d) {
        return switch (d.getNature()) {
            case CREER -> pilotage.creer(JsonTexte.lire(d.getCorps(), PilotageTachesPort.Tache.class));
            case CLOTURER -> pilotage.cloturer(JsonTexte.lire(d.getCorps(), PilotageTachesPort.Designation.class));
            case ROUVRIR -> pilotage.rouvrir(JsonTexte.lire(d.getCorps(), PilotageTachesPort.Designation.class));
        };
    }
}
