package cm.gimac.des.monitoring.application.port.out;

/**
 * Horodatage qualifié RFC 3161 d'une empreinte (rapport de quart, algorithme 6.46). L'autorité d'horodatage n'est pas
 * encore raccordée : l'adaptateur de substitution rend un jeton local, signalé comme tel.
 */
public interface HorodatagePort {
    Jeton horodater(String empreinteHex);

    record Jeton(String serie, byte[] jeton) {}
}
