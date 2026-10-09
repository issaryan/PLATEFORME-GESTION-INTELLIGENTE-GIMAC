package cm.gimac.des.pilotage.application.usecase;

import cm.gimac.des.pilotage.application.port.in.ApprobationsTransitionsUseCase;
import cm.gimac.des.pilotage.application.port.in.TachesUseCase;
import cm.gimac.des.pilotage.application.port.in.TachesUseCase.CibleInterne;
import cm.gimac.des.pilotage.application.port.in.TachesUseCase.Origine;
import cm.gimac.des.pilotage.application.port.in.TachesUseCase.TacheInterne;
import cm.gimac.des.pilotage.domain.activite.TypeOrigine;
import cm.gimac.des.pilotage.domain.service.ReglesActivite;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tâche de l'approbateur d'une transition de statut (D-98) : offre de MS-PILOTAGE à prendre par les porteurs de la
 * permission d'approbation (données de MS-IAM, aucun profil cité), d'origine DECISION, service participant, référence
 * {@code <institution>/TRANSITION/<transition>}, lien vers l'écran où l'on prononce la transition. La notification de
 * l'offre mène à cet écran (l'acte se fait là, sans prise préalable). Close (terminée par le service) quand la transition
 * est prononcée ou refusée ; remise à prendre après la première de deux approbations.
 */
@Service
@Transactional
public class ApprobationsTransitionsService implements ApprobationsTransitionsUseCase {

    /** Client des tâches créées pour MS-PARTICIPANT (historique et créateur par défaut). */
    static final String CLIENT = "participant-service";
    static final String SERVICE = "participant";
    /** Permission de MS-IAM qui ouvre l'approbation d'une transition (route d'approbation et de rejet). */
    static final String PERMISSION = "PAR_TRANSITION_APPROVE";

    private final TachesUseCase taches;

    public ApprobationsTransitionsService(TachesUseCase taches) {
        this.taches = taches;
    }

    static String reference(String institutionId, String transitionId) {
        return institutionId + "/TRANSITION/" + transitionId;
    }

    /** Lien de la proposition s'il est un chemin relatif de l'interface, sinon l'onglet des transitions de l'institution. */
    static String lien(String lien, String institutionId) {
        if (lien != null && lien.startsWith("/") && !lien.startsWith("//") && lien.length() <= 500 && lien.chars().noneMatch(Character::isWhitespace)) {
            return lien;
        }
        return "/des/institutions/" + institutionId + "?onglet=transitions";
    }

    /** Libellé d'un statut d'institution (DDR-14, DDR-16) : jamais le code brut dans le titre ou la description d'une tâche. */
    static String statut(String code) {
        if (code == null) {
            return "";
        }
        return switch (code) {
            case "CANDIDAT" -> "Candidat";
            case "ADHERENT" -> "Adhérent";
            case "EN_HOMOLOGATION" -> "En homologation";
            case "ACTIF" -> "Actif";
            case "A_REGULARISER" -> "À régulariser";
            case "SUSPENDU" -> "Suspendu";
            case "RETIRE" -> "Retiré";
            case "CLOS_SANS_SUITE" -> "Clos sans suite";
            default -> code;
        };
    }

    static String titre(Proposition p) {
        String nom = p.institutionNom() == null || p.institutionNom().isBlank() ? p.institutionId() : p.institutionNom().strip();
        String t = "ADHERENT".equals(p.vers())
            ? "Prononcer l'adhésion : " + nom
            : "Statuer sur le passage de " + statut(p.de()) + " à " + statut(p.vers()) + " : " + nom;
        return t.length() > ReglesActivite.TITRE_MAX ? t.substring(0, ReglesActivite.TITRE_MAX - 1) + "…" : t;
    }

    static String description(Proposition p) {
        String motif = p.motif() == null || p.motif().isBlank() ? "" : " Motif : " + p.motif().strip() + ".";
        String approbations = p.approbationsRequises() > 1
            ? " Deux approbations de personnes distinctes sont requises."
            : "";
        String d =
            "Proposition de passage de " + statut(p.de()) + " à " + statut(p.vers()) + "." + motif + approbations +
            " Prononcer ou refuser depuis l'onglet Propositions de statut de la fiche de l'institution ; l'auteur de la proposition ne peut pas la prononcer.";
        return d.length() > 2000 ? d.substring(0, 1999) + "…" : d;
    }

    @Override
    public boolean proposee(Proposition p) {
        Origine origine = new Origine(TypeOrigine.DECISION.name(), reference(p.institutionId(), p.transitionId()), SERVICE, lien(p.lien(), p.institutionId()));
        TacheInterne t = new TacheInterne(
            titre(p),
            description(p),
            null,
            "HAUTE",
            null,
            p.demandeurId(),
            origine,
            null,
            null,
            List.of(PERMISSION)
        );
        return taches.creerPourService(CLIENT, t).creee();
    }

    @Override
    public int approuvee(String institutionId, String transitionId, boolean definitive) {
        CibleInterne cible = new CibleInterne(origine(institutionId, transitionId), null, true);
        return definitive ? taches.terminerPourService(CLIENT, cible).size() : taches.rouvrirPourService(CLIENT, cible).size();
    }

    @Override
    public int rejetee(String institutionId, String transitionId) {
        return taches.terminerPourService(CLIENT, new CibleInterne(origine(institutionId, transitionId), null, true)).size();
    }

    private static Origine origine(String institutionId, String transitionId) {
        return new Origine(TypeOrigine.DECISION.name(), reference(institutionId, transitionId), SERVICE, null);
    }
}
