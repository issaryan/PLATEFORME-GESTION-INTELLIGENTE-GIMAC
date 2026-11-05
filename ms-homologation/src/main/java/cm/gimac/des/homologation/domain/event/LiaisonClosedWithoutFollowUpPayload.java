package cm.gimac.des.homologation.domain.event;

import java.time.Instant;
import java.util.UUID;

/**
 * homologation.liaison-closed-without-follow-up.v1 (sujet homologation.liaison, D-137) : trace de la clôture sans suite
 * d'une liaison reprise, avec son auteur, son motif et le statut qu'elle avait. La Sandbox GIMAC voit la fermeture par
 * homologation.liaison-status-changed.v1, publié dans la même transaction.
 */
public record LiaisonClosedWithoutFollowUpPayload(
    UUID liaisonId,
    UUID institutionId,
    String referenceOrigine,
    String statutPrecedent,
    String motif,
    UUID acteurId,
    String acteurNom,
    Instant survenuLe
) {
    public static final String TYPE = "homologation.liaison-closed-without-follow-up.v1";
}
