package cm.gimac.des.monitoring.application.port.out;

/**
 * Réauthentification forte FIDO2 (IAM-07) : l'assertion transmise par l'interface (en-tête X-Reauth-Assertion)
 * est vérifiée et consommée par MS-IAM (POST /api/v1/interne/reauthentifications/verification, jeton de service
 * monitoring-service), pour le sujet et la finalité annoncés (SIGNATURE_RELEVE, PUBLICATION_GRILLE). Refus : faux ; MS-IAM
 * injoignable : 503 MON_REAUTH_INDISPONIBLE, sans valeur supposée.
 */
public interface ReauthentificationPort {
    String SIGNATURE_RELEVE = "SIGNATURE_RELEVE";
    String PUBLICATION_GRILLE = "PUBLICATION_GRILLE";

    boolean verifier(String assertion, String sujet, String finalite);
}
