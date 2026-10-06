package cm.gimac.des.iam.domain.demande;

import java.time.Instant;
import java.util.UUID;

/**
 * Demande d'un chef à l'administration (D-81, docs/lot1/pouvoirs-hierarchiques.md §4) : création d'un
 * compte de subordonné, ajout ou retrait de profils, désactivation ou réactivation. L'administration
 * l'accepte (l'acte est alors exécuté par MS-IAM) ou la refuse avec un motif ; le demandeur peut
 * l'annuler tant qu'elle est en attente.
 *
 * @param cible compte visé ; nul pour une création tant qu'elle n'est pas acceptée, puis le compte créé
 * @param decision nulle tant que la demande est en attente ou annulée
 */
public record Demande(UUID id, TypeDemande type, Personne demandeur, Personne cible, ContenuDemande contenu, String motif, EtatDemande etat, Decision decision, Instant creeeLe) {
    /** Identité d'un compte dans une demande. */
    public record Personne(String id, String nomAffiche) {}

    /** Décision de l'administration (ou annulation par le demandeur : par, le ; motif nul). */
    public record Decision(Personne par, String motif, Instant le) {}
}
