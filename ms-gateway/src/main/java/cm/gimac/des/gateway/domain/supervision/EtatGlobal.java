package cm.gimac.des.gateway.domain.supervision;

import java.util.Collection;

/**
 * Synthèse de la plateforme : OPERATIONNELLE si tous les services sont UP, DEGRADEE si au
 * moins un ne l'est pas, HORS_SERVICE si aucun ne l'est. « down » compte tout service qui
 * n'est pas UP (DOWN, OUT_OF_SERVICE, INCONNU, INJOIGNABLE).
 */
public record EtatGlobal(String etat, int total, int up, int down, int injoignables) {
    public static final String OPERATIONNELLE = "OPERATIONNELLE";
    public static final String DEGRADEE = "DEGRADEE";
    public static final String HORS_SERVICE = "HORS_SERVICE";

    public static EtatGlobal de(Collection<EtatService> services) {
        int total = services.size();
        int up = (int) services.stream().filter(s -> s.etat() == EtatSante.UP).count();
        int injoignables = (int) services.stream().filter(s -> s.etat() == EtatSante.INJOIGNABLE).count();
        String etat;
        if (total > 0 && up == total) {
            etat = OPERATIONNELLE;
        } else if (up == 0) {
            etat = HORS_SERVICE;
        } else {
            etat = DEGRADEE;
        }
        return new EtatGlobal(etat, total, up, total - up, injoignables);
    }
}
