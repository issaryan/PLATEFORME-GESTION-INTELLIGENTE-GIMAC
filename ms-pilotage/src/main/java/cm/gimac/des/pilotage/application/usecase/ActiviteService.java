package cm.gimac.des.pilotage.application.usecase;

import cm.gimac.des.pilotage.application.port.in.ActivitesUseCase;
import cm.gimac.des.pilotage.application.port.in.Appelant;
import cm.gimac.des.pilotage.domain.activite.Activite;
import cm.gimac.des.pilotage.domain.activite.StatutActivite;
import cm.gimac.des.pilotage.domain.activite.Tache;
import cm.gimac.des.pilotage.domain.exception.PilotageException;
import cm.gimac.des.pilotage.domain.service.ReglesActivite;
import cm.gimac.des.pilotage.repository.ActiviteRepository;
import cm.gimac.des.pilotage.repository.TacheRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Activités du jour (§2.2) : intervalle de 31 jours au plus, triées par jour puis ordre ; création,
 * modification, statut et suppression par l'auteur seul ; reporter une activité en crée une copie
 * A_FAIRE au jour indiqué.
 */
@Service
@Transactional
public class ActiviteService implements ActivitesUseCase {

    private final ActiviteRepository activites;
    private final TacheRepository taches;
    private final Equipes equipes;
    private final Clock horloge;

    public ActiviteService(ActiviteRepository activites, TacheRepository taches, Equipes equipes, Clock horloge) {
        this.activites = activites;
        this.taches = taches;
        this.equipes = equipes;
        this.horloge = horloge;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ActiviteLue> activites(Appelant appelant, String du, String au, String collaborateur) {
        LocalDate debut = ReglesActivite.lireJour("du", du);
        LocalDate fin = ReglesActivite.lireJour("au", au);
        ReglesActivite.intervalle(debut, fin, ReglesActivite.ACTIVITES_JOURS_MAX);
        String compte = collaborateur == null || collaborateur.isBlank() ? appelant.id() : collaborateur.strip();
        equipes.exigerLecture(appelant, compte);
        String nom = equipes.noms(List.of(compte)).get(compte);
        return activites.findByAuteurIdAndJourBetweenOrderByJourAscOrdreAscCreeLeAsc(compte, debut, fin).stream().map(a -> new ActiviteLue(a, nom)).toList();
    }

    @Override
    public ActiviteLue creer(Appelant appelant, ActiviteSaisie saisie) {
        Instant le = horloge.instant();
        Activite a = new Activite(appelant.id(), le);
        decrire(a, appelant, saisie, le);
        activites.save(a);
        return lu(a, appelant);
    }

    @Override
    public ActiviteLue modifier(Appelant appelant, UUID id, ActiviteSaisie saisie) {
        Activite a = auteur(appelant, id);
        decrire(a, appelant, saisie, horloge.instant());
        return lu(a, appelant);
    }

    @Override
    public ActiviteLue changerStatut(Appelant appelant, UUID id, String statut, String reporteeAu) {
        Activite a = auteur(appelant, id);
        StatutActivite cible = ReglesActivite.valeur("statut", StatutActivite.class, statut, null);
        if (a.getStatut() == StatutActivite.REPORTEE) {
            // Sa copie existe déjà au jour du report : c'est elle qui vit désormais.
            throw PilotageException.conflit(
                "PIL_TRANSITION_INTERDITE",
                "Une activité reportée ne change plus de statut",
                Map.of("de", a.getStatut().name(), "vers", cible.name())
            );
        }
        Instant le = horloge.instant();
        if (cible == StatutActivite.REPORTEE) {
            LocalDate au = ReglesActivite.lireJour("reporteeAu", reporteeAu);
            if (!au.isAfter(a.getJour())) {
                throw PilotageException.champ("reporteeAu", "Le report se fait à un jour postérieur à celui de l'activité");
            }
            a.changerStatut(cible, au, le);
            activites.save(a.copieReportee(activites.dernierOrdre(a.getAuteurId(), au) + 1, le));
        } else {
            a.changerStatut(cible, null, le);
        }
        return lu(a, appelant);
    }

    @Override
    public void supprimer(Appelant appelant, UUID id) {
        activites.delete(auteur(appelant, id));
    }

    private Activite auteur(Appelant appelant, UUID id) {
        Activite a = activites.findById(id).orElseThrow(() -> PilotageException.introuvable("PIL_ACTIVITE_INTROUVABLE", id));
        if (!a.getAuteurId().equals(appelant.id())) {
            if (equipes.peutLire(appelant, a.getAuteurId())) {
                throw PilotageException.reservee("Seul l'auteur modifie son activité");
            }
            throw PilotageException.horsEquipe();
        }
        return a;
    }

    private void decrire(Activite a, Appelant appelant, ActiviteSaisie s, Instant le) {
        if (s == null) {
            throw PilotageException.champ("jour", "Corps de la requête absent");
        }
        LocalDate jour = ReglesActivite.lireJour("jour", s.jour());
        String titre = ReglesActivite.texteObligatoire("titre", s.titre(), ReglesActivite.TITRE_MAX);
        String description = ReglesActivite.texteFacultatif("description", s.description(), ReglesActivite.DESCRIPTION_MAX);
        Integer duree = ReglesActivite.duree(s.dureeMinutes());
        int ordre;
        if (s.ordre() != null) {
            if (s.ordre() < 0) {
                throw PilotageException.champ("ordre", "Ordre positif attendu");
            }
            ordre = s.ordre();
        } else if (a.getJour() != null && a.getJour().equals(jour)) {
            ordre = a.getOrdre();
        } else {
            ordre = activites.dernierOrdre(appelant.id(), jour) + 1;
        }
        a.decrire(jour, titre, description, duree, ordre, tache(appelant, s.tacheId()), le);
    }

    /** Tâche liée : connue et dont l'appelant est responsable ou créateur ; 422 PIL_TACHE_INVALIDE sinon. */
    private UUID tache(Appelant appelant, String tacheId) {
        if (tacheId == null || tacheId.isBlank()) {
            return null;
        }
        UUID id;
        try {
            id = UUID.fromString(tacheId.strip());
        } catch (IllegalArgumentException e) {
            throw PilotageException.champ("tacheId", "Identifiant de tâche invalide");
        }
        Tache t = taches.findById(id).orElse(null);
        if (t == null || !(appelant.id().equals(t.getResponsableId()) || appelant.id().equals(t.getCreateurId()))) {
            throw PilotageException.regle("PIL_TACHE_INVALIDE", "Tâche inconnue ou étrangère à l'appelant", Map.of("tacheId", id.toString()));
        }
        return id;
    }

    private ActiviteLue lu(Activite a, Appelant appelant) {
        return new ActiviteLue(a, appelant.nomAffiche() != null ? appelant.nomAffiche() : equipes.noms(List.of(a.getAuteurId())).get(a.getAuteurId()));
    }
}
