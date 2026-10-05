package cm.gimac.des.pilotage.application.port.in;

import cm.gimac.des.pilotage.domain.activite.Activite;
import java.util.List;
import java.util.UUID;

/** Activités du jour (docs/lot1/mon-activite.md §2.2). */
public interface ActivitesUseCase {
    record ActiviteSaisie(String jour, String titre, String description, Integer dureeMinutes, Integer ordre, String tacheId) {}

    record ActiviteLue(Activite activite, String nomAuteur) {}

    /** Activités de l'intervalle [du, au] (31 jours au plus), triées par jour puis ordre. */
    List<ActiviteLue> activites(Appelant appelant, String du, String au, String collaborateur);

    ActiviteLue creer(Appelant appelant, ActiviteSaisie saisie);

    ActiviteLue modifier(Appelant appelant, UUID id, ActiviteSaisie saisie);

    /** Nouveau statut ; REPORTEE avec {@code reporteeAu} crée une copie A_FAIRE au jour indiqué. */
    ActiviteLue changerStatut(Appelant appelant, UUID id, String statut, String reporteeAu);

    void supprimer(Appelant appelant, UUID id);
}
