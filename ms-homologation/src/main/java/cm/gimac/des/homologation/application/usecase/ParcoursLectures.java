package cm.gimac.des.homologation.application.usecase;

import cm.gimac.des.homologation.application.port.out.ParticipantDirectoryPort;
import cm.gimac.des.homologation.domain.HomologationException;
import cm.gimac.des.homologation.domain.Liaison;
import cm.gimac.des.homologation.domain.parcours.Acteur;
import cm.gimac.des.homologation.domain.parcours.EtapeCode;
import cm.gimac.des.homologation.domain.parcours.Fonction;
import cm.gimac.des.homologation.domain.parcours.Parcours;
import cm.gimac.des.homologation.domain.parcours.Personne;
import cm.gimac.des.homologation.domain.parcours.Vues;
import cm.gimac.des.homologation.domain.service.ParcoursProjection;
import cm.gimac.des.homologation.domain.service.TableauDeBordCalculateur;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lectures du parcours (docs/lot1/parcours-backend.md §1) : dossier numérique, opérations, fiche de suivi,
 * tableaux de bord par fonction, pilotage, intervenants, projections du portail cloisonnées à
 * l'institution du jeton, contexte servi à MS-CERTIFICATION. Une liaison ouverte avant cette tranche est
 * instanciée à la première lecture (d'où la transaction en écriture).
 */
@Service
@Transactional
public class ParcoursLectures {

    private final ParcoursDepot depot;
    private final ParcoursInstanciation instanciation;
    private final ParcoursProjection projection;
    private final TableauDeBordCalculateur tableaux;
    private final Intervenants intervenants;
    private final ParticipantDirectoryPort participants;
    private final Clock clock;

    public ParcoursLectures(
        ParcoursDepot depot,
        ParcoursInstanciation instanciation,
        ParcoursProjection projection,
        TableauDeBordCalculateur tableaux,
        Intervenants intervenants,
        ParticipantDirectoryPort participants,
        Clock clock
    ) {
        this.depot = depot;
        this.instanciation = instanciation;
        this.projection = projection;
        this.tableaux = tableaux;
        this.intervenants = intervenants;
        this.participants = participants;
        this.clock = clock;
    }

    private LocalDate aujourdhui() {
        return LocalDate.now(clock);
    }

    Parcours charger(UUID liaisonId) {
        return depot.trouver(liaisonId, false).orElseGet(() -> instanciation.instancierExistante(depot.liaison(liaisonId)));
    }

    private ParcoursProjection.Contexte contexte(Parcours p) {
        Liaison l = p.liaison();
        Personne conducteur = new Personne(l.getChefDeProjetId().toString(), intervenants.nom(l.getChefDeProjetId()), Fonction.CONDUCTEUR);
        String banque = null;
        if (l.getBanquePartenaireId() != null) {
            try {
                banque = participants.getInstitution(l.getBanquePartenaireId()).nom();
            } catch (HomologationException e) {
                banque = null;
            }
        }
        return new ParcoursProjection.Contexte(conducteur, banque, l.getVersion() == null ? null : l.getVersion().getNumero(), l.getVersion() == null ? null : l.getVersion().etapes());
    }

    public Vues.Dossier dossier(UUID liaisonId, Acteur a) {
        Parcours p = charger(liaisonId);
        return projection.dossier(p, contexte(p), a, aujourdhui());
    }

    /** Opérations instanciées, filtrées par étape et par fonction du responsable (R). */
    public List<Vues.Operation> operations(UUID liaisonId, EtapeCode etape, Fonction fonction, Acteur a) {
        Parcours p = charger(liaisonId);
        return p
            .operations()
            .stream()
            .filter(o -> etape == null || o.getEtape() == etape)
            .filter(o -> fonction == null || java.util.Objects.equals(o.fonctionR(), fonction))
            .map(o -> projection.operation(p, o, a, aujourdhui()))
            .toList();
    }

    public Vues.FicheSuivi fiche(UUID liaisonId, Acteur a) {
        return projection.fiche(charger(liaisonId), a, false);
    }

    /**
     * Tableau de bord d'un acteur : seules les fonctions que confèrent les profils du jeton sont retenues
     * (D-58, D-61) ; sans fonction demandée, toutes celles de l'acteur.
     */
    public Vues.TableauActeur tableauActeur(List<Fonction> demandees, Acteur a) {
        Set<Fonction> detenues = Fonction.desProfils(a.permissions());
        List<Fonction> retenues = new ArrayList<>((demandees == null || demandees.isEmpty() ? List.copyOf(detenues) : demandees).stream().distinct().filter(detenues::contains).filter(f -> f != Fonction.PARTICIPANT).toList());
        if (retenues.isEmpty()) {
            throw HomologationException.forbidden("HOM_FUNCTION_NOT_HELD", "Aucune des fonctions demandées n'est portée par le jeton.");
        }
        List<Parcours> tous = depot.tous();
        return tableaux.tableau(tous, retenues, a, null, noms(tous), aujourdhui());
    }

    public Vues.Pilotage pilotage() {
        return tableaux.pilotage(depot.tous(), aujourdhui());
    }

    public List<Personne> intervenants(Fonction f) {
        return intervenants.deFonction(f);
    }

    private Map<UUID, String> noms(List<Parcours> tous) {
        Map<UUID, String> noms = new HashMap<>();
        tous.forEach(p -> noms.computeIfAbsent(p.liaison().getChefDeProjetId(), intervenants::nom));
        return noms;
    }

    /** Résumés du parcours d'une page de liaisons (liste du DES). */
    public Map<UUID, Vues.LiaisonResume> resumes(List<Liaison> liaisons, Map<UUID, String> conducteurs) {
        Map<UUID, Vues.LiaisonResume> m = new HashMap<>();
        for (Liaison l : liaisons) {
            depot.trouver(l.getId(), false).ifPresent(p -> m.put(l.getId(), projection.resume(p, conducteurs.get(l.getChefDeProjetId()), aujourdhui())));
        }
        return m;
    }

    // ------------------------------------------------------------------ portail

    /** Liaisons de l'institution du jeton. */
    public List<Parcours> mesParcours(Acteur a) {
        return depot.deLInstitution(a.institutionId());
    }

    public Vues.LiaisonResume resumePortail(Parcours p) {
        return projection.resume(p, "Conducteur de projet GIMAC", aujourdhui());
    }

    public Vues.Dossier dossierPortail(UUID liaisonId, Acteur a) {
        Parcours p = ParcoursCommandes.sienne(charger(liaisonId), a);
        return projection.dossierPortail(p, contexte(p), aujourdhui());
    }

    public Vues.TableauActeur tachesPortail(Acteur a) {
        List<Parcours> siens = depot.deLInstitution(a.institutionId());
        return tableaux.tableau(siens, List.of(Fonction.PARTICIPANT), a, a.institutionId(), Map.of(), aujourdhui());
    }

    public Vues.FicheSuivi fichePortail(UUID liaisonId, Acteur a) {
        return projection.fiche(ParcoursCommandes.sienne(charger(liaisonId), a), a, true);
    }

    // ------------------------------------------------------------------ interne

    /** Contexte de certification servi à MS-CERTIFICATION (jeton de service, INTERNAL_HOMOLOGATION_READ). */
    public Vues.ContexteCertification contexteCertification(UUID liaisonId) {
        Parcours p = charger(liaisonId);
        return projection.contexte(p, intervenants.nom(p.liaison().getChefDeProjetId()));
    }
}
