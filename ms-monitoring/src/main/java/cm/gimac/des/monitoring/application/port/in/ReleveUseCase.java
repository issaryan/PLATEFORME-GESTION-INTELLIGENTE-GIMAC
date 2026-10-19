package cm.gimac.des.monitoring.application.port.in;

import cm.gimac.des.monitoring.application.port.in.Vues.RapportVue;
import java.util.UUID;

/**
 * Relève de quart (algorithme 6.46, D-12, MON-15) : rapport généré, signé par le sortant, contresigné par l'entrant,
 * visé par le chef de quart, puis scellé ; chaque acte avec une assertion FIDO2 vérifiée par MS-IAM.
 */
public interface ReleveUseCase {
    RapportVue generer(Appelant appelant, UUID vacation, String consignes);

    RapportVue rapportDeVacation(Appelant appelant, UUID vacation);

    RapportVue rapport(Appelant appelant, UUID id);

    RapportVue signer(Appelant appelant, UUID id, String consignes, String assertion);

    RapportVue contresigner(Appelant appelant, UUID id, String reserves, String assertion);

    RapportVue viser(Appelant appelant, UUID id, String assertion);
}
