package cm.gimac.des.audit.application.port.in;

import cm.gimac.des.audit.domain.sandbox.EnveloppeSandbox;
import java.util.Optional;

/**
 * Inscription au registre d'audit d'un événement à effet de la Sandbox GIMAC (US-EXI-10) : même chaîne que les
 * signatures (déduplication par identifiant CloudEvents, index sans trou, forme canonique, arbre de Merkle), domaine
 * {@value EnveloppeSandbox#DOMAINE}, institution retenue pour la recherche. Le rebut garde les enveloppes non conformes
 * avec leur motif.
 */
public interface InscrireEvenementSandboxUseCase {
    /** Index de la feuille inscrite ; vide si l'événement était déjà inscrit (relivraison). */
    Optional<Long> inscrire(EnveloppeSandbox enveloppe, String message);

    /** Enveloppe écartée : inscrite au rebut avec son motif, jamais au registre. */
    void rebuter(String eventId, String sujet, String motif, String message);
}
