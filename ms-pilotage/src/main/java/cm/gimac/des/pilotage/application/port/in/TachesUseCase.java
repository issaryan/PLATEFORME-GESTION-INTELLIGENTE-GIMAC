package cm.gimac.des.pilotage.application.port.in;

import cm.gimac.des.pilotage.domain.activite.Tache;
import cm.gimac.des.pilotage.domain.activite.TacheHistorique;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Tâches (docs/lot1/mon-activite.md §2.3 ; CP PIL-04, PIL-08). */
public interface TachesUseCase {
    record TacheSaisie(String titre, String description, String echeance, String priorite, String responsableId) {}

    record Origine(String type, String reference, String service, String lien) {}

    /** Tâche demandée par un autre service (jeton de service INTERNAL_PILOTAGE_TASK_CREATE). */
    record TacheInterne(String titre, String description, String echeance, String priorite, String responsableId, String createurId, Origine origine) {}

    record TacheLue(Tache tache, List<TacheHistorique> historique, Map<String, String> noms, boolean enRetard) {}

    /** Désignation par un autre service des tâches qu'il a créées : origine (type facultatif, référence) et responsable facultatif. */
    record CibleInterne(Origine origine, String responsableId) {}

    /** {@code creee} faux : la tâche existait déjà pour cette origine et ce responsable (idempotence). */
    record Creation(TacheLue tache, boolean creee) {}

    /** {@code vue} : moi (responsable), creees (confiées par moi), equipe (PIL_TEAM_READ). */
    Page<TacheLue> taches(Appelant appelant, String vue, String etat, Integer page, Integer taille);

    TacheLue tache(Appelant appelant, UUID id);

    TacheLue creer(Appelant appelant, TacheSaisie saisie);

    TacheLue modifier(Appelant appelant, UUID id, TacheSaisie saisie);

    TacheLue transiter(Appelant appelant, UUID id, String etat, String motif);

    Creation creerPourService(String client, TacheInterne demande);

    /** Tâches d'une origine terminées par le service qui les a créées (D-77) ; idempotent. Rend les tâches visées. */
    List<TacheLue> terminerPourService(String client, CibleInterne cible);

    /** Tâches d'une origine rouvertes par le service qui les a créées (D-77) ; idempotent. Rend les tâches visées. */
    List<TacheLue> rouvrirPourService(String client, CibleInterne cible);
}
