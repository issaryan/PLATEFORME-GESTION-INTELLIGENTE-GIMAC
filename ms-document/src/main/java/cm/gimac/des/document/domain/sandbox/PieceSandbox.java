package cm.gimac.des.document.domain.sandbox;

import java.time.Instant;
import java.util.UUID;

/**
 * Pièce de la Sandbox GIMAC (US-EXI-08) : document scellé dans le dépôt inaltérable (même identifiant que son
 * document) ou pièce mise en quarantaine (aucun contenu conservé). Codes, empreinte et dates seulement.
 */
public record PieceSandbox(
    UUID id,
    String type,
    String codeParticipant,
    String reference,
    String sha256,
    long taille,
    Etat etat,
    Instant deposeeLe,
    Instant conserveeJusquAu,
    String serviceDeposant,
    int occurrencesPan
) {
    public enum Etat {
        SCELLEE,
        QUARANTAINE,
    }
}
