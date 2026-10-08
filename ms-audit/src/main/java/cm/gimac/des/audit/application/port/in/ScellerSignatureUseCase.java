package cm.gimac.des.audit.application.port.in;

import cm.gimac.des.audit.domain.registre.NatureSignature;
import java.time.Instant;
import java.util.Optional;

/**
 * Scellement d'une signature au registre d'audit (D-93 §6 et §11, CC §6.1.3.3) : l'enveloppe CloudEvents reçue, sous
 * forme canonique (RFC 8785), devient une feuille de l'arbre de Merkle, avec l'entrée de déduplication et les nœuds
 * des sous-arbres qu'elle achève, dans une même transaction. Idempotent par identifiant CloudEvents.
 */
public interface ScellerSignatureUseCase {
    /**
     * Signature reçue.
     *
     * @param eventId identifiant CloudEvents de l'enveloppe
     * @param survenueLe instant d'occurrence fourni par le producteur (champ time, sinon le de la charge) ; absent :
     *     l'instant d'inscription
     * @param correlationId extension correlationid, si c'est un UUID
     * @param signataireId compte qui a signé (acteur de l'entrée)
     * @param enveloppe enveloppe CloudEvents reçue, telle quelle (JSON)
     */
    record SignatureRecue(String eventId, NatureSignature nature, Instant survenueLe, String correlationId, String signataireId, String enveloppe) {}

    /** Index de la feuille inscrite ; vide si l'événement était déjà scellé (relivraison). */
    Optional<Long> sceller(SignatureRecue signature);
}
