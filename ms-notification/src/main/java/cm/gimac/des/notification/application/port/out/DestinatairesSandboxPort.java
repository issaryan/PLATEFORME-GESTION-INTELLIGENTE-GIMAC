package cm.gimac.des.notification.application.port.out;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Destinataires des notifications de la Sandbox GIMAC (US-EXI-09), lus chez MS-IAM (jeton de service INTERNAL_IAM_READ) :
 * comptes actifs d'une institution (code participant, attribut {@code institution_id}) avec leurs profils, et comptes du
 * GIMAC porteurs d'une permission. MS-IAM injoignable : NOT_ANNUAIRE_INDISPONIBLE (503), la demande est rejouée puis mise
 * en rebut, jamais perdue.
 */
public interface DestinatairesSandboxPort {
    /** Comptes actifs de l'institution et leurs profils ; institution sans compte : vide. */
    Map<String, Set<String>> comptesInstitution(String codeParticipant);

    /** Comptes du GIMAC actifs porteurs de la permission. */
    List<String> porteurs(String permission);
}
