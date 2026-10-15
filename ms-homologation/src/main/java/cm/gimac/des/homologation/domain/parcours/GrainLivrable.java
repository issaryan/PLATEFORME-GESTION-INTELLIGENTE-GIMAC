package cm.gimac.des.homologation.domain.parcours;

/**
 * Grain d'un livrable attendu (D-109, CR §4.4.1 et §6.3.1) : une pièce pour l'opération, ou une pièce par service
 * inclus au périmètre contractualisé (essais service par service, tests de transaction en production). Distinct du
 * grain de preuve de l'opération ({@link GrainPreuve}, D-59 : phase en essai, opération en production).
 */
public enum GrainLivrable {
    OPERATION,
    SERVICE,
}
