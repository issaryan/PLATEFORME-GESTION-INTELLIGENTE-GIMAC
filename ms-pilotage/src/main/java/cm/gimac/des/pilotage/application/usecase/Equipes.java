package cm.gimac.des.pilotage.application.usecase;

import cm.gimac.des.pilotage.application.port.in.Appelant;
import cm.gimac.des.pilotage.application.port.out.OrganisationPort;
import cm.gimac.des.pilotage.application.port.out.OrganisationPort.Membre;
import cm.gimac.des.pilotage.application.port.out.OrganisationPort.Perimetre;
import cm.gimac.des.pilotage.application.port.out.OrganisationPort.Portee;
import cm.gimac.des.pilotage.application.port.out.OrganisationPort.Pouvoir;
import cm.gimac.des.pilotage.application.port.out.OrganisationPort.Pouvoirs;
import cm.gimac.des.pilotage.domain.exception.PilotageException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;

/**
 * Règles d'équipe (docs/lot1/mon-activite.md §3), réglées par la matrice des pouvoirs de MS-IAM
 * (docs/lot1/pouvoirs-hierarchiques.md §6, D-81) : un compte agit sur un autre si la portée de l'action
 * (TACHE_CONFIER, TACHE_SUIVRE, ACTIVITE_LIRE, RAPPORT_VISER) le couvre. Pouvoirs et périmètres sont mis
 * en cache 60 s par l'adaptateur. La permission du jeton (PIL_TEAM_READ pour lire et suivre,
 * PIL_TASK_ASSIGN pour confier) reste une condition nécessaire : la matrice règle la portée, le profil
 * ouvre la fonction. Tout autre accès : 403 PIL_HORS_EQUIPE.
 */
@Component
public class Equipes {

    private final OrganisationPort organisation;

    public Equipes(OrganisationPort organisation) {
        this.organisation = organisation;
    }

    public Pouvoirs pouvoirs(String compteId) {
        return organisation.pouvoirs(compteId);
    }

    /** Comptes couverts par la portée de {@code pouvoir} pour l'appelant (vide si AUCUNE), sans lui-même. */
    public Perimetre perimetre(Appelant appelant, Pouvoir pouvoir) {
        Portee portee = pouvoirs(appelant.id()).portee(pouvoir);
        if (portee == Portee.AUCUNE) {
            return Perimetre.vide();
        }
        Perimetre p = organisation.perimetre(appelant.id(), portee);
        List<Membre> autres = p.comptes().stream().filter(m -> !appelant.id().equals(m.id())).toList();
        return new Perimetre(p.portee(), autres, p.unites());
    }

    public List<Membre> membres(Appelant appelant, Pouvoir pouvoir) {
        return perimetre(appelant, pouvoir).comptes();
    }

    /** Membre couvert par la portée de {@code pouvoir} pour l'appelant. */
    public Optional<Membre> couvert(Appelant appelant, Pouvoir pouvoir, String autre) {
        if (autre == null || autre.equals(appelant.id())) {
            return Optional.empty();
        }
        return perimetre(appelant, pouvoir).membre(autre);
    }

    /** L'unité est couverte par {@code pouvoir} : portée UNITES et unité dirigée (sous-unités comprises). */
    public boolean couvreUnite(Appelant appelant, Pouvoir pouvoir, String unite) {
        Pouvoirs p = pouvoirs(appelant.id());
        return p.portee(pouvoir) == Portee.UNITES && p.dirige(unite);
    }

    /** Lecture des données d'un autre compte : soi-même, ou un compte couvert par {@code pouvoir}, avec PIL_TEAM_READ. */
    public boolean peutLire(Appelant appelant, Pouvoir pouvoir, String autre) {
        if (appelant.id().equals(autre)) {
            return true;
        }
        return appelant.litEquipe() && couvert(appelant, pouvoir, autre).isPresent();
    }

    public void exigerLecture(Appelant appelant, Pouvoir pouvoir, String autre) {
        if (!peutLire(appelant, pouvoir, autre)) {
            throw PilotageException.horsEquipe();
        }
    }

    /** Le compte est rattaché à l'unité ou à l'une de ses sous-unités (chemin des unités de MS-IAM). */
    public boolean dansUnite(String compteId, String unite) {
        return organisation.rattachement(compteId).filter(r -> r.dans(unite)).isPresent();
    }

    /** Unités de l'appelant : celles de son chemin (de la plus haute à la plus fine) et celles qu'il dirige. */
    public List<String> unitesDe(String compteId) {
        List<String> chemin = organisation.rattachement(compteId).map(OrganisationPort.Rattachement::chemin).orElse(List.of());
        return Stream.concat(chemin.stream(), pouvoirs(compteId).unitesDirigees().stream()).distinct().toList();
    }

    /**
     * Membres actifs d'une unité et de ses sous-unités parmi les comptes couverts par TACHE_CONFIER : le chemin
     * de chaque unité rencontrée est lu une fois (rattachement d'un de ses membres).
     */
    public List<Membre> membresUnite(Appelant appelant, String unite) {
        Map<String, Boolean> sousUnite = new HashMap<>();
        return membres(appelant, Pouvoir.TACHE_CONFIER)
            .stream()
            .filter(Membre::actif)
            .filter(m -> m.unite() != null && sousUnite.computeIfAbsent(m.unite(), u -> dansUnite(m.id(), unite)))
            .toList();
    }

    /**
     * Noms affichés des comptes (MS-IAM, cache de l'adaptateur). Un nom introuvable ou un annuaire
     * injoignable n'empêche pas la lecture : le nom est alors absent.
     */
    public Map<String, String> noms(Collection<String> ids) {
        Map<String, String> noms = new HashMap<>();
        ids
            .stream()
            .filter(Objects::nonNull)
            .distinct()
            .forEach(id -> {
                try {
                    organisation.collaborateur(id).map(OrganisationPort.Personne::nomAffiche).filter(Objects::nonNull).ifPresent(n -> noms.put(id, n));
                } catch (PilotageException e) {
                    // Annuaire indisponible : nom absent, lecture maintenue.
                }
            });
        return noms;
    }

    public Optional<OrganisationPort.Personne> collaborateur(String compteId) {
        return organisation.collaborateur(compteId);
    }

    /** Compte interne actif de l'annuaire (participant, responsable d'une tâche). */
    public boolean interneActif(String compteId) {
        return organisation.collaborateur(compteId).filter(p -> p.actif() && p.interne()).isPresent();
    }

    public Optional<OrganisationPort.Rattachement> rattachement(String compteId) {
        return organisation.rattachement(compteId);
    }

    /**
     * Profils du compte (processus d'unité, D-87) ; annuaire injoignable : aucun profil (la vue des tâches à
     * prendre ne montre alors que celles sans profil exigé), sans échec de la lecture.
     */
    public Set<String> profilsDe(String compteId) {
        try {
            return organisation.profilsDe(compteId);
        } catch (PilotageException e) {
            return Set.of();
        }
    }

    /** Unité, son responsable et ses membres portant {@code profil} (null : tous), sous-unités comprises. */
    public Optional<OrganisationPort.UniteMembres> membresUnite(String unite, String profil) {
        return organisation.membresUnite(unite, profil);
    }

    /** Le compte, actif, est membre de l'unité (ou d'une sous-unité) et porte le profil. */
    public boolean porteProfilDans(String compteId, String unite, String profil) {
        return membresUnite(unite, profil).map(u -> u.actifs().stream().anyMatch(m -> m.id().equals(compteId))).orElse(false);
    }
}
