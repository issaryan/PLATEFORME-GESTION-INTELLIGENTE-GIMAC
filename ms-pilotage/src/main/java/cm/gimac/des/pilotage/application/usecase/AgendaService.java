package cm.gimac.des.pilotage.application.usecase;

import cm.gimac.des.pilotage.application.port.in.AgendaUseCase;
import cm.gimac.des.pilotage.application.port.in.Appelant;
import cm.gimac.des.pilotage.application.port.out.OrganisationPort.Pouvoir;
import cm.gimac.des.pilotage.domain.activite.CategorieEvenement;
import cm.gimac.des.pilotage.domain.activite.EvenementAgenda;
import cm.gimac.des.pilotage.domain.activite.ParticipantEvenement;
import cm.gimac.des.pilotage.domain.activite.ReponseParticipant;
import cm.gimac.des.pilotage.domain.exception.PilotageException;
import cm.gimac.des.pilotage.domain.service.ReglesActivite;
import cm.gimac.des.pilotage.repository.EvenementAgendaRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Agenda (§2.1) : événements de l'intervalle (62 jours au plus), création, modification et suppression
 * par le propriétaire seul, réponse d'un participant invité ; agenda d'un membre de l'équipe en lecture
 * seule (§3).
 */
@Service
@Transactional
public class AgendaService implements AgendaUseCase {

    private final EvenementAgendaRepository evenements;
    private final Equipes equipes;
    private final Clock horloge;

    public AgendaService(EvenementAgendaRepository evenements, Equipes equipes, Clock horloge) {
        this.evenements = evenements;
        this.equipes = equipes;
        this.horloge = horloge;
    }

    @Override
    @Transactional(readOnly = true)
    public List<EvenementLu> agenda(Appelant appelant, String du, String au, String collaborateur) {
        LocalDate debut = ReglesActivite.lireJour("du", du);
        LocalDate fin = ReglesActivite.lireJour("au", au);
        ReglesActivite.intervalle(debut, fin, ReglesActivite.AGENDA_JOURS_MAX);
        String compte = collaborateur == null || collaborateur.isBlank() ? appelant.id() : collaborateur.strip();
        equipes.exigerLecture(appelant, Pouvoir.ACTIVITE_LIRE, compte);
        List<EvenementAgenda> liste = evenements.agenda(compte, ReglesActivite.debutJour(debut), ReglesActivite.debutJour(fin.plusDays(1)));
        Map<String, String> noms = equipes.noms(liste.stream().flatMap(AgendaService::comptes).toList());
        return liste.stream().map(e -> new EvenementLu(e, relation(e, appelant.id()), noms)).toList();
    }

    @Override
    public EvenementLu creer(Appelant appelant, EvenementSaisi saisie) {
        Instant le = horloge.instant();
        EvenementAgenda e = new EvenementAgenda(appelant.id(), le);
        decrire(e, appelant, saisie, le);
        evenements.save(e);
        return lu(e, appelant);
    }

    @Override
    public EvenementLu modifier(Appelant appelant, UUID id, EvenementSaisi saisie) {
        EvenementAgenda e = proprietaire(appelant, id);
        decrire(e, appelant, saisie, horloge.instant());
        return lu(e, appelant);
    }

    @Override
    public void supprimer(Appelant appelant, UUID id) {
        evenements.delete(proprietaire(appelant, id));
    }

    @Override
    public EvenementLu repondre(Appelant appelant, UUID id, String reponse) {
        EvenementAgenda e = evenements.findById(id).orElseThrow(() -> PilotageException.introuvable("PIL_EVENEMENT_INTROUVABLE", id));
        ReponseParticipant r = ReglesActivite.valeur("reponse", ReponseParticipant.class, reponse, null);
        if (r == ReponseParticipant.EN_ATTENTE) {
            throw PilotageException.champ("reponse", "Réponse ACCEPTE ou DECLINE attendue");
        }
        if (!e.repondre(appelant.id(), r, horloge.instant())) {
            throw invisible(appelant, e);
        }
        return lu(e, appelant);
    }

    /** Événement modifiable : propriétaire seul ; participant ou équipe : 403 ; inconnu : 404. */
    private EvenementAgenda proprietaire(Appelant appelant, UUID id) {
        EvenementAgenda e = evenements.findById(id).orElseThrow(() -> PilotageException.introuvable("PIL_EVENEMENT_INTROUVABLE", id));
        if (!e.getProprietaireId().equals(appelant.id())) {
            if (e.participant(appelant.id()).isPresent() || equipes.peutLire(appelant, Pouvoir.ACTIVITE_LIRE, e.getProprietaireId())) {
                throw PilotageException.reservee("Seul le propriétaire modifie ou supprime l'événement");
            }
            throw PilotageException.horsEquipe();
        }
        return e;
    }

    private PilotageException invisible(Appelant appelant, EvenementAgenda e) {
        if (e.getProprietaireId().equals(appelant.id()) || equipes.peutLire(appelant, Pouvoir.ACTIVITE_LIRE, e.getProprietaireId())) {
            return PilotageException.reservee("Seul un participant invité répond à l'événement");
        }
        return PilotageException.horsEquipe();
    }

    private void decrire(EvenementAgenda e, Appelant appelant, EvenementSaisi s, Instant le) {
        if (s == null) {
            throw PilotageException.champ("titre", "Corps de la requête absent");
        }
        String titre = ReglesActivite.texteObligatoire("titre", s.titre(), ReglesActivite.TITRE_MAX);
        String description = ReglesActivite.texteFacultatif("description", s.description(), ReglesActivite.DESCRIPTION_MAX);
        String lieu = ReglesActivite.texteFacultatif("lieu", s.lieu(), ReglesActivite.LIEU_MAX);
        Instant debut = ReglesActivite.lireHorodatage("debut", s.debut());
        Instant fin = ReglesActivite.lireHorodatage("fin", s.fin());
        boolean journee = Boolean.TRUE.equals(s.journeeEntiere());
        ReglesActivite.bornesEvenement(debut, fin, journee);
        CategorieEvenement categorie = ReglesActivite.valeur("categorie", CategorieEvenement.class, s.categorie(), CategorieEvenement.AUTRE);
        List<String> invites = participants(e, appelant, s.participants());
        e.decrire(titre, description, lieu, debut, fin, journee, categorie, invites, le);
    }

    /**
     * Participants : sans doublon ni le propriétaire, 50 au plus, comptes internes actifs (422
     * PIL_PARTICIPANT_INVALIDE) ; un participant déjà invité n'est pas revérifié.
     */
    private List<String> participants(EvenementAgenda e, Appelant appelant, List<String> demandes) {
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        if (demandes != null) {
            demandes.stream().filter(i -> i != null && !i.isBlank()).map(String::strip).filter(i -> !i.equals(appelant.id())).forEach(ids::add);
        }
        if (ids.size() > ReglesActivite.PARTICIPANTS_MAX) {
            throw PilotageException.regle(
                "PIL_PARTICIPANT_INVALIDE",
                ReglesActivite.PARTICIPANTS_MAX + " participants au plus",
                Map.of("maximum", ReglesActivite.PARTICIPANTS_MAX)
            );
        }
        List<String> invalides = new ArrayList<>();
        for (String id : ids) {
            if (e.participant(id).isEmpty() && !equipes.interneActif(id)) {
                invalides.add(id);
            }
        }
        if (!invalides.isEmpty()) {
            throw PilotageException.regle(
                "PIL_PARTICIPANT_INVALIDE",
                "Participant inconnu, inactif ou externe",
                Map.of("participants", List.copyOf(invalides))
            );
        }
        return List.copyOf(ids);
    }

    private EvenementLu lu(EvenementAgenda e, Appelant appelant) {
        return new EvenementLu(e, relation(e, appelant.id()), equipes.noms(comptes(e).toList()));
    }

    static Relation relation(EvenementAgenda e, String moi) {
        if (e.getProprietaireId().equals(moi)) {
            return Relation.PROPRIETAIRE;
        }
        return e.participant(moi).isPresent() ? Relation.PARTICIPANT : Relation.EQUIPE;
    }

    private static Stream<String> comptes(EvenementAgenda e) {
        return Stream.concat(Stream.of(e.getProprietaireId()), e.getParticipants().stream().map(ParticipantEvenement::getParticipantId));
    }
}
