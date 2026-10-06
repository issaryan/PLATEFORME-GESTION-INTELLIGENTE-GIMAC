package cm.gimac.des.iam.domain.event;

import java.time.Instant;
import java.util.List;

/**
 * iam.request-submitted.v1 (sujet iam.request, clé = demande ; D-81, docs/lot1/pouvoirs-hierarchiques.md
 * §4) : demande adressée à l'administration. Destinataires : comptes actifs portant IAM_ACCOUNT_CREATE ou
 * IAM_ACCOUNT_ROLES, sauf le demandeur. Ni contenu ni motif : MS-NOTIFICATION lit le nom du demandeur dans
 * l'annuaire.
 *
 * @param cibleId compte visé, nul pour une création
 */
public record DemandeSoumisePayload(String demandeId, String type, String etat, String demandeurId, String cibleId, List<String> destinataires, Instant le) {
    public static final String TYPE = "iam.request-submitted.v1";
}
