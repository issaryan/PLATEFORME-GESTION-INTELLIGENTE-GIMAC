package cm.gimac.des.monitoring.application.port.in;

import cm.gimac.des.monitoring.application.port.in.Vues.Reception;
import java.time.Instant;

/**
 * Réception des alertes des sondes (algorithme 6.44 : admission, empreinte, déduplication, corrélation, échéance),
 * résolution par la source et escalade des alertes non acquittées (MON-05).
 */
public interface ReceptionAlertesUseCase {
    Reception recevoir(String source, String liaison, String regle, String gravite, Instant survenueLe);

    int resoudre(String source, String liaison, String regle);

    /** Fait monter d'un palier les racines non acquittées dont le palier dû a changé ; rend le nombre d'escalades. */
    int escalader();
}
