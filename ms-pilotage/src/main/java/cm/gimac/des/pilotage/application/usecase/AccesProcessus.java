package cm.gimac.des.pilotage.application.usecase;

import cm.gimac.des.pilotage.application.port.in.Appelant;
import cm.gimac.des.pilotage.application.port.out.OrganisationPort.Pouvoir;
import cm.gimac.des.pilotage.domain.exception.PilotageException;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Droits des processus d'unité (D-87 §1, §2 ; D-86 §4) : définir et publier = PROCESSUS_DEFINIR couvrant l'unité
 * (portée UNITES et unité dirigée) ou IAM_PROFILE_EDIT (administration) ; lancer = membre de l'unité (sous-unités
 * comprises) ou PROCESSUS_LANCER couvrant l'unité ; lire = membres, responsables de l'unité, porteurs de ces
 * pouvoirs et administration ; piloter une instance (réaffecter, suspendre, reprendre, annuler) = chef de l'unité
 * (unité dirigée, D-81 §2) ou TACHE_CONFIER couvrant l'unité. Refus : 403 PIL_PROCESSUS_INTERDIT.
 */
@Component
class AccesProcessus {

    static final String ADMINISTRATION = "IAM_PROFILE_EDIT";

    private final Equipes equipes;

    AccesProcessus(Equipes equipes) {
        this.equipes = equipes;
    }

    boolean administration(Appelant a) {
        return a.permissions().contains(ADMINISTRATION);
    }

    boolean definit(Appelant a, String unite) {
        return administration(a) || equipes.couvreUnite(a, Pouvoir.PROCESSUS_DEFINIR, unite);
    }

    boolean lance(Appelant a, String unite) {
        return equipes.dansUnite(a.id(), unite) || equipes.couvreUnite(a, Pouvoir.PROCESSUS_LANCER, unite);
    }

    boolean lit(Appelant a, String unite) {
        return administration(a) || equipes.dansUnite(a.id(), unite) || equipes.pouvoirs(a.id()).dirige(unite) || definit(a, unite) || lance(a, unite);
    }

    boolean pilote(Appelant a, String unite) {
        return equipes.pouvoirs(a.id()).dirige(unite) || equipes.couvreUnite(a, Pouvoir.TACHE_CONFIER, unite);
    }

    /** Unités dont l'appelant lit les processus : son chemin d'unités et ses unités dirigées. */
    List<String> unitesLisibles(Appelant a) {
        return equipes.unitesDe(a.id());
    }

    static PilotageException interdit(String message) {
        return PilotageException.interdit("PIL_PROCESSUS_INTERDIT", message);
    }
}
