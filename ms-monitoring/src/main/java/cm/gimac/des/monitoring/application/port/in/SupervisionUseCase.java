package cm.gimac.des.monitoring.application.port.in;

import cm.gimac.des.monitoring.application.port.in.Vues.AlerteVue;
import cm.gimac.des.monitoring.application.port.in.Vues.LiaisonVue;
import cm.gimac.des.monitoring.application.port.in.Vues.PolitiqueVue;
import java.util.List;
import java.util.UUID;

/** Liaisons supervisées et alertes (MON-01 à MON-06, algorithme 6.44), lecture et acquittement. */
public interface SupervisionUseCase {
    Page<LiaisonVue> liaisons(Appelant appelant, String etat, String recherche, Integer page, Integer taille);

    LiaisonVue liaison(Appelant appelant, UUID id);

    Page<AlerteVue> alertes(Appelant appelant, String etat, String gravite, UUID liaison, Integer page, Integer taille);

    AlerteVue alerte(Appelant appelant, UUID id);

    /** Acquittement réservé à la vacation en cours ; ticket obligatoire pour une critique racine. */
    AlerteVue acquitter(Appelant appelant, UUID id, String commentaire, Boolean ticket);

    List<PolitiqueVue> politiques();
}
