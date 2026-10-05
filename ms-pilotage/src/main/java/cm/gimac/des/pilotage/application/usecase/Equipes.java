package cm.gimac.des.pilotage.application.usecase;

import cm.gimac.des.pilotage.application.port.in.Appelant;
import cm.gimac.des.pilotage.application.port.out.OrganisationPort;
import cm.gimac.des.pilotage.application.port.out.OrganisationPort.Membre;
import cm.gimac.des.pilotage.domain.exception.PilotageException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Règles d'équipe (docs/lot1/mon-activite.md §3) : l'équipe d'un compte est l'ensemble des comptes
 * dont il figure dans la chaîne des responsables (MS-IAM, mise en cache 60 s par l'adaptateur). Avec
 * PIL_TEAM_READ, on lit les données de son équipe ; tout autre accès : 403 PIL_HORS_EQUIPE.
 */
@Component
public class Equipes {

    private final OrganisationPort organisation;

    public Equipes(OrganisationPort organisation) {
        this.organisation = organisation;
    }

    public List<Membre> membres(String compteId) {
        return organisation.equipe(compteId);
    }

    public Optional<Membre> membre(String compteId, String autre) {
        if (autre == null) {
            return Optional.empty();
        }
        return membres(compteId).stream().filter(m -> autre.equals(m.id())).findFirst();
    }

    /** Lecture des données d'un autre compte : soi-même, ou un membre de son équipe avec PIL_TEAM_READ. */
    public boolean peutLire(Appelant appelant, String autre) {
        return appelant.id().equals(autre) || (appelant.litEquipe() && membre(appelant.id(), autre).isPresent());
    }

    public void exigerLecture(Appelant appelant, String autre) {
        if (!peutLire(appelant, autre)) {
            throw PilotageException.horsEquipe();
        }
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

    /** Compte interne actif de l'annuaire (participant, responsable d'une tâche). */
    public boolean interneActif(String compteId) {
        return organisation.collaborateur(compteId).filter(p -> p.actif() && p.interne()).isPresent();
    }

    public Optional<OrganisationPort.Rattachement> rattachement(String compteId) {
        return organisation.rattachement(compteId);
    }
}
