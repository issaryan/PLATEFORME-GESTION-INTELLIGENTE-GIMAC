package cm.gimac.des.notification.application.port.out;

import java.util.List;

/**
 * Comptes actifs d'une institution (correspondants, utilisateurs du portail), lus chez MS-IAM (jeton de service
 * INTERNAL_IAM_READ) pour notifier une institution qui tient une fonction d'une opération d'homologation (D-93).
 * Institution sans compte : liste vide ; MS-IAM injoignable : NOT_ANNUAIRE_INDISPONIBLE (503), l'événement est
 * alors rejoué puis mis en rebut, jamais perdu.
 */
public interface ComptesInstitutionPort {
    List<String> comptes(String institutionId);
}
