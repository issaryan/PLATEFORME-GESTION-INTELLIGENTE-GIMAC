package cm.gimac.des.gateway.security.session;

import java.time.Duration;

/**
 * Bornes d'une session (contrat D-65 §6).
 *
 * @param inactiviteMax inactivité maximale (gimac.session.inactivite-max, 30 min)
 * @param dureeMax durée absolue maximale (gimac.session.duree-max, 10 h)
 */
public record ParametresSession(Duration inactiviteMax, Duration dureeMax) {
    public ParametresSession {
        if (inactiviteMax == null || inactiviteMax.isNegative() || inactiviteMax.isZero()) {
            throw new IllegalArgumentException("gimac.session.inactivite-max doit être une durée positive");
        }
        if (dureeMax == null || dureeMax.compareTo(inactiviteMax) < 0) {
            throw new IllegalArgumentException("gimac.session.duree-max doit être au moins égale à l'inactivité maximale");
        }
    }
}
