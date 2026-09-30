package cm.gimac.des.certification.application.port.out;

import java.time.Instant;

/**
 * Cachet de la plateforme (D-35, second acte) : la clé de signature de la plateforme contresigne
 * l'empreinte de la décision et de l'assertion, puis l'autorité d'horodatage horodate le cachet. Le cachet
 * atteste l'intégrité, jamais le consentement ; le coffre ne signe jamais au nom d'une personne.
 */
public interface CachetPlateformePort {
    Cachet sceller(String empreinte);

    /** Signature de la plateforme, jeton d'horodatage (encodés en base64), nonce, instant et chaîne de l'autorité. */
    record Cachet(String signature, String jetonHorodatage, String nonce, Instant horodatage, String autorite) {}
}
