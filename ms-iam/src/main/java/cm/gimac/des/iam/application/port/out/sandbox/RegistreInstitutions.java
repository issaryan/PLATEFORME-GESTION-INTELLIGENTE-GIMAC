package cm.gimac.des.iam.application.port.out.sandbox;

import java.util.Optional;

/**
 * Correspondance du registre des participants (MS-PARTICIPANT, D-134) entre l'identifiant d'une institution, porté par
 * le claim institution_id de la plateforme, et son code participant, porté par la sandbox (CC-SBX-GIMAC-02 : « le jeton
 * porte institution_id égal au code de l'espace »). Lecture seule.
 */
public interface RegistreInstitutions {
    /** Code participant de l'institution de cet identifiant ; vide si elle est inconnue ou n'a pas encore de code. */
    Optional<String> codeParticipant(java.util.UUID institutionId);

    /** Identifiant au registre de l'institution de ce code participant ; vide si le code est inconnu. */
    Optional<java.util.UUID> identifiant(String codeParticipant);

    /** Registre injoignable : la traduction échoue, sans jamais deviner. */
    final class RegistreIndisponible extends RuntimeException {

        public RegistreIndisponible(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
