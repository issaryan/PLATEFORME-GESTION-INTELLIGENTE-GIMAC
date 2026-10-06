package cm.gimac.des.pilotage.application.port.out;

/**
 * Réauthentification forte FIDO2 (IAM-07) : l'assertion transmise par l'interface (en-tête X-Reauth-Assertion)
 * est vérifiée et consommée par MS-IAM (POST /api/v1/interne/reauthentifications/verification, jeton de service
 * pilotage-service), pour le sujet et la finalité annoncés (SIGNATURE_PROCESSUS). Refus : faux ; MS-IAM
 * injoignable : 503 PIL_REAUTH_INDISPONIBLE, sans valeur supposée.
 */
public interface ReauthentificationPort {
    String SIGNATURE_PROCESSUS = "SIGNATURE_PROCESSUS";

    boolean verifier(String assertion, String sujet, String finalite);
}
