package cm.gimac.des.gateway.domain.supervision;

import java.util.Locale;

/** État d'un service tel que le rapporte sa sonde /management/health, ou INJOIGNABLE. */
public enum EtatSante {
    UP,
    DOWN,
    OUT_OF_SERVICE,
    INCONNU,
    INJOIGNABLE;

    /** Lecture tolérante du statut Spring Boot (UP, DOWN, OUT_OF_SERVICE, UNKNOWN). */
    public static EtatSante depuis(Object statut) {
        if (statut == null) {
            return INCONNU;
        }
        return switch (statut.toString().trim().toUpperCase(Locale.ROOT)) {
            case "UP" -> UP;
            case "DOWN" -> DOWN;
            case "OUT_OF_SERVICE" -> OUT_OF_SERVICE;
            default -> INCONNU;
        };
    }
}
