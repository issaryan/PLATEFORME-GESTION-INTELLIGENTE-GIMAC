package cm.gimac.des.gateway.domain.supervision;

import java.time.Instant;
import java.util.List;

/** Alertes actives, ou Alertmanager injoignable (joignable faux, erreur renseignée). */
public record EtatAlertes(Instant horodatage, boolean joignable, String erreur, int nombre, List<Alerte> alertes) {
    public EtatAlertes {
        alertes = List.copyOf(alertes);
    }
}
