package cm.gimac.des.pilotage.application.usecase;

import cm.gimac.des.pilotage.application.port.in.Appelant;
import cm.gimac.des.pilotage.application.port.in.ProcessusUseCase.PorteeCatalogue;
import cm.gimac.des.pilotage.application.port.out.OrganisationPort.Portee;
import cm.gimac.des.pilotage.application.port.out.OrganisationPort.Pouvoir;
import cm.gimac.des.pilotage.application.port.out.OrganisationPort.Pouvoirs;
import cm.gimac.des.pilotage.domain.exception.PilotageException;
import cm.gimac.des.pilotage.domain.processus.Initiateurs;
import cm.gimac.des.pilotage.domain.processus.Processus;
import cm.gimac.des.pilotage.domain.processus.RegleInitiateur;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Droits des processus d'unité (D-87 §1, §2 ; D-86 §4 ; D-92 ; D-94 ; D-100) : définir et publier = PROCESSUS_DEFINIR
 * couvrant l'unité (portée UNITES et unité dirigée) ; lancer une version publiée (et la voir dans « Lancer une
 * demande ») = faire partie de son public, défini par son créateur dans la définition (règles d'initiateurs, D-100),
 * sans aucun passe-droit ; lire = membres, responsables de l'unité et porteurs des pouvoirs de l'unité ; piloter une
 * instance (réaffecter, suspendre, reprendre, annuler) = chef de l'unité (unité dirigée, D-81 §2) ou TACHE_CONFIER
 * couvrant l'unité. Toutes les unités, actives ou non (D-92) : la permission REF_METIER_ADMIN du jeton donne la
 * définition, la lecture et le pilotage ; la portée TOUTES de la matrice donne, sur PROCESSUS_DEFINIR, la définition (et
 * la lecture), sur PROCESSUS_LANCER, le pilotage des instances (et la lecture). D-100 : ni REF_METIER_ADMIN, ni
 * PROCESSUS_LANCER (portée TOUTES ou couvrant l'unité) n'ouvrent plus le lancement : un administrateur ne voit dans
 * « Lancer une demande » que les processus dont il fait partie du public. Aucun nom de profil n'intervient : seuls le
 * jeton, la matrice et la définition décident. D-94 : l'ancien repli « IAM_PROFILE_EDIT vaut toutes les unités » est
 * retiré ; l'administration des comptes n'ouvre pas les processus des autres unités. Refus : 403 PIL_PROCESSUS_INTERDIT
 * (définition, lecture, pilotage) et 403 PIL_PROCESSUS_NON_INITIATEUR (lancement).
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

    /**
     * Lit et pilote les instances de toutes les unités : REF_METIER_ADMIN ou PROCESSUS_LANCER à TOUTES. D-100 : n'ouvre
     * pas le lancement, qui relève du seul public de chaque processus ({@link #lance(Appelant, Processus)}).
     */
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

    /**
     * Rattachement à l'unité au titre du lancement, pour la seule lecture des définitions et des instances de l'unité
     * ({@link #lit}) : membre, PROCESSUS_LANCER couvrant l'unité ou REF_METIER_ADMIN. N'ouvre pas le lancement d'une
     * version (D-100 : {@link #lance(Appelant, Processus)}).
     */
    boolean lance(Appelant a, String unite) {
        return referentielsMetier(a) || equipes.dansUnite(a.id(), unite) || equipes.couvreUnite(a, Pouvoir.PROCESSUS_LANCER, unite);
    }

    /**
     * Voir et lancer cette version (D-98 §1, D-100) : l'appelant fait partie du public que le créateur a défini, c'est-à-dire
     * satisfait l'une des règles d'initiateurs de la définition, et rien d'autre. REF_METIER_ADMIN, PROCESSUS_LANCER
     * (portée TOUTES ou couvrant l'unité) n'y ajoutent rien : ils gardent la définition, la lecture et le pilotage.
     * Seul juge du catalogue « Lancer une demande », du lancement, de la lecture d'une version publiée par un non-lecteur
     * de l'unité et de {@code droits.lancer}.
     */
    boolean lance(Appelant a, Processus p) {
        return initiateur(a, p.getUnite(), p.getInitiateurs());
    }

    /**
     * L'appelant satisfait l'une des règles d'initiateurs : membre de l'unité propriétaire ou l'un de ses responsables
     * (MEMBRES_UNITE, D-100 : le chef qui dirige l'unité ou l'une de ses unités parentes), compte interne actif
     * (TOUT_LE_GIMAC), membre d'une unité désignée (UNITES), porteur actif d'un profil désigné (PROFILS), porteur d'une
     * permission désignée dans son jeton (PERMISSIONS), personne désignée nommément et compte interne actif (PERSONNES).
     */
    boolean initiateur(Appelant a, String unite, Initiateurs i) {
        for (RegleInitiateur r : i.regles()) {
            boolean ok = switch (r) {
                case MEMBRES_UNITE -> equipes.dansUnite(a.id(), unite) || equipes.pouvoirs(a.id()).dirige(unite);
                case TOUT_LE_GIMAC -> equipes.interneActif(a.id());
                case UNITES -> i.unites().stream().anyMatch(u -> equipes.dansUnite(a.id(), u));
                case PROFILS -> !i.profils().isEmpty() && equipes.interneActif(a.id()) && equipes.profilsDe(a.id()).stream().anyMatch(i.profils()::contains);
                case PERMISSIONS -> i.permissions().stream().anyMatch(a.permissions()::contains);
                case PERSONNES -> i.personnes().contains(a.id()) && equipes.interneActif(a.id());
            };
            if (ok) {
                return true;
            }
        }
        return false;
    }

    /**
     * Refus explicite d'un lancement (D-98 §1, D-100) : 403 PIL_PROCESSUS_NON_INITIATEUR, le public défini par le
     * créateur dans le message et en détail (règles, unités, profils, permissions, personnes désignées).
     */
    static PilotageException nonInitiateur(Processus p) {
        Initiateurs i = p.getInitiateurs();
        List<String> qui = new ArrayList<>();
        for (RegleInitiateur r : i.regles()) {
            qui.add(
                switch (r) {
                    case MEMBRES_UNITE -> "les membres et les responsables de l'unité " + p.getUnite();
                    case TOUT_LE_GIMAC -> "tout compte interne actif du GIMAC";
                    case UNITES -> "les membres des unités " + String.join(", ", i.unites());
                    case PROFILS -> "les porteurs des profils " + String.join(", ", i.profils());
                    case PERMISSIONS -> "les porteurs des permissions " + String.join(", ", i.permissions());
                    case PERSONNES -> i.personnes().size() == 1 ? "la personne désignée par son créateur" : "les " + i.personnes().size() + " personnes désignées par son créateur";
                }
            );
        }
        return PilotageException.interditDetaille(
            "PIL_PROCESSUS_NON_INITIATEUR",
            "Ce processus ne peut être lancé que par " + String.join(", ou ", qui),
            java.util.Map.of(
                "regles",
                i.regles().stream().map(Enum::name).toList(),
                "unites",
                i.unites(),
                "profils",
                i.profils(),
                "permissions",
                i.permissions(),
                "personnes",
                i.personnes()
            )
        );
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
     * « PROCESSUS_DEFINIR portée TOUTES » ou de « PROCESSUS_LANCER portée TOUTES ». D-100 : un lancement relève
     * toujours du public de la définition, jamais d'un passe-droit ; sa mention est vide.
     */
    String mention(Appelant a, String unite, Acte acte) {
        Pouvoirs p = equipes.pouvoirs(a.id());
        boolean regleUnite = switch (acte) {
            case DEFINIR -> p.portee(Pouvoir.PROCESSUS_DEFINIR) == Portee.UNITES && p.dirige(unite);
            case LANCER -> true;
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
