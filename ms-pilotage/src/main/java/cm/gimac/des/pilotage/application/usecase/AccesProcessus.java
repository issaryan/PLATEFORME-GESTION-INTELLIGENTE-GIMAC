package cm.gimac.des.pilotage.application.usecase;

import cm.gimac.des.pilotage.application.port.in.Appelant;
import cm.gimac.des.pilotage.application.port.in.ProcessusUseCase.PorteeCatalogue;
import cm.gimac.des.pilotage.application.port.out.OrganisationPort.Portee;
import cm.gimac.des.pilotage.application.port.out.OrganisationPort.Pouvoir;
import cm.gimac.des.pilotage.application.port.out.OrganisationPort.Pouvoirs;
import cm.gimac.des.pilotage.domain.exception.PilotageException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Droits des processus d'unité (D-87 §1, §2 ; D-86 §4 ; D-92 ; D-94) : définir et publier = PROCESSUS_DEFINIR
 * couvrant l'unité (portée UNITES et unité dirigée) ; lancer = membre de l'unité (sous-unités comprises) ou
 * PROCESSUS_LANCER couvrant l'unité ; lire = membres, responsables de l'unité et porteurs de ces pouvoirs ; piloter une instance (réaffecter, suspendre, reprendre, annuler) = chef de
 * l'unité (unité dirigée, D-81 §2) ou TACHE_CONFIER couvrant l'unité. Toutes les unités, actives ou non (D-92) :
 * la permission REF_METIER_ADMIN du jeton donne tout ; la portée TOUTES de la matrice donne, sur PROCESSUS_DEFINIR,
 * la définition (et la lecture), sur PROCESSUS_LANCER, le lancement, le pilotage des instances (et la lecture).
 * Aucun nom de profil n'intervient : seuls le jeton et la matrice décident. D-94 : l'ancien repli « IAM_PROFILE_EDIT
 * vaut toutes les unités » est retiré ; l'administration des comptes n'ouvre pas les processus des autres unités.
 * Refus : 403 PIL_PROCESSUS_INTERDIT.
 */
@Component
class AccesProcessus {

    static final String REF_METIER_ADMIN = "REF_METIER_ADMIN";
    static final String TOUS_DOMAINES = "TOUS_DOMAINES";
    static final String MES_UNITES = "MES_UNITES";

    /** Acte soumis à un droit, pour la mention du journal. */
    enum Acte {
        DEFINIR,
        LANCER,
        PILOTER,
    }

    private final Equipes equipes;

    AccesProcessus(Equipes equipes) {
        this.equipes = equipes;
    }

    /** Permission REF_METIER_ADMIN (administration des référentiels métier, tous domaines, D-92). */
    boolean referentielsMetier(Appelant a) {
        return a.permissions().contains(REF_METIER_ADMIN);
    }

    private boolean toutes(Appelant a, Pouvoir p) {
        return equipes.pouvoirs(a.id()).toutes(p);
    }

    /** Définit les processus de toutes les unités : REF_METIER_ADMIN ou PROCESSUS_DEFINIR à TOUTES. */
    boolean definitToutes(Appelant a) {
        return referentielsMetier(a) || toutes(a, Pouvoir.PROCESSUS_DEFINIR);
    }

    /** Lance et pilote les processus de toutes les unités : REF_METIER_ADMIN ou PROCESSUS_LANCER à TOUTES. */
    boolean lanceToutes(Appelant a) {
        return referentielsMetier(a) || toutes(a, Pouvoir.PROCESSUS_LANCER);
    }

    /** Lit les processus et les instances de toutes les unités. */
    boolean litToutes(Appelant a) {
        return definitToutes(a) || lanceToutes(a);
    }

    boolean definit(Appelant a, String unite) {
        return referentielsMetier(a) || equipes.couvreUnite(a, Pouvoir.PROCESSUS_DEFINIR, unite);
    }

    boolean lance(Appelant a, String unite) {
        return referentielsMetier(a) || equipes.dansUnite(a.id(), unite) || equipes.couvreUnite(a, Pouvoir.PROCESSUS_LANCER, unite);
    }

    boolean lit(Appelant a, String unite) {
        return (
            referentielsMetier(a) ||
            equipes.dansUnite(a.id(), unite) ||
            equipes.pouvoirs(a.id()).dirige(unite) ||
            definit(a, unite) ||
            lance(a, unite)
        );
    }

    boolean pilote(Appelant a, String unite) {
        return (
            referentielsMetier(a) ||
            equipes.pouvoirs(a.id()).dirige(unite) ||
            toutes(a, Pouvoir.PROCESSUS_LANCER) ||
            equipes.couvreUnite(a, Pouvoir.TACHE_CONFIER, unite)
        );
    }

    /** Unités dont l'appelant lit les processus : son chemin d'unités et ses unités dirigées. */
    List<String> unitesLisibles(Appelant a) {
        return equipes.unitesDe(a.id());
    }

    /** Portée du catalogue pour l'interface (bandeau « Administration de tous les domaines », choix des unités). */
    PorteeCatalogue portee(Appelant a) {
        Pouvoirs p = equipes.pouvoirs(a.id());
        List<String> titres = new ArrayList<>();
        if (referentielsMetier(a)) {
            titres.add(REF_METIER_ADMIN);
        }
        if (p.toutes(Pouvoir.PROCESSUS_DEFINIR)) {
            titres.add(Pouvoir.PROCESSUS_DEFINIR.name() + ":" + Portee.TOUTES.name());
        }
        if (p.toutes(Pouvoir.PROCESSUS_LANCER)) {
            titres.add(Pouvoir.PROCESSUS_LANCER.name() + ":" + Portee.TOUTES.name());
        }
        boolean definit = definitToutes(a);
        boolean lance = lanceToutes(a);
        boolean tous = definit || lance;
        List<String> unites = tous ? List.of() : unitesLisibles(a);
        List<String> definissables = definit
            ? List.of()
            : p.portee(Pouvoir.PROCESSUS_DEFINIR) == Portee.UNITES ? p.unitesDirigees() : List.of();
        return new PorteeCatalogue(tous ? TOUS_DOMAINES : MES_UNITES, definit, lance, titres, unites, definissables);
    }

    /**
     * Mention du droit utilisé pour le journal (D-92 §2 : acteur, motif et droit) : vide si la règle de l'unité
     * suffit (chef, membre, portée UNITES sur une unité dirigée) ; sinon « (au titre de REF_METIER_ADMIN) », de
     * « PROCESSUS_DEFINIR portée TOUTES » ou de « PROCESSUS_LANCER portée TOUTES ».
     */
    String mention(Appelant a, String unite, Acte acte) {
        Pouvoirs p = equipes.pouvoirs(a.id());
        boolean regleUnite = switch (acte) {
            case DEFINIR -> p.portee(Pouvoir.PROCESSUS_DEFINIR) == Portee.UNITES && p.dirige(unite);
            case LANCER -> equipes.dansUnite(a.id(), unite) || (p.portee(Pouvoir.PROCESSUS_LANCER) == Portee.UNITES && p.dirige(unite));
            case PILOTER -> p.dirige(unite) || equipes.couvreUnite(a, Pouvoir.TACHE_CONFIER, unite);
        };
        if (regleUnite) {
            return "";
        }
        if (referentielsMetier(a)) {
            return " (au titre de " + REF_METIER_ADMIN + ")";
        }
        Pouvoir pouvoir = acte == Acte.DEFINIR ? Pouvoir.PROCESSUS_DEFINIR : Pouvoir.PROCESSUS_LANCER;
        if (p.toutes(pouvoir)) {
            return " (au titre de " + pouvoir.name() + " portée TOUTES)";
        }
        return "";
    }

    static PilotageException interdit(String message) {
        return PilotageException.interdit("PIL_PROCESSUS_INTERDIT", message);
    }
}
