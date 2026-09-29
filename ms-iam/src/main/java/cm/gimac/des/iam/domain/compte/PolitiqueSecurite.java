package cm.gimac.des.iam.domain.compte;

/**
 * Politique de sécurité de l'authentification (realm), configurée par le super administrateur
 * ou le RSSI : mots de passe, second facteur, sessions, protection contre la force brute.
 */
public record PolitiqueSecurite(
    int longueurMinimale,
    int majusculesMin,
    int minusculesMin,
    int chiffresMin,
    int speciauxMin,
    int historique,
    int expirationJours,
    boolean interditIdentifiant,
    int otpChiffres,
    int otpPeriodeSecondes,
    String otpAlgorithme,
    int sessionInactiviteMinutes,
    int sessionDureeMaxHeures,
    boolean forceBruteActive,
    int forceBruteEchecsMax,
    int forceBruteAttenteMinutes
) {}
