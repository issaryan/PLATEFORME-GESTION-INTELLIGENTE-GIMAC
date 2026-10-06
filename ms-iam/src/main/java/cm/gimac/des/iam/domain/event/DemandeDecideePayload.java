package cm.gimac.des.iam.domain.event;

import java.time.Instant;
import java.util.List;

/**
 * iam.request-decided.v1 (sujet iam.request, clé = demande ; D-81, docs/lot1/pouvoirs-hierarchiques.md §4) :
 * demande acceptée ou refusée par l'administration. Destinataire : le demandeur.
 *
 * @param etat ACCEPTEE ou REFUSEE
 * @param cibleId compte visé ; pour une création acceptée, le compte créé
 * @param acteurId compte de l'administration qui a décidé
 */
public record DemandeDecideePayload(String demandeId, String type, String etat, String demandeurId, String cibleId, String acteurId, List<String> destinataires, Instant le) {
    public static final String TYPE = "iam.request-decided.v1";
}
