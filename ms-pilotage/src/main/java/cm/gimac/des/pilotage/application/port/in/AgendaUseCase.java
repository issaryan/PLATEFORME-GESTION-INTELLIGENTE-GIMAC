package cm.gimac.des.pilotage.application.port.in;

import cm.gimac.des.pilotage.domain.activite.EvenementAgenda;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Agenda (docs/lot1/mon-activite.md §2.1). */
public interface AgendaUseCase {
    /** Lien de l'appelant avec l'événement : PROPRIETAIRE, PARTICIPANT ou EQUIPE (lecture seule). */
    enum Relation {
        PROPRIETAIRE,
        PARTICIPANT,
        EQUIPE,
    }

    record EvenementSaisi(
        String titre,
        String description,
        String lieu,
        String debut,
        String fin,
        Boolean journeeEntiere,
        String categorie,
        List<String> participants
    ) {}

    /** Événement et noms affichés de son propriétaire et de ses participants. */
    record EvenementLu(EvenementAgenda evenement, Relation relation, Map<String, String> noms) {}

    /** Événements de l'intervalle [du, au] (62 jours au plus) ; {@code collaborateur} : un membre de l'équipe. */
    List<EvenementLu> agenda(Appelant appelant, String du, String au, String collaborateur);

    EvenementLu creer(Appelant appelant, EvenementSaisi saisie);

    EvenementLu modifier(Appelant appelant, UUID id, EvenementSaisi saisie);

    void supprimer(Appelant appelant, UUID id);

    EvenementLu repondre(Appelant appelant, UUID id, String reponse);
}
